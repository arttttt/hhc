package controller.physical2.common

import controller.common.ControllerState
import kotlinx.cinterop.MemScope
import platform.posix.pollfd

interface InputDevice {
    val hwInfo: InputDeviceHwInfo

    context(MemScope)
    fun open(): pollfd

    fun close()

    fun read(to: ByteArray): Int

    context(MemScope)
    fun processRawData(rawData: ByteArray, state: ControllerState): Boolean
}