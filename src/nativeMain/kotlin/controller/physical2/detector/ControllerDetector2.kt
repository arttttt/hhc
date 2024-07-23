package controller.physical2.detector

import controller.physical.common.PhysicalController
import controller.physical2.common.PhysicalController2
import kotlinx.coroutines.flow.Flow

interface ControllerDetector2 {

    sealed interface ControllerEvent {

        data class Attached(
            val controller: PhysicalController,
        ) : ControllerEvent

        data class Detached(
            val path: String,
        ) : ControllerEvent
    }

    fun detectControllers(): List<PhysicalController2>

    fun controllerEventsFlow(): Flow<ControllerEvent>
}