package controller.common.rumble

interface RumbleState {

    val strong: Float
    val weak: Float

    fun isEmpty(): Boolean {
        return strong == 0f && weak == 0f
    }
}