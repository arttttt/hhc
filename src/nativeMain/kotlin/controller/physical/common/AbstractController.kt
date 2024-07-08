package controller.physical.common

import controller.common.ControllerState
import controller.common.rumble.RumbleHandler
import controller.common.rumble.RumbleState
import input.*
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import platform.posix.*

/**
 * todo: move out rumble handler
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractController(
    final override val name: String,
    final override val path: String,
    val type: ControllerType,
) : PhysicalController {

    protected var fd: Int = -1
        private set

    protected val inputEventsScope = CoroutineScope(newSingleThreadContext("${name}_input_scope") + SupervisorJob())
    protected val outputEventsScope = CoroutineScope(newSingleThreadContext("${name}_output_scope") + SupervisorJob())

    protected val outputEventsChannel = Channel<ControllerState>(Channel.BUFFERED)

    protected val rumbleHandler = RumbleHandler()

    protected abstract fun handleUhidEvent(event: input_event)

    override fun start() {
        inputEventsScope.launch {
            fd = open(path, O_RDWR)

            startInputEventsLoop()
        }

        outputEventsScope.launch {
            startOutputEventsLoop()
        }
    }

    override fun stop() {
        inputEventsScope.coroutineContext.cancel()
    }

    protected fun write(state: ControllerState) {
        outputEventsChannel.trySend(state)
    }

    private suspend fun startOutputEventsLoop() {
        for (event in outputEventsChannel) {
            handleOutputState(event)
        }
    }

    private suspend fun startInputEventsLoop() {
        memScoped {
            val pollFd = alloc<pollfd>().apply {
                fd = this@AbstractController.fd
                events = POLLIN.toShort()
            }

            while (true) {
                currentCoroutineContext().ensureActive()

                val ret = poll(pollFd.ptr, 1u, -1)

                if (ret == -1) throw IllegalStateException("Can not start polling")

                if (pollFd.revents.toInt() and POLLIN != 0) {
                    val ev = alloc<input_event>()
                    val bytesRead = read(fd, ev.ptr, sizeOf<input_event>().toULong())
                    if (bytesRead < 0) {
                        perror("Error reading from device")
                        break
                    }

                    if (ev.type.toInt() == EV_SYN) continue

                    handleUhidEvent(ev)
                }
            }
        }
    }

    private suspend fun handleOutputState(state: ControllerState) {
        when (state) {
            is RumbleState -> handleVibration(state)
        }
    }

    private suspend fun handleVibration(state: RumbleState) {
        when {
            state.isEmpty() -> rumbleHandler.clearRumbleEffect(fd)
            else -> rumbleHandler.rumble(fd, state)
        }
    }
}