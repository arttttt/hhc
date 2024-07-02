package controller.virtual.common

import controller.common.ControllerState
import controller.virtual.VirtualControllerConfig
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import platform.posix.POLLIN
import platform.posix.poll
import platform.posix.pollfd
import uhid.BUS_USB
import uhid.UHidDevice
import uhid.UHidEvent

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

    protected val uhidScope = CoroutineScope(newSingleThreadContext("UHIDDispatcher") + SupervisorJob())
    protected val stateProcessingScope = CoroutineScope(newSingleThreadContext("StateProcessingDispatcher") + SupervisorJob())
    private val stateChannel = Channel<ControllerState>(Channel.BUFFERED)

    protected abstract fun handleUhidEvent(event: UHidEvent)
    protected abstract suspend fun handleInputState(state: ControllerState)

    override fun create() {
        uhidScope.launch {
            uhidDevice.open()
            uhidDevice.create()
            startControllerLoop()
        }
        stateProcessingScope.launch {
            startStateProcessingLoop()
        }
        println("virtual controller ${deviceInfo.name} created")
    }

    override fun destroy() {
        uhidScope.cancel()
        stateProcessingScope.cancel()
        uhidDevice.write(UHidEvent.Destroy)
        uhidDevice.close()
        println("virtual controller ${deviceInfo.name} destroyed")
    }

    override fun consumeControllerState(state: ControllerState) {
        stateChannel.trySend(state)
    }

    private suspend fun startControllerLoop() {
        memScoped {
            val pollFd = alloc<pollfd>().apply {
                fd = uhidDevice.fd
                events = POLLIN.toShort()
            }

            while (true) {
                currentCoroutineContext().ensureActive()

                val ret = poll(pollFd.ptr, 1u, -1)

                if (ret == -1) throw IllegalStateException("Can not start polling")

                if (pollFd.revents.toInt() and POLLIN != 0) {
                    val event = uhidDevice.read()
                    handleUhidEvent(event)
                }
            }
        }
    }

    private suspend fun startStateProcessingLoop() {
        for (state in stateChannel) {
            handleInputState(state)
        }
    }
}