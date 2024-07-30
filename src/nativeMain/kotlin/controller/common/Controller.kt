package controller.common

import kotlinx.cinterop.MemScope

interface Controller {

    val controllerState: ControllerState

    var onControllerStateChanged: ((ControllerState) -> Unit)?

    context(MemScope)
    fun readEvents()

    fun consumeControllerState(state: ControllerState)
}