package controller.physical2.common

import kotlinx.cinterop.MemScope
import platform.posix.pollfd

interface InputDevice {
    val hwInfo: InputDeviceHwInfo

    context(MemScope)
    fun open(): pollfd

    fun close()

    fun read(to: ByteArray): Int
}