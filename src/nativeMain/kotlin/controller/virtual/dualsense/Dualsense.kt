package controller.virtual.dualsense

import CompactInputDataReport
import Direction
import TouchData
import TouchFingerData
import USBPackedInputDataReport
import controller.AbsInfo
import controller.common.*
import controller.virtual.VirtualControllerConfig
import controller.virtual.common.AbstractVirtualController
import controller.virtual.common.MacAddressFormatter
import controller.virtual.dualsense.constants.*
import fromByteArray
import kotlinx.coroutines.flow.Flow
import toByteArray
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

    private val axisInfo: Map<Axis, AbsInfo> = buildMap {
        val dpadAbsInfo = AbsInfo(
            minimum = -1,
            maximum = 1,
        )

        put(Axis.HAT0X, dpadAbsInfo)
        put(Axis.HAT0Y, dpadAbsInfo)

        val triggersAbsInfo = AbsInfo(
            minimum = 0,
            maximum = UByte.MAX_VALUE.toInt(),
        )

        put(Axis.LT, triggersAbsInfo)
        put(Axis.RT, triggersAbsInfo)

        val joystickAbsInfo = AbsInfo(
            minimum = 0,
            maximum = UByte.MAX_VALUE.toInt(),
        )

        put(Axis.LX, joystickAbsInfo)
        put(Axis.LY, joystickAbsInfo)
        put(Axis.RX, joystickAbsInfo)
        put(Axis.RY, joystickAbsInfo)
    }

    override val states: Flow<ControllerState>
        get() = TODO("Not yet implemented")

    private val report = CompactInputDataReport(
        joystickLX = axisInfo.getValue(Axis.LX).denormalizeSignedValue(0.0),
        joystickLY = axisInfo.getValue(Axis.LX).denormalizeSignedValue(0.0),
        joystickRY = axisInfo.getValue(Axis.LX).denormalizeSignedValue(0.0),
        joystickRX = axisInfo.getValue(Axis.LX).denormalizeSignedValue(0.0),
        l2Trigger = axisInfo.getValue(Axis.LX).denormalize(0.0).toUByte(),
        r2Trigger = axisInfo.getValue(Axis.LX).denormalize(0.0).toUByte(),
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

    override suspend fun handleInputState(state: ControllerState) {
        when (state) {
            is ButtonsState -> handleButtonsState(state)
            is AxisState -> handleAxisState(state)
        }

        if (state is ButtonsState) {
            handleButtonsState(state)
        }

        if (state is AxisState) {
            handleAxisState(state)
        }

        uhidDevice.write(
            UHidEvent.Input(
                report.getRawData()
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
            is UHidEvent.Output -> println("output")
            else -> throw IllegalArgumentException("Unsupported event: $event")
        }
    }

    private fun handleButtonsState(state: ButtonsState) {
        state.buttons.forEach { (button, pressed) ->
            when (button) {
                Button.X -> report.square = pressed
                Button.Y -> report.triangle = pressed
                Button.B -> report.circle = pressed
                Button.A -> report.cross = pressed
                Button.LB -> report.l1 = pressed
                Button.RB -> report.r1 = pressed
                Button.LS -> report.l3 = pressed
                Button.RS -> report.r3 = pressed
                Button.MODE -> report.ps = pressed
                Button.SELECT -> report.create = pressed
                Button.START -> report.options = pressed
            }
        }
    }

    private fun handleAxisState(state: AxisState) {
        state.axis.forEach { (axis, value) ->
            when (axis) {
                Axis.LX -> report.joystickLX = axisInfo.getValue(axis).denormalizeSignedValue(value)
                Axis.LY -> report.joystickLY = axisInfo.getValue(axis).denormalizeSignedValue(value)
                Axis.RX -> report.joystickRX = axisInfo.getValue(axis).denormalizeSignedValue(value)
                Axis.RY -> report.joystickRY = axisInfo.getValue(axis).denormalizeSignedValue(value)
                Axis.LT -> report.l2Trigger = axisInfo.getValue(axis).denormalize(value).toUByte()
                Axis.RT -> report.r2Trigger = axisInfo.getValue(axis).denormalize(value).toUByte()
                else -> {}
            }
        }

        val (hat0x, hat0y) = state.axis[Axis.HAT0X] to state.axis[Axis.HAT0Y]

        if (hat0x != null && hat0y != null) {
            report.dpad = Direction.from(hat0x, hat0y)
        }
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

    private fun AbsInfo.denormalizeSignedValue(value: Double): UByte {
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

    private fun AbsInfo.denormalize(value: Double): Int {
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