package controller.common.rumble

interface RumbleState {

    /**
     * usually is on the right side
     */
    val weakRumble: Double

    /**
     * usually is on the left side
     */
    val strongRumble: Double

    fun isEmpty(): Boolean {
        return strongRumble == 0.0 && weakRumble == 0.0
    }
}