package controller.physical2.common

import controller.common.input.axis.AxisCode
import controller.common.normalization.NormalizationMode

data class AxisMapping(
    val systemCode: Int,
    val code: AxisCode,
    val location: Int,
    val normalizationMode: NormalizationMode,
) {

    companion object {

        const val UNKNOWN_LOCATION = Int.MIN_VALUE
        const val UNKNOWN_SYSTEM_CODE = Int.MIN_VALUE
    }
}