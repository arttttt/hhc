package controller.common

import kotlinx.cinterop.MemScope

interface Controller {

    val controllerState: ControllerState

    context(MemScope)
    fun readEvents()
}