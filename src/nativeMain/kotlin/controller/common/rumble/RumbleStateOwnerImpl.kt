package controller.common.rumble

import controller.common.normalization.NormalizationInfo

class RumbleStateOwnerImpl(
    private val normalizationInfo: NormalizationInfo,
) : RumbleStateOwner {

    override val state = RumbleImpl(
        weakRumble = normalizationInfo.normalize(0),
        strongRumble = normalizationInfo.normalize(0),
    )

    override fun setWeakRumbleValue(rawValue: Int) {
        state.weakRumble = normalizationInfo.normalize(rawValue)
    }

    override fun setStrongRumbleValue(rawValue: Int) {
        state.strongRumble = normalizationInfo.normalize(rawValue)
    }
}