package controller.common.input.buttons

import controller.physical2.common.ButtonMapping

class ButtonsStateOwnerImpl(
    buttonsMapping: List<ButtonMapping>,
) : ButtonsStateOwner {

    override val buttonsState: Map<ButtonCode, ButtonImpl>
    private val systemButtonsState: Map<Int, ButtonImpl>

    init {
        val buttons = buttonsMapping.map { mapping ->
            ButtonImpl(
                mapping = mapping,
                isPressed = false,
            )
        }

        buttonsState = buttons.associateBy { button -> button.mapping.code }
        systemButtonsState = buttons.associateBy { button -> button.mapping.systemCode }
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

    override fun setButtonsState(report: ByteArray): Boolean {
        var stateChanged = false
        for ((_, button) in buttonsState) {
            if (button.mapping.location == ButtonMapping.UNKNOWN_LOCATION) continue

            val newState = getButtonState(
                report = report,
                location = button.mapping.location
            )

            stateChanged = stateChanged || button.isPressed != newState
            button.isPressed = newState
        }

        return stateChanged
    }

    private fun getButtonState(
        report: ByteArray,
        location: Int,
    ): Boolean {
        val byteIndex = location / 8
        val bitIndex = 7 - (location % 8)
        val value = (report[byteIndex].toInt() and (1 shl bitIndex)) != 0

        return value
    }
}