package controller.common.input.buttons

interface Button {

    val systemCode: Int
    val code: ButtonCode
    val isPressed: Boolean
}