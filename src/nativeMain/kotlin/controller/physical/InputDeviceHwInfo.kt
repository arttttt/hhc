package controller.physical

data class InputDeviceHwInfo(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val path: String,
)