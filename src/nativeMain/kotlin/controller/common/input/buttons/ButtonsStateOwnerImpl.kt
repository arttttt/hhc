package controller.common.input.buttons

class ButtonsStateOwnerImpl(
    supportedButtons: Map<Int, ButtonCode>
) : ButtonsStateOwner {

    override val buttonsState: Map<ButtonCode, ButtonImpl>
    private val systemButtonsState: Map<Int, ButtonImpl>

    init {
        val buttons = supportedButtons.map { (systemCode, code) ->
            ButtonImpl(
                code = code,
                systemCode = systemCode,
                isPressed = false,
            )
        }

        buttonsState = buttons.associateBy(Button::code)
        systemButtonsState = buttons.associateBy(Button::systemCode)
    }

    override fun setButtonState(code: ButtonCode, isPressed: Boolean): Boolean {
        buttonsState[code]?.isPressed = isPressed

        return buttonsState.containsKey(code)
    }

    override fun setButtonState(code: Int, isPressed: Boolean): Boolean {
        systemButtonsState[code]?.isPressed = isPressed

        return systemButtonsState.containsKey(code)
    }
}