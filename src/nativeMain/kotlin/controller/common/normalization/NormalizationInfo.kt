package controller.common.normalization

import kotlin.math.roundToInt

data class NormalizationInfo(
    val minimum: Int,
    val maximum: Int,
) {

    fun normalize(value: Int): Double {
        return if (minimum < 0) {
            2.0 * (value - minimum).toDouble() / (maximum - minimum).toDouble() - 1.0
        } else {
            (value - minimum).toDouble() / (maximum - minimum).toDouble()
        }
    }

    fun denormalizeSignedValue(value: Double): UByte {
        val mid = (maximum + minimum) / 2.0
        val normalValueAbs = kotlin.math.abs(value)

        return if (value >= 0.0) {
            val maximum = maximum - mid
            (value * maximum + mid).roundToInt().toUByte()
        } else {
            val minimum = minimum - mid
            (normalValueAbs * minimum + mid).roundToInt().toUByte()
        }
    }

    fun denormalize(value: Double): Int {
        return if (minimum < 0) {
            (((value + 1.0) / 2.0) * (maximum - minimum) + minimum).toInt()
        } else {
            ((value * (maximum - minimum)) + minimum).toInt()
        }
    }
}