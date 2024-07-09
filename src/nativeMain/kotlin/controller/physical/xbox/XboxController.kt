package controller.physical.xbox

import controller.AbsInfo
import controller.common.*
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.physical.common.AbstractController
import controller.physical.common.ControllerType
import controller.physical.common.PhysicalController
import controller.physical.factory.ControllerFactory
import input.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.collections.set

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

    data class InputState(
        override val axis: MutableMap<Axis, Double>,
    ) : ControllerState,
        ButtonsStateOwner by ButtonsStateOwnerImpl(
            supportedButtons = mapOf(
                BTN_X to ButtonCode.X,
                BTN_Y to ButtonCode.Y,
                BTN_B to ButtonCode.B,
                BTN_A to ButtonCode.A,
                BTN_SELECT to ButtonCode.SELECT,
                BTN_START to ButtonCode.START,
                BTN_TL to ButtonCode.LB,
                BTN_TR to ButtonCode.RB,
                BTN_THUMBL to ButtonCode.LS,
                BTN_THUMBR to ButtonCode.RS,
                BTN_MODE to ButtonCode.MODE,
            ),
        ),
        AxisState

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

    override val states = MutableSharedFlow<InputState>(
        extraBufferCapacity = 1,
    )

    private val state = InputState(
        axis = Axis
            .entries
            .associateWith { axis ->
                axisInfo.getValue(axis).normalize(0)
            }
            .toMutableMap(),
    )

    override fun consumeControllerState(state: ControllerState) {}

    override fun handleUhidEvent(event: input_event) {
        when (event.type.toInt()) {
            EV_KEY -> handleKeys(event)
            EV_ABS -> handleAxis(event)
        }
    }

    private fun handleKeys(event: input_event) {
        if (!state.setButtonState(event.code.toInt(), event.value == 1)) return

        states.tryEmit(state)
    }

    private fun handleAxis(event: input_event) {
        val axis = codeToAxis(event.code.toInt()) ?: return

        state.axis[axis] = axisInfo.getValue(axis).normalize(event.value)
        states.tryEmit(state)
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
    private fun AbsInfo.normalize(value: Int): Double {
        return if (minimum < 0) {
            2.0 * (value - minimum).toDouble() / (maximum - minimum).toDouble() - 1.0
        } else {
            (value - minimum).toDouble() / (maximum - minimum).toDouble()
        }
    }
}