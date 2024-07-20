package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.common.PhysicalController

interface ControllerFactory {

    val vendor: Int
    val product: Int

    fun create(
        hwInfo: InputDeviceHwInfo,
    ): PhysicalController
}