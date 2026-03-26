package controller.physical2.common

import kotlinx.cinterop.MemScope
import platform.posix.pollfd

interface InputDevice {
    val hwInfo: InputDeviceHwInfo

    context(scope: MemScope)
    fun open(): pollfd

    fun close()

    fun read(to: ByteArray): Int

    fun write(data: ByteArray): Int
}