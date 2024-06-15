package controller.virtual.common

import events.InputEvent
import kotlinx.coroutines.flow.Flow

interface VirtualController {

    val events: Flow<InputEvent>

    fun create()
    fun destroy()

    fun consumeInputEvent(event: InputEvent)
}