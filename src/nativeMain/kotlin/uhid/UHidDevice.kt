package uhid

import kotlinx.cinterop.*
import platform.posix.*

/**
 * low level thing
 */
class UHidDevice(
    private val name: String,
    private val uniq: String,
    private val product: UInt,
    private val vendor: UInt,
    private val version: UInt,
    private val country: UInt,
    private val bus: UShort,
    private val reportDescriptor: UByteArray,
) {

    companion object {

        private const val UHID_DEVICE = "/dev/uhid"
    }

    var fd: Int = -1
        private set(value) {
            field = value
        }

    fun open() {
        if (fd >= 0) throw IllegalStateException("UHID device already open")

        fd = open(UHID_DEVICE, O_RDWR)
        if (fd < 0) {
            perror("open")

            throw RuntimeException("Failed to open UHID device")
        }
    }

    fun create() {
        memScoped {
            val createEvent = UHidEvent.Create(
                name = this@UHidDevice.name,
                uniq = this@UHidDevice.uniq,
                reportDescriptor = this@UHidDevice.reportDescriptor,
                bus = this@UHidDevice.bus,
                vendor = this@UHidDevice.vendor,
                product = this@UHidDevice.product,
                version = this@UHidDevice.version,
                country = this@UHidDevice.country,
            )

            write(createEvent.toPlatformEvent(this))
        }
    }

    fun close() {
        if (fd < 0) throw IllegalStateException("Failed to close UHID device")
        close(fd)
        fd = -1
    }

    fun write(event: UHidEvent) {
        memScoped {
            val platformEvent = event.toPlatformEvent(this)
            val ret = write(fd, platformEvent.ptr, sizeOf<uhid_event>().toULong())

            if (ret < 0) {
                perror("write")
                throw RuntimeException("Failed to write to UHID device")
            }
        }
    }

    fun read(): UHidEvent {
        return memScoped {
            val event = alloc<uhid_event>()

            if (read(fd, event.ptr, sizeOf<uhid_event>().toULong()) <= 0) throw IllegalStateException("Failed to read UHID device")

            UHidEvent.fromPlatformEvent(event)
        }
    }


    private fun write(event: uhid_event) {
        if (write(fd, event.ptr, sizeOf<uhid_event>().toULong()) < 0) {
            perror("write")
            throw RuntimeException("Failed to write to UHID device")
        }
    }
}