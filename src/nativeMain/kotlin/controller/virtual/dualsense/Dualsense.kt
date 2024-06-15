package controller.virtual.dualsense

import USBPackedInputDataReport
import controller.AbsInfo
import controller.virtual.VirtualControllerConfig
import controller.virtual.common.AbstractVirtualController
import controller.virtual.common.MacAddressFormatter
import controller.virtual.dualsense.constants.*
import events.*
import fromByteArray
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

    private var report = USBPackedInputDataReport.fromByteArray(UByteArray(64)).copy(
        reportId = 0x1u,
        dpad = Direction.None,
    )

    override fun consumeInputEvent(event: InputEvent) {
        when (event) {
            is ButtonEvent -> {
                report = when (event.button) {
                    Button.X -> report.copy(
                        square = event.pressed,
                    )
                    Button.Y -> report.copy(
                        triangle = event.pressed,
                    )
                    Button.B -> report.copy(
                        circle = event.pressed,
                    )
                    Button.A -> report.copy(
                        cross = event.pressed,
                    )
                    Button.LB -> report.copy(
                        l1 = event.pressed
                    )
                    Button.RB -> report.copy(
                        r1 = event.pressed,
                    )
                    Button.LS -> report.copy(
                        l3 = event.pressed,
                    )
                    Button.RS -> report.copy(
                        r3 = event.pressed,
                    )
                    Button.MODE -> report.copy(
                        ps = event.pressed,
                    )
                    Button.SELECT -> report.copy(
                        create = event.pressed,
                    )
                    Button.START -> report.copy(
                        options = event.pressed,
                    )
                }

                uhidDevice.write(
                    UHidEvent.Input(
                        report.toByteArray()
                    )
                )
            }
            is AxisEvent -> {
                report = when (event.axis) {
                    Axis.LX -> report.copy(
                        joystickLX = axisInfo.getValue(event.axis).denormalizeSignedValue(event.value)
                    )
                    Axis.LY -> report.copy(
                        joystickLY = axisInfo.getValue(event.axis).denormalizeSignedValue(event.value)
                    )
                    Axis.RX -> report.copy(
                        joystickRX = axisInfo.getValue(event.axis).denormalizeSignedValue(event.value)
                    )
                    Axis.RY -> report.copy(
                        joystickRY = axisInfo.getValue(event.axis).denormalizeSignedValue(event.value)
                    )
                    Axis.LT -> report.copy(
                        l2Trigger = axisInfo.getValue(event.axis).denormalize(event.value).toUByte()
                    )
                    Axis.RT -> report.copy(
                        r2Trigger = axisInfo.getValue(event.axis).denormalize(event.value).toUByte()
                    )
                    Axis.HAT0X -> report.copy(
                        dpad = when (event.value) {
                            -1.0 -> Direction.West
                            1.0 -> Direction.East
                            else -> Direction.None
                        }
                    )
                    Axis.HAT0Y -> report.copy(
                        dpad = when (event.value) {
                            -1.0 -> Direction.North
                            1.0 -> Direction.South
                            else -> Direction.None
                        }
                    )
                }

                uhidDevice.write(
                    UHidEvent.Input(
                        report.toByteArray()
                    )
                )
            }
            else -> {}
        }
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
            val value = value * maximum + mid
            value.roundToInt().toUByte()
        } else {
            val minimum = minimum - mid
            val value = normalValueAbs * minimum + mid
            value.roundToInt().toUByte()
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