package controller.physical2.lego

import controller.common.ControllerState
import controller.common.input.axis.AxisCode
import controller.common.input.axis.AxisStateOwner
import controller.common.input.axis.AxisStateOwnerImpl
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.common.normalization.NormalizationMode
import controller.physical2.common.AbstractPhysicalController
import controller.physical2.common.AxisMapping
import controller.physical2.common.ButtonMapping
import controller.physical2.common.InputDevice
import input.*

class LenovoLegionGoController(
    devices: List<InputDevice>
) : AbstractPhysicalController(devices) {

    class InputState : ControllerState,
        ButtonsStateOwner by ButtonsStateOwnerImpl(
            buttonsMapping = listOf(
                ButtonMapping(
                    systemCode = BTN_X,
                    code = ButtonCode.X,
                    location = 19 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = BTN_Y,
                    code = ButtonCode.Y,
                    location = 19 * 8 + 3,
                ),
                ButtonMapping(
                    systemCode = BTN_B,
                    code = ButtonCode.B,
                    location = 19 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = BTN_A,
                    code = ButtonCode.A,
                    location = 19 * 8,
                ),
                ButtonMapping(
                    systemCode = BTN_SELECT,
                    code = ButtonCode.SELECT,
                    location = 20 * 8 + 6,
                ),
                ButtonMapping(
                    systemCode = BTN_START,
                    code = ButtonCode.START,
                    location = 20 * 8 + 7,
                ),
                ButtonMapping(
                    systemCode = BTN_TL,
                    code = ButtonCode.LB,
                    location = 19 * 8 + 4,
                ),
                ButtonMapping(
                    systemCode = BTN_TR,
                    code = ButtonCode.RB,
                    location = 19 * 8 + 6,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBL,
                    code = ButtonCode.LS,
                    location = 18 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBR,
                    code = ButtonCode.RS,
                    location = 18 * 8 + 3,
                ),
                ButtonMapping(
                    systemCode = BTN_MODE,
                    code = ButtonCode.MODE,
                    location = 18 * 8,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.SHARE,
                    location = 18 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_L1,
                    location = 20 * 8,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_L2,
                    location = 20 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R1,
                    location = 20 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R2,
                    location = 20 * 8 + 5,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R3,
                    location = 20 * 8 + 4,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_LEFT,
                    location = 18 * 8 + 6,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_UP,
                    location = 18 * 8 + 4,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_RIGHT,
                    location = 18 * 8 + 7,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_DOWN,
                    location = 18 * 8 + 5,
                ),
            ),
        ),
        AxisStateOwner by AxisStateOwnerImpl(
            axisMapping = buildList {
                add(
                    AxisMapping(
                        systemCode = ABS_Z,
                        code = AxisCode.LT,
                        normalizationMode = NormalizationMode.U8,
                        location = 22 * 8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RZ,
                        code = AxisCode.RT,
                        normalizationMode = NormalizationMode.U8,
                        location = 23 * 8,
                    )
                )

                add(
                    AxisMapping(
                        systemCode = ABS_X,
                        code = AxisCode.LX,
                        normalizationMode = NormalizationMode.M8,
                        location = 14 * 8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_Y,
                        code = AxisCode.LY,
                        normalizationMode = NormalizationMode.M8,
                        location = 15 * 8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RX,
                        code = AxisCode.RX,
                        normalizationMode = NormalizationMode.M8,
                        location = 16 * 8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = ABS_RY,
                        code = AxisCode.RY,
                        normalizationMode = NormalizationMode.M8,
                        location = 17 * 8,
                    )
                )
            }
        )

    override val controllerState = InputState()

    override fun consumeControllerState(state: ControllerState) {}
}