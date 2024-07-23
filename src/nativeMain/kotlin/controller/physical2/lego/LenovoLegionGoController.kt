package controller.physical2.lego

import controller.common.ControllerState
import controller.physical2.common.AbstractPhysicalController
import controller.physical2.common.InputDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class LenovoLegionGoController(
    devices: List<InputDevice>
) : AbstractPhysicalController(devices) {

    init {
        println(devices.joinToString("\n"))
    }

    override val inputState: ControllerState = object : ControllerState {}

    override fun onStateUpdated() {}

    override val states: Flow<ControllerState> = emptyFlow()

    override fun consumeControllerState(state: ControllerState) {}
}