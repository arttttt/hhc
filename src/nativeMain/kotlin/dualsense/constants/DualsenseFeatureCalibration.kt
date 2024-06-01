package dualsense.constants

val DS_FEATURE_REPORT_CALIBRATION: UByte = 0x05u
val DS_FEATURE_REPORT_CALIBRATION_SIZE: UShort = 41u

val DS_FEATURE_REPORT_CALIBRATION_DATA = ubyteArrayOf(
    DS_FEATURE_REPORT_CALIBRATION,
    0xfeu,
    0xffu,
    0xfcu,
    0xffu,
    0xfeu,
    0xffu,
    0x83u,
    0x22u,
    0x78u,
    0xddu,
    0x92u,
    0x22u,
    0x5fu,
    0xddu,
    0x95u,
    0x22u,
    0x6du,
    0xddu,
    0x1cu,
    0x02u,
    0x1cu,
    0x02u,
    0xf2u,
    0x1fu,
    0xedu,
    0xdfu,
    0xe3u,
    0x20u,
    0xdau,
    0xe0u,
    0xeeu,
    0x1fu,
    0xdfu,
    0xdfu,
    0x0bu,
    0x00u,
    0x00u,
    0x00u,
    0x00u,
    0x00u,
)