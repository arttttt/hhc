package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical.common.PhysicalController

class PhysicalControllerFactory(
    private val factories: Map<InputDeviceIds, ControllerFactory>
) {

    fun create(
        devices: Set<InputDeviceHwInfo>,
    ): PhysicalController? {
        val hwInfo = devices.firstOrNull() ?: return null

        val factory = factories[hwInfo.ids]?.create(devices) ?: return null

        return factory
    }
}