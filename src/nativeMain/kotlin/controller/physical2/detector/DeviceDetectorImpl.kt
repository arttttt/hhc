package controller.physical2.detector

import controller.physical2.common.DeviceType
import controller.physical2.common.InputDeviceHwInfo
import controller.physical2.common.InputDeviceIds
import controller.physical2.common.EvdevDevice
import controller.physical2.common.HidrawDevice
import controller.physical2.common.InputDevice
import controller.physical2.common.PhysicalController2
import controller.physical2.lego.LenovoLegionGoController
import hidraw.hidraw_devinfo
import input.EVIOCGID
import input.input_id
import kotlinx.cinterop.*
import platform.posix.*
import utils.EVIOCGNAME
import utils.HIDIOCGRAWINFO
import utils.HIDIOCGRAWNAME

class DeviceDetectorImpl : DeviceDetector {

    companion object {

        private const val PRODUCT_NAME_PATH = "/sys/devices/virtual/dmi/id/product_name"

        private const val LENOVO_LEGION_GO_PRODUCT_NAME = "83E1"

        private const val HIDRAW_DIR_PATH = "/dev"
        private const val HIDRAW_DEVICE_PREFIX = "hidraw"

        private const val INPUT_DIR_PATH = "/dev/input"
        private const val EVDEV_DEVICE_PREFIX = "event"
    }

    override fun detect(): PhysicalController2? {
        val productName = readProductName()

        return when (productName) {
            LENOVO_LEGION_GO_PRODUCT_NAME -> LenovoLegionGoController(
                devices = buildList {
                    this += findHidrawInputDevices(
                        vid = 0x17ef,
                        pid = 0x6182,
                    )

                    this += findEvdevInputDevices(
                        vid = 0x17ef,
                        pid = 0x6182,
                    )
                },
            )
            else -> null
        }
    }

    private fun findHidrawInputDevices(
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

                if (name.startsWith(HIDRAW_DEVICE_PREFIX)) {
                    val hidrawDevicePath = "$HIDRAW_DIR_PATH/$name"

                    val info = getHidrawControllerInfo(hidrawDevicePath) ?: continue
                    if (info.ids.vendorId != vid || info.ids.productId != pid) continue

                    inputDevices += createInputDevice(info)
                }
            }

            closedir(hidrawDir)
        }

        return inputDevices
    }

    private fun findEvdevInputDevices(
        vid: Int,
        pid: Int,
    ): List<InputDevice> {
        val inputDevices = mutableListOf<InputDevice>()

        val inputDir = opendir(INPUT_DIR_PATH)
        if (inputDir != null) {
            var entry: dirent?

            while (true) {
                entry = readdir(inputDir)?.pointed ?: break

                val name = entry.d_name.toKString()

                if (name.startsWith(EVDEV_DEVICE_PREFIX)) {
                    val eventDevicePath = "$INPUT_DIR_PATH/$name"

                    val info = getEvdevControllerInfo(eventDevicePath) ?: continue
                    if (info.ids.vendorId != vid || info.ids.productId != pid) continue

                    /**
                     * todo: remove later
                     */
                    if (info.name != "Lenovo Legion Controller for Windows") continue

                    inputDevices += createInputDevice(info)
                }
            }
            closedir(inputDir)
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

    private fun getHidrawControllerInfo(
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

                val name = nameBuffer.toKString().trim()
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

    private fun getEvdevControllerInfo(
        eventDevicePath: String
    ): InputDeviceHwInfo? {
        val fd = open(eventDevicePath, O_RDONLY)
        if (fd < 0) {
            perror("Error opening device: $eventDevicePath")
            return null
        }

        return try {
            val name = readName(fd)
            val (vendor, product) = readVendorAndProduct(fd)

            when {
                name != null && vendor != null && product != null -> {
                    InputDeviceHwInfo(
                        name = name,
                        ids = InputDeviceIds(
                            vendorId = vendor,
                            productId = product,
                        ),
                        path = eventDevicePath,
                        type = DeviceType.STANDARD,
                    )
                }
                else -> null
            }
        } finally {
            close(fd)
        }
    }

    private fun readVendorAndProduct(fd: Int): Pair<Int?, Int?> {
        return memScoped {
            val id = alloc<input_id>()
            val ret = ioctl(fd, EVIOCGID, id.ptr)

            when {
                ret < 0 -> null to null
                id.vendor.toInt() == 0 || id.product.toInt() == 0 -> null to null
                else -> id.vendor.toInt() to id.product.toInt()
            }
        }
    }

    private fun readName(fd: Int): String? {
        val buffer = ByteArray(4096)

        val ret = ioctl(fd, EVIOCGNAME(buffer.size.toULong()), buffer.refTo(0))

        return when {
            ret < 0 -> null
            else -> buffer.toKString().trim()
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