package controller.bridge

import controller.physical.common.PhysicalController
import controller.physical.detector.ControllerDetector
import controller.virtual.VirtualControllerFactory
import controller.virtual.common.VirtualController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter

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

    override fun start() {
        detectionScope.launch {
            controllerDetector
                .detectControllers()
                .firstOrNull()
                ?.let(::connectController)

            controllerDetector
                .controllerEventsFlow()
                .filter { event ->
                    when (event) {
                        is ControllerDetector.ControllerEvent.Attached -> activeController == null
                        is ControllerDetector.ControllerEvent.Detached -> activeController?.path == event.path
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
        bridgeScope.coroutineContext.cancelChildren()
        disconnectController()
        virtualController?.destroy()
        virtualController = null
    }

    override fun shutdown() {
        stop()
        detectionScope.cancel()
        bridgeScope.cancel()
        detectionContext.close()
        bridgeContext.close()
    }

    private fun connectController(controller: PhysicalController) {
        disconnectController()
        activeController = controller
        controller.start()
        ensureVirtualControllerExists()
        bridgeScope.launch {
            controller.states.collect { state ->
                virtualController?.consumeControllerState(state)
            }
        }
    }

    private fun disconnectController() {
        bridgeScope.coroutineContext.cancelChildren()
        activeController?.stop()
        activeController = null
        destroyVirtualController()
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
}