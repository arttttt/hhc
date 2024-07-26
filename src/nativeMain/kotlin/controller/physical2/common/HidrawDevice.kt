package controller.physical2.common

import controller.common.ControllerState
import controller.common.input.axis.AxisStateOwner
import controller.common.input.buttons.ButtonsStateOwner
import controller.physical.InputDeviceHwInfo
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.refTo
import platform.posix.*

class HidrawDevice(
    override val hwInfo: InputDeviceHwInfo,
) : InputDevice {

    private var fd: Int = -1

    context(MemScope)
    override fun open(): pollfd {
        fd = open(hwInfo.path, O_RDWR)
        if (fd < 0) {
            throw IllegalStateException("Не удалось открыть устройство: ${hwInfo.path}")
        }
        return alloc<pollfd>().apply {
            this.fd = this@HidrawDevice.fd
            events = POLLIN.toShort()
        }
    }

    override fun close() {
        if (fd < 0) return

        close(fd)
        fd = -1
    }

    override fun read(to: ByteArray): Int {
        return read(fd, to.refTo(0), to.size.toULong()).toInt()
    }

    context(MemScope)
    override fun processRawData(rawData: ByteArray, state: ControllerState) {
        if (state is ButtonsStateOwner) {
            state.setButtonsState(rawData)
        }

        if (state is AxisStateOwner) {
            state.setAxisState(rawData)
        }
    }
}