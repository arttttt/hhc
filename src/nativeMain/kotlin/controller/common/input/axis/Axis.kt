package controller.common.input.axis

interface Axis {

    val systemCode: Int
    val code: AxisCode

    /**
     * normalized value
     */
    val value: Double

    /**
     * can the value be negative or not
     */
    val isSigned: Boolean
}