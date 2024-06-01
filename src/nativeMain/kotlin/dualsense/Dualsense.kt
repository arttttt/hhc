package dualsense

import VirtualController
import uhid.UHidDevice
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import uhid.BUS_USB
import uhid.uhid_event
import uhid.uhid_event_type

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

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
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
        memScoped {
            val destroyEvent = alloc<uhid_event>().apply {
                type = uhid_event_type.UHID_DESTROY.value
            }

            uhidDevice.write(destroyEvent)
        }

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
                        val event = uhidDevice.read(alloc<uhid_event>())

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

    private fun handleUhidEvent(event: uhid_event) {
        memScoped {
            when (event.type) {
                uhid_event_type.UHID_START.value -> println("start")
                uhid_event_type.UHID_OPEN.value -> println("open")
                uhid_event_type.UHID_CLOSE.value -> println("close")
                uhid_event_type.UHID_STOP.value -> println("stop")
                uhid_event_type.UHID_GET_REPORT.value -> {
                    println("get_report")

                    when (event.u.get_report.rnum) {
                        DS_FEATURE_REPORT_PAIRING_INFO -> {
                            val macAddrResponse = alloc<uhid_event>().apply {
                                type = uhid_event_type.UHID_GET_REPORT_REPLY.value
                                u.get_report_reply.apply {
                                    size = DS_FEATURE_REPORT_PAIRING_INFO_SIZE
                                    id = event.u.get_report.id
                                    err = 0u

                                    data[0] = DS_FEATURE_REPORT_PAIRING_INFO
                                    data[1] = DS5_EDGE_MAC_ADDR[0].toUByte()
                                    data[2] = DS5_EDGE_MAC_ADDR[1].toUByte()
                                    data[3] = DS5_EDGE_MAC_ADDR[2].toUByte()
                                    data[4] = DS5_EDGE_MAC_ADDR[3].toUByte()
                                    data[5] = DS5_EDGE_MAC_ADDR[4].toUByte()
                                    data[6] = DS5_EDGE_MAC_ADDR[5].toUByte()
                                    data[7] = 0x08u
                                    data[8] = 0x25u
                                    data[9] = 0x00u
                                    data[10] = 0x1eu
                                    data[11] = 0x00u
                                    data[12] = 0xeeu
                                    data[13] = 0x74u
                                    data[14] = 0xd0u
                                    data[15] = 0xbcu
                                    data[16] = 0x00u
                                    data[17] = 0x00u
                                    data[18] = 0x00u
                                    data[19] = 0x00u
                                }
                            }

                            println("write macaddr")

                            uhidDevice.write(macAddrResponse)
                        }
                        DS_FEATURE_REPORT_FIRMWARE_INFO -> {
                            val firmwareInfoResponse = alloc<uhid_event>().apply {
                                type = uhid_event_type.UHID_GET_REPORT_REPLY.value
                                u.get_report_reply.apply {
                                    size = DS_FEATURE_REPORT_FIRMWARE_INFO_SIZE
                                    id = event.u.get_report.id
                                    err = 0u
                                    data[0] = DS_FEATURE_REPORT_FIRMWARE_INFO.toUByte()
                                    data[1] = 0x4a.toUByte()
                                    data[2] = 0x75.toUByte()
                                    data[3] = 0x6e.toUByte()
                                    data[4] = 0x20.toUByte()
                                    data[5] = 0x31.toUByte()
                                    data[6] = 0x39.toUByte()
                                    data[7] = 0x20.toUByte()
                                    data[8] = 0x32.toUByte()
                                    data[9] = 0x30.toUByte()
                                    data[10] = 0x32.toUByte()
                                    data[11] = 0x33.toUByte()
                                    data[12] = 0x31.toUByte()
                                    data[13] = 0x34.toUByte()
                                    data[14] = 0x3a.toUByte()
                                    data[15] = 0x34.toUByte()
                                    data[16] = 0x37.toUByte()
                                    data[17] = 0x3a.toUByte()
                                    data[18] = 0x33.toUByte()
                                    data[19] = 0x34.toUByte()
                                    data[20] = 0x03.toUByte()
                                    data[21] = 0x00.toUByte()
                                    data[22] = 0x44.toUByte()
                                    data[23] = 0x00.toUByte()
                                    data[24] = 0x08.toUByte()
                                    data[25] = 0x02.toUByte()
                                    data[26] = 0x00.toUByte()
                                    data[27] = 0x01.toUByte()
                                    data[28] = 0x36.toUByte()
                                    data[29] = 0x00.toUByte()
                                    data[30] = 0x00.toUByte()
                                    data[31] = 0x01.toUByte()
                                    data[32] = 0xc1.toUByte()
                                    data[33] = 0xc8.toUByte()
                                    data[34] = 0x00.toUByte()
                                    data[35] = 0x00.toUByte()
                                    data[36] = 0x00.toUByte()
                                    data[37] = 0x00.toUByte()
                                    data[38] = 0x00.toUByte()
                                    data[39] = 0x00.toUByte()
                                    data[40] = 0x00.toUByte()
                                    data[41] = 0x00.toUByte()
                                    data[42] = 0x00.toUByte()
                                    data[43] = 0x00.toUByte()
                                    data[44] = 0x54.toUByte()
                                    data[45] = 0x01.toUByte()
                                    data[46] = 0x00.toUByte()
                                    data[47] = 0x00.toUByte()
                                    data[48] = 0x14.toUByte()
                                    data[49] = 0x00.toUByte()
                                    data[50] = 0x00.toUByte()
                                    data[51] = 0x00.toUByte()
                                    data[52] = 0x0b.toUByte()
                                    data[53] = 0x00.toUByte()
                                    data[54] = 0x01.toUByte()
                                    data[55] = 0x00.toUByte()
                                    data[56] = 0x06.toUByte()
                                    data[57] = 0x00.toUByte()
                                    data[58] = 0x00.toUByte()
                                    data[59] = 0x00.toUByte()
                                    data[60] = 0x00.toUByte()
                                    data[61] = 0x00.toUByte()
                                    data[62] = 0x00.toUByte()
                                    data[63] = 0x00.toUByte()
                                }
                            }

                            uhidDevice.write(firmwareInfoResponse)
                        }
                        DS_FEATURE_REPORT_CALIBRATION -> {
                            val calibrationInfo = alloc<uhid_event>().apply {
                                type = uhid_event_type.UHID_GET_REPORT_REPLY.value
                                u.get_report_reply.apply {
                                    size = DS_FEATURE_REPORT_CALIBRATION_SIZE
                                    id = event.u.get_report.id
                                    err = 0u
                                    data[0] = DS_FEATURE_REPORT_CALIBRATION
                                    data[1] = 0xfe.toUByte()
                                    data[2] = 0xff.toUByte()
                                    data[3] = 0xfc.toUByte()
                                    data[4] = 0xff.toUByte()
                                    data[5] = 0xfe.toUByte()
                                    data[6] = 0xff.toUByte()
                                    data[7] = 0x83.toUByte()
                                    data[8] = 0x22.toUByte()
                                    data[9] = 0x78.toUByte()
                                    data[10] = 0xdd.toUByte()
                                    data[11] = 0x92.toUByte()
                                    data[12] = 0x22.toUByte()
                                    data[13] = 0x5f.toUByte()
                                    data[14] = 0xdd.toUByte()
                                    data[15] = 0x95.toUByte()
                                    data[16] = 0x22.toUByte()
                                    data[17] = 0x6d.toUByte()
                                    data[18] = 0xdd.toUByte()
                                    data[19] = 0x1c.toUByte()
                                    data[20] = 0x02.toUByte()
                                    data[21] = 0x1c.toUByte()
                                    data[22] = 0x02.toUByte()
                                    data[23] = 0xf2.toUByte()
                                    data[24] = 0x1f.toUByte()
                                    data[25] = 0xed.toUByte()
                                    data[26] = 0xdf.toUByte()
                                    data[27] = 0xe3.toUByte()
                                    data[28] = 0x20.toUByte()
                                    data[29] = 0xda.toUByte()
                                    data[30] = 0xe0.toUByte()
                                    data[31] = 0xee.toUByte()
                                    data[32] = 0x1f.toUByte()
                                    data[33] = 0xdf.toUByte()
                                    data[34] = 0xdf.toUByte()
                                    data[35] = 0x0b.toUByte()
                                    data[36] = 0x00.toUByte()
                                    data[37] = 0x00.toUByte()
                                    data[38] = 0x00.toUByte()
                                    data[39] = 0x00.toUByte()
                                    data[40] = 0x00.toUByte()
                                }
                            }

                            uhidDevice.write(calibrationInfo)
                        }
                    }
                }
            }
        }
    }
}