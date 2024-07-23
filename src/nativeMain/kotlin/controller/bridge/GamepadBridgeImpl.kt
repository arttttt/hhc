package controller.bridge

import controller.physical2.common.PhysicalController2
import controller.physical2.detector.ControllerDetector2
import controller.virtual.VirtualControllerFactory
import controller.virtual.common.VirtualController
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    override fun start() {
        detectionScope.launch {
            controllerDetector
                .detectControllers()
                .firstOrNull()
                ?.let { controller ->
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
        stop()
        detectionScope.cancel()
        bridgeScope.cancel()
        detectionContext.close()
        bridgeContext.close()
    }

    private suspend fun connectController(
        controller: PhysicalController2,
    ) {
        mutex.withLock {
            activeController = controller
        }

        controller.start()
        ensureVirtualControllerExists()

        bridgeScope.launch {
            controller.states.collect { state ->
                virtualController!!.consumeControllerState(state)
            }
        }

        bridgeScope.launch {
            virtualController!!.outputStates.collect { outputState ->
                controller.consumeControllerState(outputState)
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

    private suspend fun getActiveController(): PhysicalController2? {
        return mutex.withLock { activeController }
    }
}