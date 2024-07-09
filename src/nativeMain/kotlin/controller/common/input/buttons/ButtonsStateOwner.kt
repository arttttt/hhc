package controller.common.input.buttons

interface ButtonsStateOwner {

    val buttonsState: Map<ButtonCode, Button>

    /**
     * @param [code] - internal button code
     * @param [isPressed] - current state of the button
     *
     * @return true when state was updated successfully
     */
    fun setButtonState(code: ButtonCode, isPressed: Boolean): Boolean

    /**
     * @param [code] - system button code
     * @param [isPressed] - current state of the button
     *
     * @return true when state was updated successfully
     */
    fun setButtonState(code: Int, isPressed: Boolean): Boolean
}