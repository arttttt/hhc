package events

import utils.ArrayBlockingStack

object InputEventPool {

    private const val POOL_SIZE = 512

    private val axisEventsPool = ArrayBlockingStack<AxisEventImpl>(POOL_SIZE)
    private val buttonEventsPool = ArrayBlockingStack<ButtonEventImpl>(POOL_SIZE)
    private val gyroEventsPool = ArrayBlockingStack<GyroEventImpl>(POOL_SIZE)
    private val touchpadEventsPool = ArrayBlockingStack<TouchpadEventImpl>(POOL_SIZE)
    private val vibrationEventsPool = ArrayBlockingStack<VibrationEventImpl>(POOL_SIZE)

    init {
        repeat(POOL_SIZE) {
            buttonEventsPool.add(
                ButtonEventImpl(
                    timestamp = 0L,
                    button = Button.A,
                    pressed = false,
                )
            )

            axisEventsPool.add(
                AxisEventImpl(
                    timestamp = 0,
                    axis = Axis.LT,
                    value = 0.0,
                )
            )

            gyroEventsPool.add(
                GyroEventImpl(
                    timestamp = 0,
                    x = 0.0,
                    y = 0.0,
                    z = 0.0,
                )
            )

            touchpadEventsPool.add(
                TouchpadEventImpl(
                    timestamp = 0,
                    touchId = 0,
                    x = 0.0,
                    y = 0.0,
                    pressed = false,
                )
            )

            vibrationEventsPool.add(
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
        val event = requireNotNull(buttonEventsPool.pop())

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
        val event = requireNotNull(axisEventsPool.pop())

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
        val event = requireNotNull(gyroEventsPool.pop())

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
        val event = requireNotNull(touchpadEventsPool.pop())

        event.timestamp = timestamp
        event.touchId = touchId
        event.x = x
        event.y = y
        event.pressed = pressed

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
        val event = requireNotNull(vibrationEventsPool.pop())

        event.timestamp = timestamp
        event.leftMotorSpeed = leftMotorSpeed
        event.rightMotorSpeed = rightMotorSpeed

        return event
    }

    fun release(event: InputEvent) {
        when (event) {
            is ButtonEventImpl -> buttonEventsPool.add(event)
            is AxisEventImpl -> axisEventsPool.add(event)
            is GyroEventImpl -> gyroEventsPool.add(event)
            is TouchpadEventImpl -> touchpadEventsPool.add(event)
            is VibrationEventImpl -> vibrationEventsPool.add(event)
            else -> {}
        }
    }
}

private data class ButtonEventImpl(
    override var timestamp: Long,
    override var button: Button,
    override var pressed: Boolean,
) : MutableButtonEvent, ButtonEvent

private data class AxisEventImpl(
    override var timestamp: Long,
    override var axis: Axis,
    override var value: Double,
) : MutableAxisEvent, AxisEvent

private data class GyroEventImpl(
    override var timestamp: Long,
    override var x: Double,
    override var y: Double,
    override var z: Double,
) : MutableGyroEvent, GyroEvent

private data class TouchpadEventImpl(
    override var timestamp: Long,
    override var touchId: Int,
    override var x: Double,
    override var y: Double,
    override var pressed: Boolean,
) : MutableTouchpadEvent, TouchpadEvent

private data class VibrationEventImpl(
    override var timestamp: Long,
    override var leftMotorSpeed: UByte,
    override var rightMotorSpeed: UByte,
) : MutableVibrationEvent, VibrationEvent