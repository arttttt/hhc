import controller.physical.common.PhysicalController
import controller.physical.detector.ControllerDetector
import controller.virtual.common.VirtualController
import controller.virtual.dualsense.Dualsense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newSingleThreadContext

class GamepadBridge(
    private val controllerDetector: ControllerDetector
) {

    private val controllerDetectionContext = newSingleThreadContext("ControllerDetectionContext")

    private val scope = CoroutineScope(newSingleThreadContext("GamepadBridgeContext") + SupervisorJob())

    private var physicalController: PhysicalController? = null
    private var virtualController: VirtualController = Dualsense()

    fun start() {
        controllerDetector
            .controllerEventsFlow()
            .flowOn(controllerDetectionContext)
            .onEach(::handleControllerEvent)
            .launchIn(scope)

        virtualController.create()
    }

    fun stop() {
        physicalController?.stop()
        virtualController.destroy()
        scope.coroutineContext.cancel()
    }

    private fun handleControllerEvent(event: ControllerDetector.ControllerEvent) {
        when (event) {
            is ControllerDetector.ControllerEvent.Attached -> {
                physicalController = event.controller
                physicalController!!.start()

                connectControllers()
            }
            is ControllerDetector.ControllerEvent.Detached -> {
                physicalController?.stop()
                physicalController = null
            }
        }
    }

    private fun connectControllers() {
        physicalController!!
            .events
            .onEach(virtualController::consumeInputEvent)
            .launchIn(scope)

        virtualController
            .events
            .onEach(physicalController!!::consumeInputEvent)
            .launchIn(scope)

    }
}