package controller.physical2.common

import controller.common.ControllerState
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.poll
import platform.posix.pollfd

abstract class AbstractPhysicalController(
    protected val devices: List<InputDevice>
) : PhysicalController2 {

    protected class DeviceMapKey(
        val pollfd: pollfd,
    ) {

        val fd by pollfd::fd

        override fun equals(other: Any?): Boolean {
            if (other !is DeviceMapKey) return false

            if (fd != other.fd) return false

            return true
        }

        override fun hashCode(): Int {
            return fd.hashCode()
        }
    }

    private val deviceMap = mutableMapOf<DeviceMapKey, InputDevice>()

    protected val inputEventsScope = CoroutineScope(newSingleThreadContext("physical_controller_input_scope") + SupervisorJob())

    protected abstract val inputState: ControllerState

    protected abstract fun onStateUpdated()

    override fun start(){
        inputEventsScope.launch {
            memScoped {
                devices.associateByTo(deviceMap) { device ->
                    DeviceMapKey(
                        pollfd = device.open()
                    )
                }

                startInputEventsLoop()
            }
        }
    }

    override fun stop() {
        inputEventsScope.coroutineContext.cancel()

        deviceMap.forEach { (pollfd, device) ->
            close(pollfd.fd)
            device.close()
        }
    }

    private suspend fun startInputEventsLoop() {
        memScoped {
            val nativeFds = allocArray<pollfd>(deviceMap.keys.size)
            deviceMap.keys.forEachIndexed { index, key ->
                nativeFds[index].fd = key.pollfd.fd
                nativeFds[index].events = key.pollfd.events
                nativeFds[index].revents = key.pollfd.revents
            }

            while (true) {
                currentCoroutineContext().ensureActive()

                val ret = poll(nativeFds, deviceMap.keys.size.toULong(), 1000)

                if (ret == -1) throw IllegalStateException("Can not start polling")

                deviceMap.keys.forEachIndexed { index, key ->
                    key.pollfd.revents = nativeFds[index].revents
                    key.pollfd.events = nativeFds[index].events
                }

                var stateChanged = false
                deviceMap.forEach { (pfd, device) ->
                    if (pfd.pollfd.revents.toInt() and POLLIN != 0) {
                        val rawData = ByteArray(256)
                        val bytesRead = device.read(rawData)

                        if (bytesRead > 0) {
                            stateChanged = stateChanged || device.processRawData(
                                rawData = rawData,
                                state = inputState,
                            )
                        }
                    }
                }

                if (stateChanged) {
                    onStateUpdated()
                }
            }
        }
    }

    data class BM(val loc: Int, val flipped: Boolean = false)
    data class AM(
        val loc: Int,
        val type: String,
        val order: String = "little",
        val scale: Float? = null,
        val offset: Float = 0f,
        val flipped: Boolean = false,
        val bounds: Pair<Int, Int>? = null
    )

    fun parseControllerReport(report: ByteArray) {
        // Проверка правильности ID отчета
        if (report[0] != 0x04.toByte()) {
            println("Неверный ID отчета: ${report[0]}")
            return
        }

        println("Получен корректный отчет с ID: ${report[0]}")

        // Определение маппингов в соответствии с LGO_RAW_INTERFACE_BTN_MAP
        val buttonMapping = mapOf(
            "mode" to BM(18 * 8),
            "share" to BM(18 * 8 + 1),
            "ls" to BM(18 * 8 + 2),
            "rs" to BM(18 * 8 + 3),
            "dpad_up" to BM(18 * 8 + 4),
            "dpad_down" to BM(18 * 8 + 5),
            "dpad_left" to BM(18 * 8 + 6),
            "dpad_right" to BM(18 * 8 + 7),
            "a" to BM(19 * 8),
            "b" to BM(19 * 8 + 1),
            "x" to BM(19 * 8 + 2),
            "y" to BM(19 * 8 + 3),
            "lb" to BM(19 * 8 + 4),
            "lt" to BM(19 * 8 + 5),
            "rb" to BM(19 * 8 + 6),
            "rt" to BM(19 * 8 + 7),
            "extra_l1" to BM(20 * 8),
            "extra_l2" to BM(20 * 8 + 1),
            "extra_r1" to BM(20 * 8 + 2),
            "extra_r2" to BM(20 * 8 + 5),
            "extra_r3" to BM(20 * 8 + 4),
            "start" to BM(20 * 8 + 7),
            "select" to BM(20 * 8 + 6),
            "btn_middle" to BM(21 * 8)
        )

        val axisMapping = mapOf(
            "ls_x" to AM(14 * 8, "m8"),
            "ls_y" to AM(15 * 8, "m8"),
            "rs_x" to AM(16 * 8, "m8"),
            "rs_y" to AM(17 * 8, "m8"),
            "rt" to AM(22 * 8, "u8"),
            "lt" to AM(23 * 8, "u8")
        )

        // Функция для получения состояния кнопки
        fun getButton(report: ByteArray, map: BM): Boolean {
            val byteIndex = map.loc / 8
            val bitIndex = 7 - (map.loc % 8)
            val value = (report[byteIndex].toInt() and (1 shl bitIndex)) != 0
            return if (map.flipped) !value else value
        }

        // Функция для декодирования значения оси
        fun decodeAxis(report: ByteArray, map: AM): Float {
            val byteIndex = map.loc / 8
            val value = when (map.type) {
                "m8" -> {
                    val rawValue = report[byteIndex].toInt() and 0xFF
                    if (rawValue >= 128) rawValue - 256 else rawValue
                }
                "u8" -> report[byteIndex].toInt() and 0xFF
                else -> throw IllegalArgumentException("Unsupported axis type: ${map.type}")
            }

            val normalizedValue = when (map.type) {
                "m8" -> value.toFloat() / 127f
                "u8" -> value.toFloat() / 255f
                else -> throw IllegalArgumentException("Unsupported axis type: ${map.type}")
            }

            val scaledValue = map.scale?.let { normalizedValue * it } ?: normalizedValue
            val offsetValue = scaledValue + map.offset
            return if (map.flipped) -offsetValue else offsetValue
        }

        // Проверка состояния каждой кнопки
        for ((buttonName, mapping) in buttonMapping) {
            val isPressed = getButton(report, mapping)
            println("Кнопка $buttonName: ${if (isPressed) "Нажата" else "Не нажата"}")
        }

        // Проверка значений осей
        for ((axisName, mapping) in axisMapping) {
            val value = decodeAxis(report, mapping)
            println("Ось $axisName: $value")
        }
    }
}