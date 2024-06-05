package dualsense

import VirtualController
import dualsense.constants.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import uhid.*

class Dualsense : VirtualController {

    companion object {

        private const val TITLE = "Sony Interactive Entertainment DualSense Edge Wireless Controller"
        private const val PRODUCT: UInt = 0x0DF2u
        private const val VENDOR: UInt = 0x054Cu
        private const val VERSION: UInt = 256u
        private const val COUNTRY: UInt = 0u
    }

    private val scope = CoroutineScope(newSingleThreadContext("DualSenseDispatcher") + SupervisorJob())

    private val uhidDevice = UHidDevice(
        name = TITLE,
        uniq = formatMacAddress(DS5_EDGE_MAC_ADDR),
        product = PRODUCT,
        vendor = VENDOR,
        version = VERSION,
        country = COUNTRY,
        bus = BUS_USB.toUShort(),
        reportDescriptor = DS5_EDGE_DESCRIPTOR,
    )

    override fun create() {
        uhidDevice.open()
        uhidDevice.create()
        startControllerLoop()
    }

    override fun destroy() {
        uhidDevice.write(UHidEvent.Destroy)
        uhidDevice.close()
    }

    private fun startControllerLoop() {
        scope.launch {
            memScoped {
                val pollFd = alloc<pollfd>().apply {
                    fd = uhidDevice.fd
                    events = POLLIN.toShort()
                }

                while (true) {
                    ensureActive()

                    val ret = poll(pollFd.ptr, 1u, -1)

                    if (ret == -1) throw IllegalStateException("Can not start polling")

                    if (pollFd.revents.toInt() and POLLIN != 0) {
                        val event = uhidDevice.read()

                        handleUhidEvent(event)
                    }
                }
            }
        }
    }

    private fun formatMacAddress(macAddr: UByteArray): String {
        require(macAddr.size == 6) { "MAC address must be 6 bytes long" }
        return macAddr
            .reversed()
            .joinToString(":") { byte ->
                byte
                    .toInt()
                    .and(0xFF)
                    .toString(16)
                    .padStart(2, '0')
            }
    }

    private fun handleUhidEvent(event: UHidEvent) {
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