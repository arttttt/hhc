package controller.common.rumble

interface Rumble {

    /**
     * usually is on the right side
     * normalized value
     */
    val weakRumble: Double

    /**
     * usually is on the left side
     * normalized value
     */
    val strongRumble: Double

    fun isEmpty(): Boolean {
        return strongRumble == 0.0 && weakRumble == 0.0
    }
}