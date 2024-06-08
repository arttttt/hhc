package events

interface MutableAxisEvent : InputEvent {
    var axis: Axis
    var value: Double
}

interface AxisEvent : InputEvent {
    val axis: Axis
    val value: Double
}