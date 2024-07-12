package controller.virtual.dualsense

import CompactInputDataReport
import Direction
import controller.common.normalization.NormalizationInfo
import controller.common.*
import controller.common.input.axis.Axis
import controller.common.input.axis.AxisCode
import controller.common.input.axis.AxisStateOwner
import controller.common.input.buttons.ButtonCode
import controller.common.input.buttons.ButtonsStateOwner
import controller.virtual.VirtualControllerConfig
import controller.virtual.common.AbstractVirtualController
import controller.virtual.common.MacAddressFormatter
import controller.virtual.dualsense.constants.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import uhid.BUS_USB
import uhid.UHidEvent
import kotlin.math.roundToInt

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

    private val axisInfo: Map<AxisCode, NormalizationInfo> = buildMap {
        val dpadAbsInfo = NormalizationInfo(
            minimum = -1,
            maximum = 1,
        )

        put(AxisCode.HAT0X, dpadAbsInfo)
        put(AxisCode.HAT0Y, dpadAbsInfo)

        val triggersAbsInfo = NormalizationInfo(
            minimum = 0,
            maximum = UByte.MAX_VALUE.toInt(),
        )

        put(AxisCode.LT, triggersAbsInfo)
        put(AxisCode.RT, triggersAbsInfo)

        val joystickAbsInfo = NormalizationInfo(
            minimum = 0,
            maximum = UByte.MAX_VALUE.toInt(),
        )

        put(AxisCode.LX, joystickAbsInfo)
        put(AxisCode.LY, joystickAbsInfo)
        put(AxisCode.RX, joystickAbsInfo)
        put(AxisCode.RY, joystickAbsInfo)
    }

    override val outputStates = MutableSharedFlow<CompactOutputDataReport>(
        extraBufferCapacity = 1,
    )

    private val inputReport = CompactInputDataReport(
        joystickLX = axisInfo.getValue(AxisCode.LX).denormalizeSignedValue(0.0),
        joystickLY = axisInfo.getValue(AxisCode.LX).denormalizeSignedValue(0.0),
        joystickRY = axisInfo.getValue(AxisCode.LX).denormalizeSignedValue(0.0),
        joystickRX = axisInfo.getValue(AxisCode.LX).denormalizeSignedValue(0.0),
        l2Trigger = axisInfo.getValue(AxisCode.LX).denormalize(0.0).toUByte(),
        r2Trigger = axisInfo.getValue(AxisCode.LX).denormalize(0.0).toUByte(),
        triangle = false,
        circle = false,
        cross = false,
        square = false,
        dpad = Direction.None,
        r3 = false,
        l3 = false,
        options = false,
        create = false,
        r1 = false,
        l1 = false,
        ps = false,
    )

    private val outputReport = CompactOutputDataReport(
        enableRumbleEmulation = false,
        strongRumble = 0.0,
        weakRumble = 0.0,
    )

    override suspend fun handleInputState(state: ControllerState) {
        if (state is ButtonsStateOwner) {
            handleButtonsState(state)
        }

        if (state is AxisStateOwner) {
            handleAxisState(state)
        }

        uhidDevice.write(
            UHidEvent.Input(
                inputReport.getRawData()
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
        state.buttonsState.forEach { (_, button) ->
            when (button.code) {
                ButtonCode.X -> inputReport.square = button.isPressed
                ButtonCode.Y -> inputReport.triangle = button.isPressed
                ButtonCode.B -> inputReport.circle = button.isPressed
                ButtonCode.A -> inputReport.cross = button.isPressed
                ButtonCode.LB -> inputReport.l1 = button.isPressed
                ButtonCode.RB -> inputReport.r1 = button.isPressed
                ButtonCode.LS -> inputReport.l3 = button.isPressed
                ButtonCode.RS -> inputReport.r3 = button.isPressed
                ButtonCode.MODE -> inputReport.ps = button.isPressed
                ButtonCode.SELECT -> inputReport.create = button.isPressed
                ButtonCode.START -> inputReport.options = button.isPressed
            }
        }
    }

    private fun handleAxisState(state: AxisStateOwner) {
        state.axisState.forEach { (_, axis) ->
            when (axis.code) {
                AxisCode.LX -> inputReport.joystickLX = axisInfo.getValue(axis.code).denormalizeSignedValue(axis.value)
                AxisCode.LY -> inputReport.joystickLY = axisInfo.getValue(axis.code).denormalizeSignedValue(axis.value)
                AxisCode.RX -> inputReport.joystickRX = axisInfo.getValue(axis.code).denormalizeSignedValue(axis.value)
                AxisCode.RY -> inputReport.joystickRY = axisInfo.getValue(axis.code).denormalizeSignedValue(axis.value)
                AxisCode.LT -> inputReport.l2Trigger = axisInfo.getValue(axis.code).denormalize(axis.value).toUByte()
                AxisCode.RT -> inputReport.r2Trigger = axisInfo.getValue(axis.code).denormalize(axis.value).toUByte()
                else -> {}
            }
        }

        val (hat0x, hat0y) = state.axisState[AxisCode.HAT0X] to state.axisState[AxisCode.HAT0Y]

        if (hat0x != null && hat0y != null) {
            inputReport.dpad = Direction.from(hat0x.value, hat0y.value)
        }
    }

    private fun handleOutput(event: UHidEvent.Output) {
        outputReport.setRawData(event.data)
        outputStates.tryEmit(outputReport)
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

    private fun NormalizationInfo.denormalizeSignedValue(value: Double): UByte {
        val mid = (maximum + minimum) / 2.0
        val normalValueAbs = kotlin.math.abs(value)

        return if (value >= 0.0) {
            val maximum = maximum - mid
            (value * maximum + mid).roundToInt().toUByte()
        } else {
            val minimum = minimum - mid
            (normalValueAbs * minimum + mid).roundToInt().toUByte()
        }
    }

    private fun NormalizationInfo.denormalize(value: Double): Int {
        return if (minimum < 0) {
            (((value + 1.0) / 2.0) * (maximum - minimum) + minimum).toInt()
        } else {
            ((value * (maximum - minimum)) + minimum).toInt()
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