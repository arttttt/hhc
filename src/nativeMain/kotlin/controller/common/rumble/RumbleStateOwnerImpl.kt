package controller.common.rumble

import controller.common.normalization.NormalizationMode
import utils.normalize

class RumbleStateOwnerImpl(
    private val normalizationMode: NormalizationMode,
) : RumbleStateOwner {

    override val state = RumbleImpl(
        weakRumble = normalize(0, normalizationMode),
        strongRumble = normalize(0, normalizationMode),
    )

    override fun setWeakRumbleValue(rawValue: Int) {
        state.weakRumble = normalize(rawValue, normalizationMode)
    }

    override fun setStrongRumbleValue(rawValue: Int) {
        state.strongRumble = normalize(rawValue, normalizationMode)
    }
}