package controller.common.input.axis

import controller.physical2.common.AxisMapping

interface Axis {

    val mapping: AxisMapping

    /**
     * normalized value
     */
    val value: Double
}