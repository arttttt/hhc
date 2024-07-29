package controller.virtual.common

import controller.common.Controller
import controller.common.ControllerState
import kotlinx.cinterop.MemScope
import kotlinx.coroutines.flow.Flow
import platform.posix.pollfd

interface VirtualController : Controller {

    context(MemScope)
    fun create2(): pollfd

    fun destroy()

    fun consumeControllerState(state: ControllerState)
}