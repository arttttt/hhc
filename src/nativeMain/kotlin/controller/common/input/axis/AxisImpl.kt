package controller.common.input.axis

import controller.physical2.common.AxisMapping

data class AxisImpl(
    override val mapping: AxisMapping,
    override var value: Double,
) : Axis