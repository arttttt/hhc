package controller.physical.common

import controller.common.ControllerState
import controller.physical.InputDeviceHwInfo
import kotlinx.coroutines.flow.Flow

interface PhysicalController {

    val hwInfo: InputDeviceHwInfo
    val states: Flow<ControllerState>

    fun start()
    fun stop()

    fun consumeControllerState(state: ControllerState)
}