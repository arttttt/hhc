package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical.common.PhysicalController

class PhysicalControllerFactory(
    private val factories: Map<InputDeviceIds, ControllerFactory>
) {

    fun create(
        hwInfo: InputDeviceHwInfo,
    ): PhysicalController? {
        val factory = factories[hwInfo.ids]?.create(hwInfo) ?: return null

        return factory
    }
}