package events

interface MutableButtonEvent : InputEvent {
    var button: Button
    var pressed: Boolean
}

interface ButtonEvent : InputEvent {
    val button: Button
    val pressed: Boolean
}
