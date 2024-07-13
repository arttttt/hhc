package controller.common.rumble

interface RumbleStateOwner {

    val state: Rumble

    fun setWeakRumbleValue(rawValue: Int)
    fun setStrongRumbleValue(rawValue: Int)
}