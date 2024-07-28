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
}