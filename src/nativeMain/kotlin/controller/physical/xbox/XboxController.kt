package controller.physical.xbox

import controller.common.ControllerState
import controller.common.input.axis.AxisCode
import controller.common.input.axis.AxisStateOwner
import controller.common.input.axis.AxisStateOwnerImpl
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.common.normalization.NormalizationInfo
import controller.physical.common.AbstractController
import controller.physical.common.ControllerType
import controller.physical.common.PhysicalController
import controller.physical.factory.ControllerFactory
import input.*
import kotlinx.coroutines.flow.MutableSharedFlow

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

    class InputState : ControllerState,
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
        AxisStateOwner by AxisStateOwnerImpl(
            supportedAxis = buildSet {
                val dpadNormalizationInfo = NormalizationInfo(
                    minimum = -1,
                    maximum = 1,
                )

                add(Triple(ABS_HAT0X, AxisCode.HAT0X, dpadNormalizationInfo))
                add(Triple(ABS_HAT0Y, AxisCode.HAT0Y, dpadNormalizationInfo))

                val triggersNormalizationInfo = NormalizationInfo(
                    minimum = 0,
                    maximum = 1023,
                )

                add(Triple(ABS_Z, AxisCode.LT, triggersNormalizationInfo))
                add(Triple(ABS_RZ, AxisCode.RT, triggersNormalizationInfo))

                val joystickNormalizationInfo = NormalizationInfo(
                    minimum = -32768,
                    maximum = 32767,
                )

                add(Triple(ABS_X, AxisCode.LX, joystickNormalizationInfo))
                add(Triple(ABS_Y, AxisCode.LY, joystickNormalizationInfo))
                add(Triple(ABS_RX, AxisCode.RX, joystickNormalizationInfo))
                add(Triple(ABS_RY, AxisCode.RY, joystickNormalizationInfo))
            }
        )

    override val states = MutableSharedFlow<InputState>(
        extraBufferCapacity = 1,
    )

    private val state = InputState()

    override fun consumeControllerState(state: ControllerState) {
        println("external state received: $state")
        write(state)
    }

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
        if (!state.setAxisState(event.code.toInt(), event.value)) return

        states.tryEmit(state)
    }
}