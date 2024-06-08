package events

interface MutableTouchpadEvent : InputEvent {
    var touchId: Int
    var x: Double
    var y: Double
    var pressed: Boolean
}

interface TouchpadEvent : InputEvent {
    val touchId: Int
    val x: Double
    val y: Double
    val pressed: Boolean
}