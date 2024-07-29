package controller.physical2.xbox

import controller.common.ControllerState
import controller.common.input.axis.AxisCode
import controller.common.input.axis.AxisStateOwner
import controller.common.input.axis.AxisStateOwnerImpl
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.physical2.common.AbstractPhysicalController
import controller.physical2.common.AxisMapping
import controller.physical2.common.ButtonMapping
import controller.physical2.common.InputDevice
import input.*
import kotlinx.coroutines.flow.MutableSharedFlow

class XboxController(
    devices: List<InputDevice>,
) : AbstractPhysicalController(devices) {

    class InputState : ControllerState,
        ButtonsStateOwner by ButtonsStateOwnerImpl(
            buttonsMapping = listOf(
                ButtonMapping(
                    systemCode = BTN_X,
                    code = ButtonCode.X,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_Y,
                    code = ButtonCode.Y,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_B,
                    code = ButtonCode.B,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_A,
                    code = ButtonCode.A,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_SELECT,
                    code = ButtonCode.SELECT,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_START,
                    code = ButtonCode.START,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_TL,
                    code = ButtonCode.LB,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_TR,
                    code = ButtonCode.RB,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBL,
                    code = ButtonCode.LS,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBR,
                    code = ButtonCode.RS,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
                ButtonMapping(
                    systemCode = BTN_MODE,
                    code = ButtonCode.MODE,
                    location = ButtonMapping.UNKNOWN_LOCATION,
                ),
            ),
        ),
        AxisStateOwner by AxisStateOwnerImpl(
            axisMapping = buildList {
                /*val dpadNormalizationInfo = NormalizationInfo(
                    minimum = -1,
                    maximum = 1,
                )

                add(
                    AxisMapping(
                        systemCode = ABS_HAT0X,
                        code = AxisCode.HAT0X,
                        normalizationInfo = dpadNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_HAT0Y,
                        code = AxisCode.HAT0Y,
                        normalizationInfo = dpadNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )

                val triggersNormalizationInfo = NormalizationInfo(
                    minimum = 0,
                    maximum = 1023,
                )
                add(
                    AxisMapping(
                        systemCode = ABS_Z,
                        code = AxisCode.LT,
                        normalizationInfo = triggersNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RZ,
                        code = AxisCode.RT,
                        normalizationInfo = triggersNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )

                val joystickNormalizationInfo = NormalizationInfo(
                    minimum = -32768,
                    maximum = 32767,
                )

                add(
                    AxisMapping(
                        systemCode = ABS_X,
                        code = AxisCode.LX,
                        normalizationInfo = joystickNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_Y,
                        code = AxisCode.LY,
                        normalizationInfo = joystickNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RX,
                        code = AxisCode.RX,
                        normalizationInfo = joystickNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RY,
                        code = AxisCode.RY,
                        normalizationInfo = joystickNormalizationInfo,
                        location = AxisMapping.UNKNOWN_LOCATION,
                    )
                )*/
            }
        )

    override val controllerState = InputState()

    override fun consumeControllerState(state: ControllerState) {}
}