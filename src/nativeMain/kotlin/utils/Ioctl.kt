@file:Suppress("FunctionName")

package utils

import platform.posix.*

fun IOC(dir: ULong, type: ULong, nr: ULong, size: ULong): ULong {
    return (dir shl _IOC_DIRSHIFT) or (type shl _IOC_TYPESHIFT) or (nr shl _IOC_NRSHIFT) or (size shl _IOC_SIZESHIFT)
}

fun IOR(type: ULong, nr: ULong, size: ULong): ULong {
    return IOC(_IOC_READ.toULong(), type, nr, size)
}

fun IOW(type: ULong, nr: ULong, size: ULong): ULong {
    return IOC(_IOC_WRITE.toULong(), type, nr, size)
}

fun IOWR(type: Char, nr: ULong, size: ULong): ULong {
    return IOC((_IOC_READ or _IOC_WRITE).toULong(), type.code.toULong(), nr, size)
}

fun HIDIOCGRDESCSIZE(len: ULong): ULong {
    return IOR('H'.code.toULong(), 0x01u, len)
}

fun HIDIOCGRDESC(len: ULong): ULong {
    return IOR('H'.code.toULong(), 0x02u, len)
}

fun HIDIOCGRAWINFO(len: ULong): ULong {
    return IOR('H'.code.toULong(), 0x03u, len)
}

fun HIDIOCGRAWNAME(len: ULong): ULong {
    return IOR('H'.code.toULong(), 0x04u, len)
}

fun HIDIOCGRAWPHYS(len: ULong): ULong {
    return IOR('H'.code.toULong(), 0x05u, len)
}

fun EVIOCGNAME(len: ULong): ULong {
    return IOR('E'.code.toULong(), 0x06u, len)
}

fun EVIOCGRAB(len: ULong): ULong {
    return IOW('E'.code.toULong(), 0x90u, len)
}