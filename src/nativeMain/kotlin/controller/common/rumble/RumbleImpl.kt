package controller.common.rumble

data class RumbleImpl(
    override var weakRumble: Double,
    override var strongRumble: Double,
) : Rumble