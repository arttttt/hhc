package events

interface MutableGyroEvent : InputEvent {
    var x: Double
    var y: Double
    var z: Double
}

interface GyroEvent : InputEvent {
    val x: Double
    val y: Double
    val z: Double
}