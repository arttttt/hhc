package controller.physical2.lego

import controller.common.ControllerState
import controller.common.normalization.NormalizationMode
import controller.common.output.OutputStateWriter
import controller.common.rumble.RumbleStateOwner
import controller.common.rumble.RumbleStateOwnerImpl
import controller.physical2.common.InputDevice
import utils.denormalize

class LegionGoOutputState : OutputStateWriter,
    RumbleStateOwner by RumbleStateOwnerImpl(
        normalizationMode = NormalizationMode.U8,
    ) {

    companion object {

        private const val REPORT_ID: Byte = 0x04
        private const val REPORT_SIZE = 9
    }

    private val report = ByteArray(REPORT_SIZE).apply {
        this[0] = REPORT_ID
        this[2] = 0x08
    }

    override fun flush(device: InputDevice, state: ControllerState) {
        if (state is RumbleStateOwner) {
            setWeakRumbleValue(denormalize(state.state.weakRumble, NormalizationMode.U8))
            setStrongRumbleValue(denormalize(state.state.strongRumble, NormalizationMode.U8))
        }

        report[4] = denormalize(this.state.strongRumble, NormalizationMode.U8).toByte()
        report[5] = denormalize(this.state.weakRumble, NormalizationMode.U8).toByte()

        device.write(report)
    }
}
