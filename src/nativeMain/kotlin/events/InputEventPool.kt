package events

import ArrayBlockingQueue

object InputEventPool {

    private const val POOL_SIZE = 512

    private val buttonEventsPool = ArrayBlockingQueue<ButtonEventImpl>(POOL_SIZE)

    init {
        repeat(POOL_SIZE) {
            buttonEventsPool.offer(
                ButtonEventImpl.create(
                    timestamp = 0L,
                    button = Button.A,
                    pressed = false,
                )
            )
        }
    }

    fun obtainButtonEvent(
        timestamp: Long,
        button: Button,
        pressed: Boolean,
    ): ButtonEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(buttonEventsPool.poll())

        event.timestamp = timestamp
        event.button = button
        event.pressed = pressed

        return event
    }

    fun release(event: InputEvent) {
        when (event) {
            is ButtonEventImpl -> buttonEventsPool.offer(event)
            else -> {}
        }
    }
}

class ButtonEventImpl private constructor(
    override var timestamp: Long,
    override var button: Button,
    override var pressed: Boolean,
) : MutableButtonEvent, ButtonEvent {

    companion object {

        context(InputEventPool)
        fun create(
            timestamp: Long,
            button: Button,
            pressed: Boolean,
        ): ButtonEventImpl {
            return ButtonEventImpl(timestamp, button, pressed)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (this::class != other::class) return false

        other as ButtonEventImpl

        if (timestamp != other.timestamp) return false
        if (button != other.button) return false
        if (pressed != other.pressed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + button.hashCode()
        result = 31 * result + pressed.hashCode()
        return result
    }
}