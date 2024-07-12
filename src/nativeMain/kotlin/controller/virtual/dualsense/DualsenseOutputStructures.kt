package controller.virtual.dualsense

import controller.common.ControllerState
import controller.common.rumble.RumbleState

@ExperimentalUnsignedTypes
data class CompactOutputDataReport(
    var enableRumbleEmulation: Boolean,
    override var strongRumble: Double,
    override var weakRumble: Double
) : ControllerState,
    RumbleState {

    companion object {

        const val REPORT_ID: UByte = 0x02u

        /**
         * 47 - it's original size
         * 48 - with the report id
         */
        private const val REPORT_SIZE = 48
    }

    private val rawData: UByteArray = UByteArray(64).apply {
        this[0] = REPORT_ID
    }

    fun setRawData(data: UByteArray) {
        if (data.size != REPORT_SIZE) return

        /**
         * fill required fields
         */
        updateFields(
            newData = data
        )

        /**
         * copy original output
         */
        for (i in 1 until data.size) {
            rawData[i] = data[i]
        }
    }
}

private fun CompactOutputDataReport.updateFields(
    newData: UByteArray,
) {
    val offset = 1

    enableRumbleEmulation = (newData[offset + 0] and 0b00000001u) != 0x0u.toUByte()
    weakRumble = newData[offset + 2].toDouble() / 255.0
    strongRumble = newData[offset + 3].toDouble() / 255.0
}