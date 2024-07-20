package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.common.PhysicalController

class PhysicalControllerFactory(
    private val factories: Map<Pair<Int, Int>, ControllerFactory>
) {

    fun create(
        hwInfo: InputDeviceHwInfo,
    ): PhysicalController? {
        val factory = factories[hwInfo.vendorId to hwInfo.productId]?.create(hwInfo) ?: return null

        return factory
    }
}