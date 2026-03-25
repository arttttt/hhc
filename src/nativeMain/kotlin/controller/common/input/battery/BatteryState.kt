package controller.common.input.battery

data class BatteryState(
    var leftLevel: Int = 0,
    var rightLevel: Int = 0,
) {
    val combinedLevel: Int
        get() = minOf(leftLevel, rightLevel)
}
