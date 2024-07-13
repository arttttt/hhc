package controller.virtual.dualsense

import controller.common.ControllerState
import controller.common.normalization.NormalizationInfo
import controller.common.rumble.RumbleStateOwner
import controller.common.rumble.RumbleStateOwnerImpl

@ExperimentalUnsignedTypes
data class CompactOutputDataReport(
    var enableRumbleEmulation: Boolean,
) : ControllerState,
    RumbleStateOwner by RumbleStateOwnerImpl(
        normalizationInfo = NormalizationInfo(
            minimum = UByte.MIN_VALUE.toInt(),
            maximum = UByte.MAX_VALUE.toInt(),
        )
    ) {

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
    setWeakRumbleValue(newData[offset + 2].toInt())
    setStrongRumbleValue(newData[offset + 3].toInt())
}