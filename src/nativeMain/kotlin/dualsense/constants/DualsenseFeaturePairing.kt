package dualsense.constants

val DS_FEATURE_REPORT_PAIRING_INFO: UByte = 0x09u
val DS_FEATURE_REPORT_PAIRING_INFO_SIZE: UShort = 20u

val DS_FEATURE_REPORT_PAIRING_DATA = ubyteArrayOf(
    DS_FEATURE_REPORT_PAIRING_INFO,
    DS5_EDGE_MAC_ADDR[0],
    DS5_EDGE_MAC_ADDR[1],
    DS5_EDGE_MAC_ADDR[2],
    DS5_EDGE_MAC_ADDR[3],
    DS5_EDGE_MAC_ADDR[4],
    DS5_EDGE_MAC_ADDR[5],
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
)