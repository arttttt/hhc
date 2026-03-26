package controller.common.output

import controller.common.ControllerState
import controller.physical2.common.InputDevice

interface OutputStateWriter {

    fun flush(device: InputDevice, state: ControllerState)
}
