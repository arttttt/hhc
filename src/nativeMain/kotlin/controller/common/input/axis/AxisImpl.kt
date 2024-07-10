package controller.common.input.axis

import controller.common.normalization.NormalizationInfo

data class AxisImpl(
    override val code: AxisCode,
    override val systemCode: Int,
    override var value: Double,
    /**
     * raw value constraints for normalization
     */
    val normalizationInfo: NormalizationInfo,
) : Axis {

    override val isSigned: Boolean = normalizationInfo.minimum < 0
}