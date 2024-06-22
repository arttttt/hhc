package controller.physical.common

import controller.common.ControllerState
import kotlinx.coroutines.flow.Flow

interface PhysicalController {

    val name: String
    val states: Flow<ControllerState>

    fun start()
    fun stop()

    fun consumeControllerState(state: ControllerState)
}