package controller.physical2.common

data class InputDeviceHwInfo(
    val name: String,
    val path: String,
    val ids: InputDeviceIds,
    val type: DeviceType,
)