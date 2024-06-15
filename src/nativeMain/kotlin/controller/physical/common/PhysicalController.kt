package controller.physical.common

import events.InputEvent
import kotlinx.coroutines.flow.Flow

interface PhysicalController {
    val name: String
    val events: Flow<InputEvent>

    fun start()
    fun stop()

    fun consumeInputEvent(event: InputEvent)
}