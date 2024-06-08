package events

sealed interface InputEvent {
    val timestamp: Long

    data class AxisEvent(
        override val timestamp: Long,
        val axis: Axis,
        val value: Double,
    ) : InputEvent

    data class GyroEvent(
        override val timestamp: Long,
        val x: Double,
        val y: Double,
        val z: Double,
    ) : InputEvent

    data class TouchpadEvent(
        override val timestamp: Long,
        val touchId: Int,
        val x: Double,
        val y: Double,
        val pressed: Boolean,
    ) : InputEvent

    data class TriggerEvent(
        override val timestamp: Long,
        val trigger: Trigger,
        val value: Double,
    ) : InputEvent

    data class VibrationEvent(
        override val timestamp: Long,
        val leftMotorSpeed: UByte,
        val rightMotorSpeed: UByte,
    ) : InputEvent
}

enum class Button {
    A,
    B,
    X,
    Y,
    L1,
    R1,
    L2,
    R2,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    START,
    SELECT,
    HOME,
    SHARE,
    OPTIONS,
}

enum class Axis {
    LX,
    LY,
    RX,
    RY,
    L2_TRIGGER,
    R2_TRIGGER,
}

enum class Trigger {
    LEFT,
    RIGHT
}