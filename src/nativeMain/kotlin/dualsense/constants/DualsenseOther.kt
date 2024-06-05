package dualsense.constants

val DS5_INPUT_REPORT_USB_OFS = 1
val DS5_INPUT_REPORT_BT_OFS = 2

val DS5_EDGE_MAC_ADDR = ubyteArrayOf(0x74u, 0xE7u, 0xD6u, 0x3Au, 0x53u, 0x35u)

val DS_INPUT_REPORT_USB_SIZE = 64.toUShort()
const val DS_INPUT_REPORT_USB = 0x01
const val DS_INPUT_REPORT_BT = 0x31
const val DS_INPUT_REPORT_BT_SIZE = 78
val DS_OUTPUT_REPORT_USB = 0x02.toUByte()
val DS_OUTPUT_REPORT_USB_SIZE = 63.toUByte()
const val DS_OUTPUT_REPORT_BT = 0x31