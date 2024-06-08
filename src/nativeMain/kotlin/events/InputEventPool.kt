package events

import ArrayBlockingQueue

object InputEventPool {

    private const val POOL_SIZE = 512

    private val axisEventsPool = ArrayBlockingQueue<AxisEventImpl>(POOL_SIZE)
    private val buttonEventsPool = ArrayBlockingQueue<ButtonEventImpl>(POOL_SIZE)
    private val gyroEventsPool = ArrayBlockingQueue<GyroEventImpl>(POOL_SIZE)
    private val touchpadEventsPool = ArrayBlockingQueue<TouchpadEventImpl>(POOL_SIZE)
    private val triggerEventsPool = ArrayBlockingQueue<TriggerEventImpl>(POOL_SIZE)
    private val vibrationEventsPool = ArrayBlockingQueue<VibrationEventImpl>(POOL_SIZE)

    init {
        repeat(POOL_SIZE) {
            buttonEventsPool.offer(
                ButtonEventImpl(
                    timestamp = 0L,
                    button = Button.A,
                    pressed = false,
                )
            )

            axisEventsPool.offer(
                AxisEventImpl(
                    timestamp = 0,
                    axis = Axis.L2_TRIGGER,
                    value = 0.0,
                )
            )

            gyroEventsPool.offer(
                GyroEventImpl(
                    timestamp = 0,
                    x = 0.0,
                    y = 0.0,
                    z = 0.0,
                )
            )

            touchpadEventsPool.offer(
                TouchpadEventImpl(
                    timestamp = 0,
                    touchId = 0,
                    x = 0.0,
                    y = 0.0,
                    pressed = false,
                )
            )

            triggerEventsPool.offer(
                TriggerEventImpl(
                    timestamp = 0,
                    trigger = Trigger.LEFT,
                    value = 0.0,
                )
            )

            vibrationEventsPool.offer(
                VibrationEventImpl(
                    timestamp = 0,
                    leftMotorSpeed = 0u,
                    rightMotorSpeed = 0u,
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

    fun obtainAxisEvent(
        timestamp: Long,
        axis: Axis,
        value: Double,
    ): AxisEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(axisEventsPool.poll())

        event.timestamp = timestamp
        event.axis = axis
        event.value = value

        return event
    }

    fun obtainGyroEvent(
        timestamp: Long,
        x: Double,
        y: Double,
        z: Double,
    ): GyroEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(gyroEventsPool.poll())

        event.timestamp = timestamp
        event.x = x
        event.y = y
        event.z = z

        return event
    }

    fun obtainTouchpadEvent(
        timestamp: Long,
        touchId: Int,
        x: Double,
        y: Double,
        pressed: Boolean,
    ): TouchpadEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(touchpadEventsPool.poll())

        event.timestamp = timestamp
        event.touchId = touchId
        event.x = x
        event.y = y
        event.pressed = pressed

        return event
    }

    fun obtainTriggerEvent(
        timestamp: Long,
        trigger: Trigger,
        value: Double,
    ): TriggerEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(triggerEventsPool.poll())

        event.timestamp = timestamp
        event.trigger = trigger
        event.value = value

        return event
    }

    fun obtainVibrationEvent(
        timestamp: Long,
        leftMotorSpeed: UByte,
        rightMotorSpeed: UByte
    ): VibrationEvent {
        /**
         * let's think that we always have events in a pool
         */
        val event = requireNotNull(vibrationEventsPool.poll())

        event.timestamp = timestamp
        event.leftMotorSpeed = leftMotorSpeed
        event.rightMotorSpeed = rightMotorSpeed

        return event
    }

    fun release(event: InputEvent) {
        when (event) {
            is ButtonEventImpl -> buttonEventsPool.offer(event)
            is AxisEventImpl -> axisEventsPool.offer(event)
            is GyroEventImpl -> gyroEventsPool.offer(event)
            is TouchpadEventImpl -> touchpadEventsPool.offer(event)
            is TriggerEventImpl -> triggerEventsPool.offer(event)
            is VibrationEventImpl -> vibrationEventsPool.offer(event)
            else -> {}
        }
    }
}

private class ButtonEventImpl(
    override var timestamp: Long,
    override var button: Button,
    override var pressed: Boolean,
) : MutableButtonEvent, ButtonEvent

private data class AxisEventImpl(
    override var timestamp: Long,
    override var axis: Axis,
    override var value: Double,
) : MutableAxisEvent, AxisEvent

private class GyroEventImpl(
    override var timestamp: Long,
    override var x: Double,
    override var y: Double,
    override var z: Double,
) : MutableGyroEvent, GyroEvent

private class TouchpadEventImpl(
    override var timestamp: Long,
    override var touchId: Int,
    override var x: Double,
    override var y: Double,
    override var pressed: Boolean,
) : MutableTouchpadEvent, TouchpadEvent

private class TriggerEventImpl(
    override var timestamp: Long,
    override var trigger: Trigger,
    override var value: Double,
) : MutableTriggerEvent, TriggerEvent

private class VibrationEventImpl(
    override var timestamp: Long,
    override var leftMotorSpeed: UByte,
    override var rightMotorSpeed: UByte,
) : MutableVibrationEvent, VibrationEvent