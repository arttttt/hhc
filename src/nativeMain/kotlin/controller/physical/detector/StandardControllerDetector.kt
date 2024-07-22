package controller.physical.detector

import controller.physical.DeviceType
import controller.physical.InputDeviceHwInfo
import controller.physical.InputDeviceIds
import controller.physical.common.PhysicalController
import controller.physical.factory.PhysicalControllerFactory
import input.EVIOCGID
import input.input_id
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.linux.*
import platform.posix.*
import platform.posix.ioctl
import utils.EVIOCGNAME

class StandardControllerDetector(
    private val factory: PhysicalControllerFactory,
) : ControllerDetector {

    companion object {

        private const val INPUT_DIR_PATH = "/dev/input"
        private const val DEVICE_PREFIX = "event"
    }

    override fun controllerEventsFlow(): Flow<ControllerDetector.ControllerEvent> {
        return callbackFlow {
            detectControllers().forEach { controller ->
                trySend(ControllerDetector.ControllerEvent.Attached(controller))
            }

            val fd = inotify_init1(IN_CLOEXEC)
            if (fd < 0) {
                perror("inotify_init1")
                close() // Закрываем Flow, если произошла ошибка
                return@callbackFlow
            }

            val wd = inotify_add_watch(fd, INPUT_DIR_PATH, (IN_CREATE or IN_DELETE).toUInt())
            if (wd < 0) {
                perror("inotify_add_watch")
                close()
                return@callbackFlow
            }

            memScoped {
                val pollFd = alloc<pollfd>()
                pollFd.fd = fd
                pollFd.events = POLLIN.toShort()
                val buffer = ByteArray(4096)

                while (true) {
                    ensureActive()

                    val pollResult = poll(pollFd.ptr, 1u, 1000)
                    if (pollResult == -1) {
                        perror("poll")
                        break
                    }

                    if (pollFd.revents.toInt() and POLLIN != 0) {
                        val length = read(fd, buffer.refTo(0), buffer.size.toULong())
                        if (length < 0) {
                            perror("read")
                            break
                        }

                        var offset = 0
                        while (offset < length) {
                            val event = buffer.refTo(offset).getPointer(this).reinterpret<inotify_event>().pointed

                            if (event.len > 0u) {
                                val name = event.name.toKString()

                                val path = "$INPUT_DIR_PATH/${name}"

                                when {
                                    !name.startsWith(DEVICE_PREFIX) -> {
                                        /**
                                         * do nothing
                                         */
                                    }
                                    event.mask.toInt() and IN_CREATE != 0 -> {
                                        delay(1000)

                                        ensureActive()

                                        /*getControllerInfo(path)
                                            ?.let(factory::create)
                                            ?.let(ControllerDetector.ControllerEvent::Attached)
                                            ?.let(::trySend)*/
                                    }
                                    event.mask.toInt() and IN_DELETE != 0 -> {
                                        trySend(ControllerDetector.ControllerEvent.Detached(path))
                                    }
                                }
                            }

                            offset += (sizeOf<inotify_event>().toInt() + event.len.toInt())
                        }
                    }
                }
            }

            awaitClose {
                println("clearing resources in controllerEventsFlow")
                inotify_rm_watch(fd, wd)
                close(fd)
            }
        }
    }

    override fun detectControllers(): List<PhysicalController> {
        val controllers = mutableListOf<PhysicalController>()

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
                factory.create(devices)
            }
        }

        return controllers
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