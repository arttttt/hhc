package controller.physical.common

import events.InputEvent
import input.EV_SYN
import input.input_event
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.posix.*

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractController(
    override val name: String,
    val type: ControllerType,
    protected val path: String,
) : PhysicalController {

    override val events = MutableSharedFlow<InputEvent>(
        extraBufferCapacity = 1,
    )

    protected var fd: Int = -1
        private set

    protected val scope = CoroutineScope(newSingleThreadContext("PhysicalControllerDispatcher") + SupervisorJob())

    protected abstract fun handleUhidEvent(event: input_event)

    override fun start() {
        fd = open(path, O_RDWR)

        startControllerLoop()
    }

    override fun stop() {
        scope.coroutineContext.cancel()
    }

    protected fun write() {

    }

    private fun startControllerLoop() {
        scope.launch {
            memScoped {
                val pollFd = alloc<pollfd>().apply {
                    fd = this@AbstractController.fd
                    events = POLLIN.toShort()
                }

                while (true) {
                    ensureActive()

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
    }
}