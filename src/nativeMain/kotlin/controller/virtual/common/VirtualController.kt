package controller.virtual.common

import controller.common.ControllerState
import kotlinx.coroutines.flow.Flow

interface VirtualController {

    val states: Flow<ControllerState>

    fun create()
    fun destroy()

    fun consumeControllerState(state: ControllerState)
}