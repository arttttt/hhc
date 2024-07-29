package controller.physical2.common

import controller.common.Controller
import controller.common.ControllerState
import kotlinx.cinterop.MemScope
import platform.posix.pollfd

interface PhysicalController2 : Controller {

    context(MemScope)
    fun start2(): List<pollfd>
    fun stop()

    fun consumeControllerState(state: ControllerState)
}