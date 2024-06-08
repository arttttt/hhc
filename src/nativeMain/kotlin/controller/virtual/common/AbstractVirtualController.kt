package controller.virtual.common

import controller.virtual.VirtualControllerConfig
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.*
import platform.posix.POLLIN
import platform.posix.poll
import platform.posix.pollfd
import uhid.BUS_USB
import uhid.UHidDevice
import uhid.UHidEvent

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractVirtualController(
    private val deviceInfo: VirtualControllerConfig,
) : VirtualController {

    protected val uhidDevice = UHidDevice(
        name = deviceInfo.name,
        uniq = deviceInfo.uniq,
        product = deviceInfo.product,
        vendor = deviceInfo.vendor,
        version = deviceInfo.version,
        country = deviceInfo.country,
        bus = BUS_USB.toUShort(),
        reportDescriptor = deviceInfo.reportDescriptor,
    )

    protected val scope = CoroutineScope(newSingleThreadContext("DualSenseDispatcher") + SupervisorJob())

    protected abstract fun handleUhidEvent(event: UHidEvent)

    override fun create() {
        uhidDevice.open()
        uhidDevice.create()
        startControllerLoop()

        println("virtual controller ${deviceInfo.name} created")
    }

    override fun destroy() {
        uhidDevice.write(UHidEvent.Destroy)
        uhidDevice.close()

        println("virtual controller ${deviceInfo.name} destroyed")
    }

    private fun startControllerLoop() {
        scope.launch {
            memScoped {
                val pollFd = alloc<pollfd>().apply {
                    fd = uhidDevice.fd
                    events = POLLIN.toShort()
                }

                while (true) {
                    ensureActive()

                    val ret = poll(pollFd.ptr, 1u, -1)

                    if (ret == -1) throw IllegalStateException("Can not start polling")

                    if (pollFd.revents.toInt() and POLLIN != 0) {
                        val event = uhidDevice.read()

                        handleUhidEvent(event)
                    }
                }
            }
        }
    }
}