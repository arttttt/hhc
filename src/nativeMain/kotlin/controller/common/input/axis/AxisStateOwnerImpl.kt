package controller.common.input.axis

import controller.common.normalization.NormalizationMode
import controller.physical2.common.AxisMapping
import utils.convertNormalizedValue
import utils.denormalize
import utils.normalize

@Suppress("NAME_SHADOWING")
class AxisStateOwnerImpl(
    axisMapping: List<AxisMapping>,
) : AxisStateOwner {

    override val axisState: Map<AxisCode, AxisImpl>
    private val systemAxisState: Map<Int, AxisImpl>

    init {
        val axis = axisMapping.map { mapping ->
            AxisImpl(
                mapping = mapping,
                value = normalize(0, mapping.normalizationMode),
            )
        }

        axisState = axis.associateBy { axis -> axis.mapping.code }
        systemAxisState = axis.associateBy { axis -> axis.mapping.systemCode }
    }

    override fun setAxisState(code: AxisCode, value: Int): Boolean {
        val axis = axisState[code] ?: return false

        axis.value = normalize(
            value = value,
            mode = axis.mapping.normalizationMode,
        )

        return true
    }

    override fun setAxisState(code: Int, value: Int): Boolean {
        val axis = systemAxisState[code] ?: return false

        axis.value = normalize(
            value = value,
            mode = axis.mapping.normalizationMode,
        )

        return true
    }

    override fun setAxisState(report: ByteArray): Boolean {
        for ((_, axis) in axisState) {
            if (axis.mapping.location == AxisMapping.UNKNOWN_LOCATION) continue

            val value = getAxisState(
                report = report,
                location = axis.mapping.location,
            )

            axis.value = normalize(
                value = value,
                mode = axis.mapping.normalizationMode,
            )
        }

        return true
    }

    override fun setAxisState(code: AxisCode, value: Double, fromMode: NormalizationMode): Boolean {
        val axis = axisState[code] ?: return false

        axis.value = convertNormalizedValue(
            value = value,
            fromMode = fromMode,
            toMode = axis.mapping.normalizationMode,
        )

        return true
    }

    private fun getAxisState(
        report: ByteArray,
        location: Int,
    ): Int {
        val byteIndex = location / 8

        return report[byteIndex].toInt()
    }

    private fun decodeM8(value: Byte): Double {
        val intValue = (value.toInt() and 0xFF) - (1 shl 7)
        val s = (1 shl 7).toDouble()

        return intValue / s
    }
}