package controller.physical.detector

import controller.physical.common.PhysicalController
import kotlinx.coroutines.flow.Flow

class CompositeDeviceDetector(
    private val hidrawDetector: ControllerDetector,
    private val standardDetector: ControllerDetector
) : ControllerDetector {

    override fun detectControllers(): List<PhysicalController> {
        val hidrawControllers = hidrawDetector.detectControllers()
        val standardControllers = standardDetector.detectControllers()

        val filteredStandardControllers = standardControllers.filter { standardController ->
            hidrawControllers.none { hidrawController ->
                hidrawController.hwInfo.ids == standardController.hwInfo.ids
            }
        }

        return hidrawControllers + filteredStandardControllers
    }

    /**
     * I don't care about hot attaching/detaching hidraw devices so far
     */
    override fun controllerEventsFlow(): Flow<ControllerDetector.ControllerEvent> {
        return standardDetector.controllerEventsFlow()
    }
}