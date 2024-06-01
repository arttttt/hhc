package dualsense

import VirtualController
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

        private const val DS_INPUT_REPORT_USB = 0x01
        private const val DS_INPUT_REPORT_USB_SIZE = 64
        private const val DS_INPUT_REPORT_BT = 0x31
        private const val DS_INPUT_REPORT_BT_SIZE = 78
        private const val DS_OUTPUT_REPORT_USB = 0x02
        private const val DS_OUTPUT_REPORT_USB_SIZE = 63
        private const val DS_OUTPUT_REPORT_BT = 0x31
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
        reportDescriptor = DS5_EDGE_DESCRIPTOR.toUByteArray(),
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

    private fun formatMacAddress(macAddr: ByteArray): String {
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
                    data = ubyteArrayOf(
                        DS_FEATURE_REPORT_PAIRING_INFO,
                        DS5_EDGE_MAC_ADDR[0].toUByte(),
                        DS5_EDGE_MAC_ADDR[1].toUByte(),
                        DS5_EDGE_MAC_ADDR[2].toUByte(),
                        DS5_EDGE_MAC_ADDR[3].toUByte(),
                        DS5_EDGE_MAC_ADDR[4].toUByte(),
                        DS5_EDGE_MAC_ADDR[5].toUByte(),
                        0x08u,
                        0x25u,
                        0x00u,
                        0x1eu,
                        0x00u,
                        0xeeu,
                        0x74u,
                        0xd0u,
                        0xbcu,
                        0x00u,
                        0x00u,
                        0x00u,
                        0x00u,
                    ),
                )

                uhidDevice.write(response)
            }

            DS_FEATURE_REPORT_FIRMWARE_INFO -> {
                val response = UHidEvent.GetReportReply(
                    size = DS_FEATURE_REPORT_FIRMWARE_INFO_SIZE,
                    id = event.id,
                    err = 0u,
                    data = ubyteArrayOf(
                        DS_FEATURE_REPORT_FIRMWARE_INFO,
                        0x4a.toUByte(),
                        0x75.toUByte(),
                        0x6e.toUByte(),
                        0x20.toUByte(),
                        0x31.toUByte(),
                        0x39.toUByte(),
                        0x20.toUByte(),
                        0x32.toUByte(),
                        0x30.toUByte(),
                        0x32.toUByte(),
                        0x33.toUByte(),
                        0x31.toUByte(),
                        0x34.toUByte(),
                        0x3a.toUByte(),
                        0x34.toUByte(),
                        0x37.toUByte(),
                        0x3a.toUByte(),
                        0x33.toUByte(),
                        0x34.toUByte(),
                        0x03.toUByte(),
                        0x00.toUByte(),
                        0x44.toUByte(),
                        0x00.toUByte(),
                        0x08.toUByte(),
                        0x02.toUByte(),
                        0x00.toUByte(),
                        0x01.toUByte(),
                        0x36.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x01.toUByte(),
                        0xc1.toUByte(),
                        0xc8.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x54.toUByte(),
                        0x01.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x14.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x0b.toUByte(),
                        0x00.toUByte(),
                        0x01.toUByte(),
                        0x00.toUByte(),
                        0x06.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                    ),
                )

                uhidDevice.write(response)
            }

            DS_FEATURE_REPORT_CALIBRATION -> {
                val response = UHidEvent.GetReportReply(
                    size = DS_FEATURE_REPORT_CALIBRATION_SIZE,
                    id = event.id,
                    err = 0u,
                    data = ubyteArrayOf(
                        DS_FEATURE_REPORT_CALIBRATION,
                        0xfe.toUByte(),
                        0xff.toUByte(),
                        0xfc.toUByte(),
                        0xff.toUByte(),
                        0xfe.toUByte(),
                        0xff.toUByte(),
                        0x83.toUByte(),
                        0x22.toUByte(),
                        0x78.toUByte(),
                        0xdd.toUByte(),
                        0x92.toUByte(),
                        0x22.toUByte(),
                        0x5f.toUByte(),
                        0xdd.toUByte(),
                        0x95.toUByte(),
                        0x22.toUByte(),
                        0x6d.toUByte(),
                        0xdd.toUByte(),
                        0x1c.toUByte(),
                        0x02.toUByte(),
                        0x1c.toUByte(),
                        0x02.toUByte(),
                        0xf2.toUByte(),
                        0x1f.toUByte(),
                        0xed.toUByte(),
                        0xdf.toUByte(),
                        0xe3.toUByte(),
                        0x20.toUByte(),
                        0xda.toUByte(),
                        0xe0.toUByte(),
                        0xee.toUByte(),
                        0x1f.toUByte(),
                        0xdf.toUByte(),
                        0xdf.toUByte(),
                        0x0b.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                        0x00.toUByte(),
                    ),
                )

                uhidDevice.write(response)
            }
        }
    }
}