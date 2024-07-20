package controller.physical.detector

import controller.physical.InputDeviceHwInfo
import controller.physical.common.PhysicalController
import controller.physical.factory.PhysicalControllerFactory
import hidraw.hidraw_devinfo
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.linux.*
import platform.posix.*
import platform.posix.ioctl
import utils.HIDIOCGRAWINFO
import utils.HIDIOCGRAWNAME

class HidrawControllerDetector(
    private val factory: PhysicalControllerFactory,
) : ControllerDetector {

    companion object {
        private const val HIDRAW_DIR_PATH = "/dev"
        private const val DEVICE_PREFIX = "hidraw"
    }

    override fun controllerEventsFlow(): Flow<ControllerDetector.ControllerEvent> {
        return callbackFlow {
            detectControllers().forEach { controller ->
                trySend(ControllerDetector.ControllerEvent.Attached(controller))
            }

            val fd = inotify_init1(IN_CLOEXEC)
            if (fd < 0) {
                perror("inotify_init1")
                close()
                return@callbackFlow
            }

            val wd = inotify_add_watch(fd, HIDRAW_DIR_PATH, (IN_CREATE or IN_DELETE).toUInt())
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

                                val path = "$HIDRAW_DIR_PATH/${name}"

                                when {
                                    !name.startsWith(DEVICE_PREFIX) -> {
                                        // Ничего не делаем
                                    }
                                    event.mask.toInt() and IN_CREATE != 0 -> {
                                        delay(1000)

                                        ensureActive()

                                        getControllerInfo(path)
                                            ?.let(factory::create)
                                            ?.let(ControllerDetector.ControllerEvent::Attached)
                                            ?.let(::trySend)
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
                println("Освобождение ресурсов в HidrawControllerDetector")
                inotify_rm_watch(fd, wd)
                close(fd)
            }
        }
    }

    override fun detectControllers(): List<PhysicalController> {
        val controllers = mutableListOf<PhysicalController>()

        val hidrawDir = opendir(HIDRAW_DIR_PATH)
        if (hidrawDir != null) {
            var entry: dirent?

            while (true) {
                entry = readdir(hidrawDir)?.pointed ?: break

                val name = entry.d_name.toKString()

                if (name.startsWith(DEVICE_PREFIX)) {
                    val hidrawDevicePath = "$HIDRAW_DIR_PATH/$name"
                    getControllerInfo(hidrawDevicePath)
                        ?.let(factory::create)
                        ?.let(controllers::add)
                }
            }
            closedir(hidrawDir)
        }

        return controllers
    }

    private fun getControllerInfo(
        hidrawDevicePath: String
    ): InputDeviceHwInfo? {
        val fd = open(hidrawDevicePath, O_RDONLY)
        if (fd < 0) {
            perror("Ошибка при открытии устройства: $hidrawDevicePath")
            return null
        }

        return try {
            memScoped {
                val hidrawDevInfo = alloc<hidraw_devinfo>()
                if (ioctl(fd, HIDIOCGRAWINFO(sizeOf<hidraw_devinfo>().toULong()), hidrawDevInfo.ptr) < 0) {
                    perror("Ошибка при получении информации об устройстве")
                    return null
                }

                val nameBuffer = ByteArray(256)
                if (ioctl(fd, HIDIOCGRAWNAME(256u), nameBuffer.refTo(0)) < 0) {
                    perror("Ошибка при получении имени устройства")
                    return null
                }

                val name = nameBuffer.toKString()
                val vendorId = hidrawDevInfo.vendor.toInt()
                val productId = hidrawDevInfo.product.toInt()

                InputDeviceHwInfo(
                    name = name,
                    vendorId = vendorId,
                    productId = productId,
                    path = hidrawDevicePath,
                )
            }
        } finally {
            close(fd)
        }
    }
}