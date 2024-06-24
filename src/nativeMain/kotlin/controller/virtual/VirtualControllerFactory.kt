package controller.virtual

import controller.virtual.common.VirtualController

fun interface VirtualControllerFactory {
    fun create(): VirtualController
}