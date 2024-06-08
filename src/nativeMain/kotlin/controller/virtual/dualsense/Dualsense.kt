package controller.virtual.dualsense

import controller.virtual.VirtualControllerConfig
import controller.virtual.common.AbstractVirtualController
import controller.virtual.common.MacAddressFormatter
import controller.virtual.dualsense.constants.*
import uhid.BUS_USB
import uhid.UHidEvent

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
}