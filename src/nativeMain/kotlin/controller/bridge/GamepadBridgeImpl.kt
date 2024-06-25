package controller.bridge

import controller.physical.common.PhysicalController
import controller.physical.detector.ControllerDetector
import controller.virtual.VirtualControllerFactory
import controller.virtual.common.VirtualController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GamepadBridgeImpl(
    private val controllerDetector: ControllerDetector,
    private val virtualControllerFactory: VirtualControllerFactory
) : GamepadBridge {
    private val detectionContext = newFixedThreadPoolContext(2, "ControllerDetectionContext")
    private val bridgeContext = newSingleThreadContext("ControllerBridgeContext")

    private val detectionScope = CoroutineScope(SupervisorJob() + detectionContext)
    private val bridgeScope = CoroutineScope(SupervisorJob() + bridgeContext)

    private var activeController: PhysicalController? = null
    private var virtualController: VirtualController? = null

    private val mutex = Mutex()

    override fun start() {
        detectionScope.launch {
            controllerDetector
                .detectControllers()
                .firstOrNull()
                ?.let { controller ->
                    connectController(controller)
                }

            controllerDetector
                .controllerEventsFlow()
                .filter { event ->
                    when (event) {
                        is ControllerDetector.ControllerEvent.Attached -> getActiveController() == null
                        is ControllerDetector.ControllerEvent.Detached -> getActiveController()?.path == event.path
                    }
                }
                .collect { event ->
                    when (event) {
                        is ControllerDetector.ControllerEvent.Attached -> connectController(event.controller)
                        is ControllerDetector.ControllerEvent.Detached -> disconnectController()
                    }
                }
        }
    }

    override fun stop() {
        runBlocking {
            disconnectController()
        }
    }

    override fun shutdown() {
        stop()
        detectionScope.cancel()
        bridgeScope.cancel()
        detectionContext.close()
        bridgeContext.close()
    }

    private suspend fun connectController(controller: PhysicalController) {
        mutex.withLock {
            activeController = controller
        }

        controller.start()
        ensureVirtualControllerExists()

        bridgeScope.launch {
            controller.states.collect { state ->
                virtualController?.consumeControllerState(state)
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

    private fun ensureVirtualControllerExists() {
        if (virtualController == null) {
            virtualController = virtualControllerFactory.create()
            virtualController?.create()
        }
    }

    private fun destroyVirtualController() {
        virtualController?.destroy()
        virtualController = null
    }

    private suspend fun getActiveController(): PhysicalController? {
        return mutex.withLock { activeController }
    }
}