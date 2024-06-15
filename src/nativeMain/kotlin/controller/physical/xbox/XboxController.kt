package controller.physical.xbox

import controller.physical.common.AbstractController
import controller.physical.common.ControllerType
import controller.physical.common.PhysicalController
import controller.physical.factory.ControllerFactory
import events.Axis
import events.Button
import events.InputEvent
import events.InputEventPool
import input.*

class XboxController(
    path: String,
) : AbstractController(
    name = "Xbox Series X/S Controller",
    type = ControllerType.GAMEPAD,
    path = path,
) {

    object Factory : ControllerFactory {

        override val vendor: Int = 0x045e
        override val product: Int = 0x0b12

        override fun create(
            path: String,
        ): PhysicalController {
            return XboxController(
                path = path,
            )
        }
    }

    override fun handleUhidEvent(event: input_event) {
        val type = event.type.toInt()

        val mappedEvent = when {
            type == EV_KEY -> handleKeys(event)
            type == EV_ABS && isDpadCode(event.code.toInt()) -> handleDpad(event)
            type == EV_ABS -> handleAxis(event)
            else -> null
        }

        mappedEvent?.let(events::tryEmit)
    }

    private fun handleKeys(event: input_event): InputEvent? {
        val button = codeToButton(event.code.toInt()) ?: return null

        return InputEventPool.obtainButtonEvent(
            timestamp = 0,
            button = button,
            pressed = event.value == 1
        )
    }

    private fun handleAxis(event: input_event): InputEvent? {
        val axis = codeToAxis(event.code.toInt()) ?: return null

        return InputEventPool.obtainAxisEvent(
            timestamp = 0,
            axis = axis,
            value = event.value.toDouble(),
        )
    }

    /**
     * todo: implement dpad
     */
    private fun handleDpad(event: input_event): InputEvent? {
        return null
/*        println(event.code.toHexString())
        println(event.value)

        val button = event.toDpadButton() ?: return null

        return InputEventPool.obtainButtonEvent(
            timestamp = 0,
            button = button,
            pressed = event.value != 0,
        )*/
    }

    private fun input_event.toDpadButton(): Button? {
        return when (code.toInt()) {
            ABS_HAT0X -> {
                null
            }
            ABS_HAT0Y -> {
                null
            }
            else -> null
        }
    }

    private fun codeToAxis(code: Int): Axis? {
        return when (code) {
            ABS_Z -> Axis.LT
            ABS_RZ -> Axis.RT
            ABS_X -> Axis.LX
            ABS_Y -> Axis.LY
            ABS_RX -> Axis.RX
            ABS_RY -> Axis.RY
            else -> null
        }
    }

    private fun codeToButton(code: Int): Button? {
        return when (code) {
            BTN_X -> Button.X
            BTN_Y -> Button.Y
            BTN_B -> Button.B
            BTN_A -> Button.A
            BTN_SELECT -> Button.SELECT
            BTN_START ->Button.START
            BTN_TL -> Button.LB
            BTN_TR -> Button.RB
            BTN_THUMBL -> Button.LS
            BTN_THUMBR -> Button.RS
            BTN_MODE -> Button.MODE
            else -> null
        }
    }

    private fun isDpadCode(code: Int): Boolean {
        return code == ABS_HAT0X || code == ABS_HAT0Y
    }
}