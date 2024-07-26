package controller.physical2.common

import controller.common.ControllerState
import controller.common.input.axis.AxisStateOwner
import controller.common.input.buttons.ButtonsStateOwner
import controller.physical.InputDeviceHwInfo
import input.EV_ABS
import input.EV_KEY
import input.EV_SYN
import input.input_event
import kotlinx.cinterop.*
import platform.posix.*

class EvdevDevice(
    override val hwInfo: InputDeviceHwInfo
) : InputDevice {

    private var fd: Int = -1

    context(MemScope)
    override fun open(): pollfd {
        fd = open(hwInfo.path, O_RDWR)
        if (fd < 0) {
            throw IllegalStateException("Не удалось открыть устройство: ${hwInfo.path}")
        }

        return alloc<pollfd>().apply {
            this.fd = this@EvdevDevice.fd
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
    override fun processRawData(
        rawData: ByteArray,
        state: ControllerState,
    ) {
        val ev = alloc<input_event>()
        val bytesRead = read(fd, ev.ptr, sizeOf<input_event>().toULong())
        if (bytesRead < 0) {
            perror("Error reading from device")

            return
        }

        if (ev.type.toInt() == EV_SYN) return

        handleInputEvent(ev, state)
    }

    private fun handleInputEvent(
        event: input_event,
        state: ControllerState,
    ) {
        when {
            event.type.toInt() == EV_KEY && state is ButtonsStateOwner -> handleKeys(event, state)
            event.type.toInt() == EV_ABS && state is AxisStateOwner -> handleAxis(event, state)
        }
    }

    private fun handleKeys(
        event: input_event,
        state: ButtonsStateOwner,
    ) {
        state.setButtonState(event.code.toInt(), event.value == 1)
    }

    private fun handleAxis(
        event: input_event,
        state: AxisStateOwner,
    ) {
        state.setAxisState(event.code.toInt(), event.value)
    }
}