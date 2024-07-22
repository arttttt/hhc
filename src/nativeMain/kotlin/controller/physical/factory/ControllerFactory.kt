package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical.common.PhysicalController

interface ControllerFactory {

    val ids: InputDeviceIds

    fun create(
        devices: Set<InputDeviceHwInfo>,
    ): PhysicalController
}