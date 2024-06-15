package controller

data class AbsInfo(
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

    fun denormalize(value: Double): Int {
        return if (minimum < 0) {
            (((value + 1.0) / 2.0) * (maximum - minimum) + minimum).toInt()
        } else {
            ((value * (maximum - minimum)) + minimum).toInt()
        }
    }
}