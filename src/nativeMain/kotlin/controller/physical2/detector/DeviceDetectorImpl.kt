package controller.physical2.detector

import controller.physical.DeviceType
import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical2.common.EvdevDevice
import controller.physical2.common.HidrawDevice
import controller.physical2.common.InputDevice
import controller.physical2.common.PhysicalController2
import controller.physical2.lego.LenovoLegionGoController
import hidraw.hidraw_devinfo
import kotlinx.cinterop.*
import platform.posix.*
import utils.HIDIOCGRAWINFO
import utils.HIDIOCGRAWNAME

class DeviceDetectorImpl : DeviceDetector {

    companion object {

        private const val PRODUCT_NAME_PATH = "/sys/devices/virtual/dmi/id/product_name"

        private const val LENOVO_LEGION_GO_PRODUCT_NAME = "83E1"

        private const val HIDRAW_DIR_PATH = "/dev"
        private const val DEVICE_PREFIX = "hidraw"
    }

    override fun detect(): PhysicalController2? {
        val productName = readProductName()

        return when (productName) {
            LENOVO_LEGION_GO_PRODUCT_NAME -> LenovoLegionGoController(
                devices = findInputDevices(
                    vid = 0x17ef,
                    pid = 0x6182,
                ),
            )
            else -> null
        }
    }

    private fun findInputDevices(
        vid: Int,
        pid: Int,
    ): List<InputDevice> {
        val inputDevices = mutableListOf<InputDevice>()

        val hidrawDir = opendir(HIDRAW_DIR_PATH)
        if (hidrawDir != null) {
            var entry: dirent?

            while (true) {
                entry = readdir(hidrawDir)?.pointed ?: break

                val name = entry.d_name.toKString()

                if (name.startsWith(DEVICE_PREFIX)) {
                    val hidrawDevicePath = "$HIDRAW_DIR_PATH/$name"

                    val info = getControllerInfo(hidrawDevicePath) ?: continue

                    if (info.ids.vendorId != vid || info.ids.productId != pid) continue

                    inputDevices += createInputDevice(info)
                }
            }

            closedir(hidrawDir)
        }

        return inputDevices
    }

    private fun readProductName(): String {
        val file = fopen(PRODUCT_NAME_PATH, "r") ?: throw IllegalStateException("Не удалось открыть файл")

        try {
            val buffer = ByteArray(256)
            val readBytes = fread(buffer.refTo(0), 1u, buffer.size.convert(), file)
            return buffer.decodeToString(endIndex = readBytes.toInt()).trim()
        } finally {
            fclose(file)
        }
    }

    private fun getControllerInfo(
        hidrawDevicePath: String
    ): InputDeviceHwInfo? {
        val fd = open(hidrawDevicePath, O_RDONLY)
        if (fd < 0) {
            perror("Can't open device: $hidrawDevicePath")
            return null
        }

        return try {
            memScoped {
                val hidrawDevInfo = alloc<hidraw_devinfo>()
                if (ioctl(fd, HIDIOCGRAWINFO(sizeOf<hidraw_devinfo>().toULong()), hidrawDevInfo.ptr) < 0) {
                    perror("Can't get device info")
                    return null
                }

                val nameBuffer = ByteArray(256)
                if (ioctl(fd, HIDIOCGRAWNAME(256u), nameBuffer.refTo(0)) < 0) {
                    perror("Can't get device name")
                    return null
                }

                val name = nameBuffer.toKString()
                val vendorId = hidrawDevInfo.vendor.toInt()
                val productId = hidrawDevInfo.product.toInt()

                InputDeviceHwInfo(
                    name = name,
                    ids = InputDeviceIds(
                        vendorId = vendorId,
                        productId = productId,
                    ),
                    path = hidrawDevicePath,
                    type = DeviceType.HIDRAW,
                )
            }
        } finally {
            close(fd)
        }
    }

    private fun createInputDevice(
        hwInfo: InputDeviceHwInfo,
    ): InputDevice {
        return when (hwInfo.type) {
            DeviceType.HIDRAW -> HidrawDevice(hwInfo)
            DeviceType.STANDARD -> EvdevDevice(hwInfo)
        }
    }
}