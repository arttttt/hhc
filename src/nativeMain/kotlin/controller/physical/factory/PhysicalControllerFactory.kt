package controller.physical.factory

import controller.physical.InputDeviceHwInfo
import controller.physical.common.PhysicalController

class PhysicalControllerFactory {

    fun create(
        hwInfo: InputDeviceHwInfo,
    ): PhysicalController? {
        return when {
            hwInfo.vendorId == 0x045e && hwInfo.productId == 0x0b12 -> {
                println("xbox controller found")

                null
            }
            else -> null
        }
    }
}