package events

sealed interface InputEvent {
    val timestamp: Long
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