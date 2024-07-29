package controller.virtual.common

import controller.virtual.VirtualControllerConfig
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import platform.posix.POLLIN
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

    private var pollfd: pollfd? = null

    protected abstract fun handleUhidEvent(event: UHidEvent)

    context(MemScope)
    override fun create2(): pollfd {
        println("virtual controller ${deviceInfo.name} created")

        uhidDevice.open()
        uhidDevice.create()

        val pollfd =  alloc<pollfd>().apply {
            fd = uhidDevice.fd
            events = POLLIN.toShort()
        }

        this.pollfd = pollfd

        return pollfd
    }

    override fun destroy() {
        uhidDevice.write(UHidEvent.Destroy)
        uhidDevice.close()
        pollfd = null
        println("virtual controller ${deviceInfo.name} destroyed")
    }

    context(MemScope)
    override fun readEvents() {
        /**
         * todo: inform about issues
         */
        val pollfd = pollfd ?: return

        if (pollfd.revents.toInt() and POLLIN != 0) {
            val event = uhidDevice.read()
            handleUhidEvent(event)
        }
    }
}