package controller.physical2.common

import controller.common.ControllerState
import kotlinx.coroutines.flow.Flow

interface PhysicalController2 {

    val states: Flow<ControllerState>

    fun start()
    fun stop()

    fun consumeControllerState(state: ControllerState)
}