package controller.common.input.axis

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
}