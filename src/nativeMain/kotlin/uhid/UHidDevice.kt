package uhid

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.posix.*

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

    fun open() {
        if (fd >= 0) throw IllegalStateException("UHID device already open")

        fd = open(UHID_DEVICE, O_RDWR or O_NONBLOCK)
        if (fd < 0) {
            perror("open")

            throw RuntimeException("Failed to open UHID device")
        }
    }

    fun create() {
        memScoped {
            val createReq = alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_CREATE2.value
                u.create2.apply {
                    memcpy(name, this@UHidDevice.name.cstr.getPointer(this@memScoped), this@UHidDevice.name.length.toULong())
                    memcpy(uniq, this@UHidDevice.uniq.cstr.getPointer(this@memScoped), this@UHidDevice.uniq.length.toULong())

                    rd_size = reportDescriptor.size.toUShort()
                    memcpy(rd_data, reportDescriptor.refTo(0), reportDescriptor.size.toULong())
                    bus = this@UHidDevice.bus
                    vendor = this@UHidDevice.vendor
                    product = this@UHidDevice.product
                    version = this@UHidDevice.version
                    country = this@UHidDevice.country
                }
            }
            if (write(fd, createReq.ptr, sizeOf<uhid_event>().toULong()) < 0) {
                perror("write")
                close(fd)

                throw RuntimeException("Failed to create UHID device")
            }
        }
    }

    fun close() {
        if (fd < 0) throw IllegalStateException("Failed to close UHID device")
        close(fd)
        fd = -1
    }

    fun write(event: uhid_event) {
        if (write(fd, event.ptr, sizeOf<uhid_event>().toULong()) < 0) {
            perror("write")
            throw RuntimeException("Failed to write to UHID device")
        }
    }

    fun read(event: uhid_event): uhid_event {
        if (read(fd, event.ptr, sizeOf<uhid_event>().toULong()) <= 0) throw IllegalStateException("Failed to read UHID device")

        return event
    }

    fun readUhidEvents(): Flow<uhid_event> {
        return flow {
            memScoped {
                val pollFd = alloc<pollfd>().apply {
                    fd = this@UHidDevice.fd
                    events = POLLIN.toShort()
                }

                while (true) {
                    val ret = poll(pollFd.ptr, 1u, -1)
                    println("loop")
                    if (ret == -1) {
                        perror("poll")
                        return@flow
                    }

                    if (pollFd.revents.toInt() and POLLIN != 0) {
                        val uhidEvent = alloc<uhid_event>()
                        val bytesRead = read(fd, uhidEvent.ptr, sizeOf<uhid_event>().toULong())
                        if (bytesRead <= 0) {
                            perror("read")
                            return@flow
                        }

                        // Emit the UHID event
                        emit(uhidEvent)
                    }
                }
            }
        }
    }
}