package controller.common.input.buttons

data class ButtonImpl(
    override val code: ButtonCode,
    override val systemCode: Int,
    override var isPressed: Boolean,
) : Button