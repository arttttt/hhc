package controller.common.input.buttons

import controller.physical2.common.ButtonMapping

data class ButtonImpl(
    override val mapping: ButtonMapping,
    override var isPressed: Boolean,
) : Button