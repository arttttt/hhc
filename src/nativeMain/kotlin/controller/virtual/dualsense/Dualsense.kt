package controller.virtual.dualsense

import Direction
import controller.common.ControllerState
import controller.common.input.axis.AxisCode
import controller.common.input.axis.AxisStateOwner
import controller.common.input.axis.AxisStateOwnerImpl
import controller.common.input.buttons.Button
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.common.input.buttons.ButtonsStateOwnerImpl
import controller.common.normalization.NormalizationMode
import controller.physical2.common.AxisMapping
import controller.physical2.common.ButtonMapping
import controller.virtual.VirtualControllerConfig
import controller.virtual.common.AbstractVirtualController
import controller.virtual.common.MacAddressFormatter
import controller.virtual.dualsense.constants.*
import input.*
import uhid.BUS_USB
import uhid.UHidEvent
import utils.denormalize

class Dualsense : AbstractVirtualController(
    deviceInfo = VirtualControllerConfig(
        name = TITLE,
        uniq = MacAddressFormatter.format(DS5_EDGE_MAC_ADDR),
        product = PRODUCT,
        vendor = VENDOR,
        version = VERSION,
        country = COUNTRY,
        bus = BUS_USB.toUShort(),
        reportDescriptor = DS5_EDGE_DESCRIPTOR,
    )
) {

    companion object {

        private const val TITLE = "Sony Interactive Entertainment DualSense Edge Wireless Controller"
        private const val PRODUCT: UInt = 0x0DF2u
        private const val VENDOR: UInt = 0x054Cu
        private const val VERSION: UInt = 256u
        private const val COUNTRY: UInt = 0u
    }

    class InputState : ControllerState,
        ButtonsStateOwner by ButtonsStateOwnerImpl(
            buttonsMapping = listOf(
                ButtonMapping(
                    systemCode = BTN_X,
                    code = ButtonCode.X,
                    location = 8 * 8 + 3,
                ),
                ButtonMapping(
                    systemCode = BTN_Y,
                    code = ButtonCode.Y,
                    location = 8 * 8,
                ),
                ButtonMapping(
                    systemCode = BTN_B,
                    code = ButtonCode.B,
                    location = 8 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = BTN_A,
                    code = ButtonCode.A,
                    location = 8 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = BTN_SELECT,
                    code = ButtonCode.SELECT,
                    location = 9 * 8 + 3,
                ),
                ButtonMapping(
                    systemCode = BTN_START,
                    code = ButtonCode.START,
                    location = 9 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = BTN_TL,
                    code = ButtonCode.LB,
                    location = 9 * 8 + 7,
                ),
                ButtonMapping(
                    systemCode = BTN_TR,
                    code = ButtonCode.RB,
                    location = 9 * 8 + 6,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBL,
                    code = ButtonCode.LS,
                    location = 9 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = BTN_THUMBR,
                    code = ButtonCode.RS,
                    location = 9 * 8,
                ),
                ButtonMapping(
                    systemCode = BTN_MODE,
                    code = ButtonCode.MODE,
                    location = 10 * 8 + 7,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_L1,
                    location = 10 * 8 + 3,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_L2,
                    location = 10 * 8 + 1,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R1,
                    location = 10 * 8 + 2,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R2,
                    location = 10 * 8,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.EXTRA_R3,
                    location = 10 * 8 + 4,
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_LEFT,
                    location = ButtonMapping.UNKNOWN_LOCATION
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_UP,
                    location = ButtonMapping.UNKNOWN_LOCATION
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_RIGHT,
                    location = ButtonMapping.UNKNOWN_LOCATION
                ),
                ButtonMapping(
                    systemCode = ButtonMapping.UNKNOWN_SYSTEM_CODE,
                    code = ButtonCode.DPAD_DOWN,
                    location = ButtonMapping.UNKNOWN_LOCATION
                )
            ),
        ),
        AxisStateOwner by AxisStateOwnerImpl(
            axisMapping = buildList {
                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.LT,
                        normalizationMode = NormalizationMode.U8,
                        location = 5 * 8,
                    )
                )

                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.RT,
                        normalizationMode = NormalizationMode.U8,
                        location = 6 * 8,
                    )
                )

                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.LX,
                        location = 1 * 8,
                        normalizationMode = NormalizationMode.U8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.LY,
                        location = 2 * 8,
                        normalizationMode = NormalizationMode.U8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.RX,
                        location = 3 * 8,
                        normalizationMode = NormalizationMode.U8,
                    )
                )
                add(
                    AxisMapping(
                        systemCode = AxisMapping.UNKNOWN_SYSTEM_CODE,
                        code = AxisCode.RY,
                        location = 4 * 8,
                        normalizationMode = NormalizationMode.U8,
                    )
                )
            }
        ) {

        companion object {

            const val REPORT_ID: UByte = 0x01u
        }

        private val rawData: UByteArray = UByteArray(64).apply {
            this[0] = REPORT_ID
        }

        fun getRawData(): UByteArray {
            val dpad = mutableListOf<Button>()
            for ((_, button) in buttonsState) {
                when (button.mapping.code) {
                    ButtonCode.DPAD_LEFT -> dpad.add(0, button)
                    ButtonCode.DPAD_UP -> dpad.add(1, button)
                    ButtonCode.DPAD_RIGHT -> dpad.add(2, button)
                    ButtonCode.DPAD_DOWN -> dpad.add(3, button)
                    else -> {}
                }

                if (button.mapping.location == ButtonMapping.UNKNOWN_LOCATION) continue

                val byteIndex = button.mapping.location / 8
                val bitIndex = 7 - (button.mapping.location % 8)
                val mask = 1 shl bitIndex

                if (button.isPressed) {
                    rawData[byteIndex] = (rawData[byteIndex].toInt() or mask).toUByte()
                } else {
                    rawData[byteIndex] = (rawData[byteIndex].toInt() and mask.inv()).toUByte()
                }
            }

            val direction = convertDpadToDirection(
                left = dpad[0],
                up = dpad[1],
                right = dpad[2],
                down = dpad[3],
            )

            val buttonState = rawData[8].toInt() and 0xF0
            val clearedState = buttonState.toUByte()
            val newDpadState = direction.value and 0x0Fu

            rawData[8] = (clearedState or newDpadState)

            axisState.forEach { (_, axis) ->
                val byteIndex = axis.mapping.location / 8

                if (axis.mapping.code == AxisCode.LY) {
                    println(
                        """
                            norm: ${axis.value}
                            denorm: ${denormalize(axis.value,axis.mapping.normalizationMode).toUByte()}
                            mode: ${axis.mapping.normalizationMode}
                        """.trimIndent()
                    )
                }

                rawData[byteIndex] = denormalize(
                    value = axis.value,
                    mode = axis.mapping.normalizationMode,
                ).toUByte()
            }

            rawData[33] = 0x80u
            rawData[37] = 0x80u

            return rawData
        }

        private fun convertDpadToDirection(
            left: Button,
            up: Button,
            right: Button,
            down: Button,
        ): Direction {
            return when {
                up.isPressed && right.isPressed -> Direction.NorthEast
                up.isPressed && left.isPressed -> Direction.NorthWest
                down.isPressed && right.isPressed -> Direction.SouthEast
                down.isPressed && left.isPressed -> Direction.SouthWest
                up.isPressed -> Direction.North
                right.isPressed -> Direction.East
                down.isPressed -> Direction.South
                left.isPressed -> Direction.West
                else -> Direction.None
            }
        }
    }

    override val controllerState = InputState()

    private val outputReport = CompactOutputDataReport(
        enableRumbleEmulation = false,
    )

    override fun consumeControllerState(state: ControllerState) {
        if (state is ButtonsStateOwner) {
            handleButtonsState(state)
        }

        if (state is AxisStateOwner) {
            handleAxisState(state)
        }

        val data = this.controllerState.getRawData()

        uhidDevice.write(
            UHidEvent.Input(
                data = data
            )
        )
    }

    override fun handleUhidEvent(event: UHidEvent) {
        when (event) {
            is UHidEvent.Start -> println("start")
            is UHidEvent.Open -> println("open")
            is UHidEvent.Close -> println("close")
            is UHidEvent.Stop -> println("stop")
            is UHidEvent.GetReport -> handleGetReport(event)
            is UHidEvent.Output -> handleOutput(event)
            else -> throw IllegalArgumentException("Unsupported event: $event")
        }
    }

    private fun handleButtonsState(state: ButtonsStateOwner) {
        state.buttonsState.forEach { (code, button) ->
            controllerState.setButtonState(
                code = code,
                isPressed = button.isPressed,
            )
        }
    }

    private fun handleAxisState(state: AxisStateOwner) {
        state.axisState.forEach { (code, axis) ->
            controllerState.setAxisState(
                code = code,
                value = axis.value,
                fromMode = axis.mapping.normalizationMode,
            )
        }
    }

    private fun handleOutput(event: UHidEvent.Output) {
        outputReport.setRawData(event.data)
    }

    private fun handleGetReport(event: UHidEvent.GetReport) {
        println("get_report")
        when (event.kind) {
            DS_FEATURE_REPORT_PAIRING_INFO -> {
                val response = UHidEvent.GetReportReply(
                    size = DS_FEATURE_REPORT_PAIRING_INFO_SIZE,
                    id = event.id,
                    err = 0u,
                    data = DS_FEATURE_REPORT_PAIRING_DATA,
                )

                uhidDevice.write(response)
            }

            DS_FEATURE_REPORT_FIRMWARE_INFO -> {
                val response = UHidEvent.GetReportReply(
                    size = DS_FEATURE_REPORT_FIRMWARE_INFO_SIZE,
                    id = event.id,
                    err = 0u,
                    data = DS_FEATURE_REPORT_FIRMWARE_DATA,
                )

                uhidDevice.write(response)
            }

            DS_FEATURE_REPORT_CALIBRATION -> {
                val response = UHidEvent.GetReportReply(
                    size = DS_FEATURE_REPORT_CALIBRATION_SIZE,
                    id = event.id,
                    err = 0u,
                    data = DS_FEATURE_REPORT_CALIBRATION_DATA,
                )

                uhidDevice.write(response)
            }
        }
    }

    private fun Direction.Companion.from(
        rawX: Double,
        rawY: Double,
    ): Direction {
        return when {
            rawX == 0.0 && rawY == -1.0 -> Direction.North
            rawX == 1.0 && rawY == -1.0 -> Direction.NorthEast
            rawX == 1.0 && rawY == 0.0 -> Direction.East
            rawX == 1.0 && rawY == 1.0 -> Direction.SouthEast
            rawX == 0.0 && rawY == 1.0 -> Direction.South
            rawX == -1.0 && rawY == 1.0 -> Direction.SouthWest
            rawX == -1.0 && rawY == 0.0 -> Direction.West
            rawX == -1.0 && rawY == -1.0 -> Direction.NorthWest
            else -> Direction.None
        }
    }
}