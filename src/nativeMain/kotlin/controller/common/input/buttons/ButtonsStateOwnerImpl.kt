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
        if (!buttonsState.containsKey(code)) return false

        buttonsState[code]?.isPressed = isPressed

        return true
    }

    override fun setButtonState(code: Int, isPressed: Boolean): Boolean {
        if (!systemButtonsState.containsKey(code)) return false

        systemButtonsState[code]?.isPressed = isPressed

        return true
    }
}