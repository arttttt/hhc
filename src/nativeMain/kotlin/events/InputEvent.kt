package events

sealed interface InputEvent {
    val timestamp: Long
}

enum class Button {
    A,
    B,
    X,
    Y,
    LB,
    RB,
    LS,
    RS,
    MODE,
    START,
    SELECT,
}

enum class Axis {
    LX,
    LY,
    RX,
    RY,
    LT,
    RT,
    HAT0X,
    HAT0Y,
}