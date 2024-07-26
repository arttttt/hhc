package controller.common.input.buttons

import controller.physical2.common.ButtonMapping

interface Button {

    val mapping: ButtonMapping
    val isPressed: Boolean
}