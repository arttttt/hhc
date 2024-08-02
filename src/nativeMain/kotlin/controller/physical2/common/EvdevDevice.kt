package controller.physical2.common

import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.refTo
import platform.posix.*

class EvdevDevice(
    override val hwInfo: InputDeviceHwInfo
) : InputDevice {

    private val grabber = GamepadGrabber()

    private var fd: Int = -1

    context(MemScope)
    override fun open(): pollfd {
        fd = open(hwInfo.path, O_RDWR)
        if (fd < 0) {
            throw IllegalStateException("Не удалось открыть устройство: ${hwInfo.path}")
        }

        grabber.grab(fd)

        return alloc<pollfd>().apply {
            this.fd = this@EvdevDevice.fd
            events = POLLIN.toShort()
        }
    }

    override fun close() {
        if (fd < 0) return

        grabber.release(fd)

        close(fd)
        fd = -1
    }

    override fun read(to: ByteArray): Int {
        return read(fd, to.refTo(0), to.size.toULong()).toInt()
    }
}