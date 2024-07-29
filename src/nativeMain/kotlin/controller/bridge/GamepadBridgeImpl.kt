package controller.bridge

import controller.common.Controller
import controller.physical2.common.PhysicalController2
import controller.physical2.detector.ControllerDetector2
import controller.physical2.detector.DeviceDetector
import controller.physical2.detector.DeviceDetectorImpl
import controller.virtual.VirtualControllerFactory
import controller.virtual.common.VirtualController
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.posix.poll
import platform.posix.pollfd

class GamepadBridgeImpl(
    private val controllerDetector: ControllerDetector2,
    private val virtualControllerFactory: VirtualControllerFactory
) : GamepadBridge {
    private val detectionContext = newFixedThreadPoolContext(2, "ControllerDetectionContext")
    private val bridgeContext = newSingleThreadContext("ControllerBridgeContext")

    private val detectionScope = CoroutineScope(SupervisorJob() + detectionContext + CoroutineExceptionHandler { _, throwable ->
        /**
         * do nothing
         */
    })
    private val bridgeScope = CoroutineScope(SupervisorJob() + bridgeContext)

    private var activeController: PhysicalController2? = null
    private var virtualController: VirtualController? = null

    private val mutex = Mutex()

    private val deviceDetector: DeviceDetector = DeviceDetectorImpl()

    override fun start() {
        detectionScope.launch {
            val controller = deviceDetector.detect() ?: controllerDetector
                .detectControllers()
                .firstOrNull()

            if (controller != null) {
                connectController(controller)
            }

/*            controllerDetector
                .controllerEventsFlow()
                .filter { event ->
                    when (event) {
                        is ControllerDetector.ControllerEvent.Attached -> getActiveController() == null
                        is ControllerDetector.ControllerEvent.Detached -> getActiveController()?.hwInfo?.path == event.path
                    }
                }
                .collect { event ->
                    when (event) {
                        is ControllerDetector.ControllerEvent.Attached -> connectController(event.controller)
                        is ControllerDetector.ControllerEvent.Detached -> disconnectController()
                    }
                }*/
        }
    }

    override fun stop() {
        runBlocking {
            disconnectController()
        }
    }

    override fun shutdown() {
        detectionScope.cancel()
        bridgeScope.cancel()
        detectionContext.close()
        bridgeContext.close()
        stop()
    }

    private suspend fun connectController(
        controller: PhysicalController2,
    ) {
        mutex.withLock {
            activeController = controller
        }

        if (virtualController != null) {
            virtualController!!.destroy()
            virtualController = null
        }

        virtualController = virtualControllerFactory.create()

        bridgeScope.launch {
            memScoped {
                val virtualControllerPollFd = virtualController!!.create2()
                val physicalControllerPollFds = controller.start2()

                val controllers = mapOf(
                    controller to physicalControllerPollFds,
                    virtualController!! to listOf(virtualControllerPollFd),
                )

                controller.onControllerStateChanged = {
                    virtualController!!.consumeControllerState(controller.controllerState)
                }

                virtualController!!.onControllerStateChanged = {
                    /**
                     * todo: provide virtual controller state
                     *
                     * e.g LED, Rumble, etc
                     */
                }

                startInputEventsLoop(
                    controllers = controllers,
                )
            }
        }
    }

    private suspend fun disconnectController() {
        mutex.withLock {
            bridgeScope.coroutineContext.cancelChildren()
            activeController?.stop()
            activeController = null
            destroyVirtualController()
        }
    }

    private fun destroyVirtualController() {
        virtualController?.destroy()
        virtualController = null
    }

    private suspend fun getActiveController(): PhysicalController2? {
        return mutex.withLock { activeController }
    }

    context(MemScope)
    private suspend fun startInputEventsLoop(
        controllers: Map<Controller, List<pollfd>>,
    ) {
        val size = controllers.values.sumOf { pollFds -> pollFds.size }
        val pollFds = controllers.values.flatten()
        val nativeFds = allocArray<pollfd>(size)

        pollFds.forEachIndexed { index, pollfd ->
            nativeFds[index].fd = pollfd.fd
            nativeFds[index].events = pollfd.events
            nativeFds[index].revents = pollfd.revents
        }

        while (true) {
            currentCoroutineContext().ensureActive()

            val ret = poll(nativeFds, size.toULong(), 1000)

            if (ret == -1) throw IllegalStateException("Can not start polling")

            pollFds.forEachIndexed { index, pollfd ->
                pollfd.revents = nativeFds[index].revents
                pollfd.events = nativeFds[index].events
            }

            controllers.keys.forEach { controller ->
                controller.readEvents()
            }
        }
    }
}