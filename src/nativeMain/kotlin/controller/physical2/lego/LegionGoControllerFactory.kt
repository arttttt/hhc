package controller.physical2.lego

import controller.common.output.OutputStateWriter
import controller.physical2.common.HidrawDevice
import controller.physical2.common.InputDevice
import controller.physical2.common.PhysicalController2
import controller.physical2.common.PhysicalControllerFactory

class LegionGoControllerFactory : PhysicalControllerFactory {

    companion object {

        private const val XINPUT_DEVICE_NAME = "Lenovo Legion Controller for Windows"
    }

    override fun create(devices: List<InputDevice>): PhysicalController2 {
        val deviceOutputStates = mutableMapOf<InputDevice, OutputStateWriter?>()

        for (device in devices) {
            val outputState = when {
                device is HidrawDevice && device.hwInfo.name.contains(XINPUT_DEVICE_NAME) -> LegionGoOutputState()
                else -> null
            }

            deviceOutputStates[device] = outputState
        }

        return LenovoLegionGoController(deviceOutputStates)
    }
}
