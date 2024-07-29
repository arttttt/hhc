package controller.physical2.common

import kotlinx.cinterop.IntVar
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.ioctl
import platform.posix.strerror
import utils.EVIOCGRAB

class GamepadGrabber {

    fun grab(fd: Int): Boolean {
        val grabCommand = EVIOCGRAB(sizeOf<IntVar>().toULong())
        val result = ioctl(fd, grabCommand, 1)
        if (result != 0) {
            println("Can not grab the device: ${strerror(errno)?.toKString()}")
            return false
        }
        return true
    }

    fun release(fd: Int): Boolean {
        val grabCommand = EVIOCGRAB(sizeOf<IntVar>().toULong())
        return ioctl(fd, grabCommand, 0) == 0
    }
}