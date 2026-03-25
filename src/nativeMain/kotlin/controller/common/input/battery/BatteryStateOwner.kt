package controller.common.input.battery

interface BatteryStateOwner {

    val batteryState: BatteryState

    fun setBatteryState(level: Int)

    fun setBatteryState(report: ByteArray, leftByteIndex: Int, rightByteIndex: Int)
}
