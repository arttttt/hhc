package controller.physical2.common

import controller.common.input.buttons.ButtonCode

data class ButtonMapping(
    val systemCode: Int,
    val code: ButtonCode,
    val location: Int,
) {

    companion object {

        const val UNKNOWN_LOCATION = Int.MIN_VALUE
        const val UNKNOWN_SYSTEM_CODE = Int.MIN_VALUE
    }
}