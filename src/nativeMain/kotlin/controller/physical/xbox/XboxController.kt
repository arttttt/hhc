package controller.physical.xbox

import controller.AbsInfo
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

    private val axisInfo: Map<Axis, AbsInfo> = buildMap {
        val dpadAbsInfo = AbsInfo(
            minimum = -1,
            maximum = 1,
        )

        put(Axis.HAT0X, dpadAbsInfo)
        put(Axis.HAT0Y, dpadAbsInfo)

        val triggersAbsInfo = AbsInfo(
            minimum = 0,
            maximum = 1023,
        )

        put(Axis.LT, triggersAbsInfo)
        put(Axis.RT, triggersAbsInfo)

        val joystickAbsInfo = AbsInfo(
            minimum = -32768,
            maximum = 32767,
        )

        put(Axis.LX, joystickAbsInfo)
        put(Axis.LY, joystickAbsInfo)
        put(Axis.RX, joystickAbsInfo)
        put(Axis.RY, joystickAbsInfo)
    }

    override fun handleUhidEvent(event: input_event) {
        val mappedEvent = when (event.type.toInt()) {
            EV_KEY -> handleKeys(event)
            EV_ABS -> handleAxis(event)
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
            value = axisInfo.getValue(axis).normalize(event.value),
        )
    }

    private fun codeToAxis(code: Int): Axis? {
        return when (code) {
            ABS_Z -> Axis.LT
            ABS_RZ -> Axis.RT
            ABS_X -> Axis.LX
            ABS_Y -> Axis.LY
            ABS_RX -> Axis.RX
            ABS_RY -> Axis.RY
            ABS_HAT0X -> Axis.HAT0X
            ABS_HAT0Y -> Axis.HAT0Y
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
}