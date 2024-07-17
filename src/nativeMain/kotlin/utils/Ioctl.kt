@file:Suppress("FunctionName")

package utils

import platform.posix.*

fun IOC(dir: ULong, type: ULong, nr: ULong, size: ULong): ULong {
    return (dir shl _IOC_DIRSHIFT) or (type shl _IOC_TYPESHIFT) or (nr shl _IOC_NRSHIFT) or (size shl _IOC_SIZESHIFT)
}

fun _IOR(type: ULong, nr: ULong, size: ULong): ULong {
    return IOC(_IOC_READ.toULong(), type, nr, size)
}

fun _IOW(type: ULong, nr: ULong, size: ULong): ULong {
    return IOC(_IOC_WRITE.toULong(), type, nr, size)
}

fun EVIOCGNAME(len: ULong): ULong {
    return _IOR('E'.code.toULong(), 0x06u, len)
}

fun EVIOCGRAB(len: ULong): ULong {
    return _IOW('E'.code.toULong(), 0x90u, len)
}