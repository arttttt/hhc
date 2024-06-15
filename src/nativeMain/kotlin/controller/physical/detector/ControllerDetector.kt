package controller.physical.detector

import controller.physical.common.PhysicalController
import kotlinx.coroutines.flow.Flow

interface ControllerDetector {

    sealed interface ControllerEvent {

        data class Attached(
            val controller: PhysicalController,
        ) : ControllerEvent

        data class Detached(
            val path: String,
        ) : ControllerEvent
    }

    fun detectControllers(): List<PhysicalController>

    fun controllerEventsFlow(): Flow<ControllerEvent>
}