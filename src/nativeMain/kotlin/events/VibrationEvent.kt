package events

interface MutableVibrationEvent : InputEvent {
    var leftMotorSpeed: UByte
    var rightMotorSpeed: UByte
}

interface VibrationEvent : InputEvent {
    val leftMotorSpeed: UByte
    val rightMotorSpeed: UByte
}