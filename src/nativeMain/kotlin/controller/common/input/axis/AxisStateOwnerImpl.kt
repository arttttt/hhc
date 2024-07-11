package controller.common.input.axis

import controller.common.normalization.NormalizationInfo

class AxisStateOwnerImpl(
    supportedAxis: Set<Triple<Int, AxisCode, NormalizationInfo>>,
) : AxisStateOwner {

    override val axisState: Map<AxisCode, AxisImpl>
    private val systemAxisState: Map<Int, AxisImpl>

    init {
        val axis = supportedAxis.map { (systemCode, code, normalizationInfo) ->
            AxisImpl(
                code = code,
                systemCode = systemCode,
                value = normalizationInfo.normalize(0),
                normalizationInfo = normalizationInfo,
            )
        }

        axisState = axis.associateBy(Axis::code)
        systemAxisState = axis.associateBy(Axis::systemCode)
    }

    override fun setAxisState(code: AxisCode, value: Int): Boolean {
        val axis = axisState[code] ?: return false

        axis.value = axis.normalizationInfo.normalize(value)

        return true
    }

    override fun setAxisState(code: Int, value: Int): Boolean {
        val axis = systemAxisState[code] ?: return false

        axis.value = axis.normalizationInfo.normalize(value)

        return true
    }

    private fun NormalizationInfo.normalize(value: Int): Double {
        return if (minimum < 0) {
            2.0 * (value - minimum).toDouble() / (maximum - minimum).toDouble() - 1.0
        } else {
            (value - minimum).toDouble() / (maximum - minimum).toDouble()
        }
    }
}