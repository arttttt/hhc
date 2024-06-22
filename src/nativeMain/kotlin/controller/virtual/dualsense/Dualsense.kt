package controller.virtual.dualsense

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

    private var report = USBPackedInputDataReport.fromByteArray(UByteArray(64)).copy(
        reportId = 0x1u,
        dpad = Direction.None,
    )

    override fun consumeControllerState(state: ControllerState) {
        if (state is ButtonsState) {
            state.buttons.forEach { (button, pressed) ->
                report = when (button) {
                    Button.X -> report.copy(
                        square = pressed,
                    )

                    Button.Y -> report.copy(
                        triangle = pressed,
                    )

                    Button.B -> report.copy(
                        circle = pressed,
                    )

                    Button.A -> report.copy(
                        cross = pressed,
                    )

                    Button.LB -> report.copy(
                        l1 = pressed
                    )

                    Button.RB -> report.copy(
                        r1 = pressed,
                    )

                    Button.LS -> report.copy(
                        l3 = pressed,
                    )

                    Button.RS -> report.copy(
                        r3 = pressed,
                    )

                    Button.MODE -> report.copy(
                        ps = pressed,
                    )

                    Button.SELECT -> report.copy(
                        create = pressed,
                    )

                    Button.START -> report.copy(
                        options = pressed,
                    )
                }
            }
        }

        if (state is AxisState) {
            state.axis.forEach { (axis, value) ->
                report = when (axis) {
                    Axis.LX -> report.copy(
                        joystickLX = axisInfo.getValue(axis).denormalizeSignedValue(value)
                    )
                    Axis.LY -> report.copy(
                        joystickLY = axisInfo.getValue(axis).denormalizeSignedValue(value)
                    )
                    Axis.RX -> report.copy(
                        joystickRX = axisInfo.getValue(axis).denormalizeSignedValue(value)
                    )
                    Axis.RY -> report.copy(
                        joystickRY = axisInfo.getValue(axis).denormalizeSignedValue(value)
                    )
                    Axis.LT -> report.copy(
                        l2Trigger = axisInfo.getValue(axis).denormalize(value).toUByte()
                    )
                    Axis.RT -> report.copy(
                        r2Trigger = axisInfo.getValue(axis).denormalize(value).toUByte()
                    )
                    Axis.HAT0X -> {
                        report.copy(
                            dpad = when (value) {
                                -1.0 -> Direction.West
                                1.0 -> Direction.East
                                else -> Direction.None
                            }
                        )
                    }
                    Axis.HAT0Y -> {
                        report.copy(
                            dpad = when (value) {
                                -1.0 -> Direction.North
                                1.0 -> Direction.South
                                else -> Direction.None
                            }
                        )
                    }
                }
            }
        }

        uhidDevice.write(
            UHidEvent.Input(
                report.toByteArray()
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
}