package controller.common

import kotlinx.cinterop.MemScope

interface Controller {

    val controllerState: ControllerState

    var onControllerStateChanged: (() -> Unit)?

    context(MemScope)
    fun readEvents()
}