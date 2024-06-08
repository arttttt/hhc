package events

interface MutableButtonEvent : InputEvent {
    override var timestamp: Long
    var button: Button
    var pressed: Boolean
}

interface ButtonEvent : InputEvent {
    override val timestamp: Long
    val button: Button
    val pressed: Boolean
}
