package controller.physical2.detector

import controller.physical2.common.DeviceType
import controller.physical2.common.InputDeviceHwInfo
import controller.physical2.common.InputDeviceIds
import controller.physical2.common.EvdevDevice
import controller.physical2.common.HidrawDevice
import controller.physical2.common.PhysicalController2
import controller.physical2.xbox.XboxController
import input.EVIOCGID
import input.input_id
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import platform.posix.*
import utils.EVIOCGNAME

class ControllerDetector2Impl : ControllerDetector2 {

    companion object {

        private const val INPUT_DIR_PATH = "/dev/input"
        private const val DEVICE_PREFIX = "event"
    }

    override fun detectControllers(): List<PhysicalController2> {
        val controllers = mutableListOf<PhysicalController2>()

        val inputDir = opendir(INPUT_DIR_PATH)
        if (inputDir != null) {
            var entry: dirent?

            val hwInfoMap = mutableMapOf<InputDeviceIds, MutableSet<InputDeviceHwInfo>>()
            while (true) {
                entry = readdir(inputDir)?.pointed ?: break

                val name = entry.d_name.toKString()

                if (name.startsWith(DEVICE_PREFIX)) {
                    val eventDevicePath = "$INPUT_DIR_PATH/$name"

                    val info = getControllerInfo(eventDevicePath) ?: continue

                    if (info.ids.vendorId != 0x045e || info.ids.productId != 0x0b12) continue

                    val devices = hwInfoMap.getOrPut(
                        key = info.ids,
                        defaultValue = {
                            mutableSetOf()
                        }
                    )

                    devices += info
                }
            }
            closedir(inputDir)

            hwInfoMap.mapNotNullTo(controllers) { (_, devices) ->
                val sss = devices.map { device ->
                    when (device.type) {
                        DeviceType.HIDRAW -> HidrawDevice(device)
                        DeviceType.STANDARD -> EvdevDevice(device)
                    }
                }

                XboxController(sss)
            }
        }

        return controllers
    }

    override fun controllerEventsFlow(): Flow<ControllerDetector2.ControllerEvent> {
        return emptyFlow()
    }

    private fun getControllerInfo(
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
            else -> buffer.toKString()
        }
    }
}