package controller.common.input.axis

import controller.common.normalization.NormalizationMode

interface AxisStateOwner {

    val axisState: Map<AxisCode, Axis>

    /**
     * @param [code] - internal axis code
     * @param [value] - current state of the axis
     *
     * @return true when state was updated successfully
     */
    fun setAxisState(code: AxisCode, value: Int): Boolean

    /**
     * @param [code] - system axis code
     * @param [value] - current state of the axis
     *
     * @return true when state was updated successfully
     */
    fun setAxisState(code: Int, value: Int): Boolean

    /**
     * @param [report] - raw report
     *
     * @return
     */
    fun setAxisState(report: ByteArray): Boolean

    /**
     * @param [code] - system axis code
     * @param [value] - current normalized state of the axis
     * @param [fromMode] - normalization mode of the value
     *
     * @return true when state was updated successfully
     */
    fun setAxisState(code: AxisCode, value: Double, fromMode: NormalizationMode): Boolean
}