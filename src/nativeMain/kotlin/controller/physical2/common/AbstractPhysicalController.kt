package controller.physical2.common

import controller.common.ControllerState
import kotlinx.cinterop.MemScope
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.pollfd

abstract class AbstractPhysicalController(
    protected val devices: List<InputDevice>
) : PhysicalController2 {

    override var onControllerStateChanged: ((ControllerState) -> Unit)? = null

    private val pollFdsMap = mutableMapOf<Int, pollfd>()
    private val devicesMap = mutableMapOf<Int, InputDevice>()

    context(MemScope)
    override fun start2(): List<pollfd> {
        val pollFds = devices.associateBy { device -> device.open() }

        pollFds
            .map { (pollFd, _) ->
                pollFd.fd to pollFd
            }
            .toMap(pollFdsMap)

        pollFds
            .map { (pollFd, device) ->
                pollFd.fd to device
            }
            .toMap(devicesMap)

        return pollFds.keys.toList()
    }

    override fun stop() {
        pollFdsMap.forEach { (_, value) ->
            close(value.fd)
        }

        devicesMap.forEach { (_, device) ->
            device.close()
        }
    }

    context(MemScope)
    override fun readEvents() {

        var stateChanged = false

        pollFdsMap.entries.forEach { (_, pollfd) ->
            if (pollfd.revents.toInt() and POLLIN != 0) {
                val device = devicesMap.getValue(pollfd.fd)

                val rawData = ByteArray(256)
                val bytesRead = device.read(rawData)

                if (bytesRead > 0) {
                    stateChanged = stateChanged || device.processRawData(
                        rawData = rawData,
                        state = controllerState,
                    )
                }
            }
        }

        if (stateChanged) {
            onControllerStateChanged?.invoke(controllerState)
        }
    }
}