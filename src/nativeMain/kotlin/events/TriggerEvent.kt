package events

interface MutableTriggerEvent : InputEvent {
    var trigger: Trigger
    var value: Double
}

interface TriggerEvent : InputEvent {
    val trigger: Trigger
    val value: Double
}