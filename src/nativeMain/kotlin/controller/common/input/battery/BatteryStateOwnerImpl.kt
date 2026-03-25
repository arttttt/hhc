package controller.common.input.battery

class BatteryStateOwnerImpl : BatteryStateOwner {

    override val batteryState = BatteryState()

    override fun setBatteryState(level: Int) {
        batteryState.leftLevel = level.coerceIn(0, 100)
        batteryState.rightLevel = level.coerceIn(0, 100)
    }

    override fun setBatteryState(report: ByteArray, leftByteIndex: Int, rightByteIndex: Int) {
        batteryState.leftLevel = (report[leftByteIndex].toInt() and 0xFF).coerceIn(0, 100)
        batteryState.rightLevel = (report[rightByteIndex].toInt() and 0xFF).coerceIn(0, 100)
    }
}
