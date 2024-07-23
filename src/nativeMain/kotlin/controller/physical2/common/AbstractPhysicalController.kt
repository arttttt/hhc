package controller.physical2.common

import controller.common.ControllerState
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import utils.toCArrayPointer

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
            val nativeFds = deviceMap.map { (key, _) -> key.pollfd }.toTypedArray().toCArrayPointer()

            while (true) {
                currentCoroutineContext().ensureActive()

                val ret = poll(nativeFds, deviceMap.size.toULong(), 1000)

                if (ret == -1) throw IllegalStateException("Can not start polling")

                var stateChanged = false
                deviceMap.forEach { (pfd, device) ->
                    if (pfd.pollfd.revents.toInt() and POLLIN != 0) {
                        val rawData = ByteArray(256)
                        val bytesRead = device.read(rawData)

                        if (bytesRead > 0) {
                            device.processRawData(
                                rawData = rawData,
                                state = inputState,
                            )

                            stateChanged = true
                        }
                    }
                }

                if (stateChanged) {
                    onStateUpdated()
                }
            }
        }
    }
}