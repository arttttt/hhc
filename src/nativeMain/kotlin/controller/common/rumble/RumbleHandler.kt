package controller.common.rumble

import input.*
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.posix.write
import kotlin.coroutines.resume
import kotlin.math.roundToInt

class RumbleHandler {

    companion object {

        private const val NO_RUMBLE = Int.MIN_VALUE
    }

    private val rumbleEffectId = atomic(NO_RUMBLE)

    suspend fun rumble(
        fd: Int,
        state: RumbleState,
    ) {
        if (rumbleEffectId.value != NO_RUMBLE) {
            clearRumbleEffect(fd)
        }

        suspendCancellableCoroutine { continuation ->
            memScoped {

                val effect = alloc<ff_effect>().apply {
                    type = FF_RUMBLE.toUShort()
                    id = -1
                    u.rumble.strong_magnitude = (state.strongRumble * UShort.MAX_VALUE.toInt()).roundToInt().toUShort()
                    u.rumble.weak_magnitude = (state.weakRumble * UShort.MAX_VALUE.toInt()).roundToInt().toUShort()
                }

                rumbleEffectId.value = ioctl(fd, EVIOCSFF, effect)
                if (rumbleEffectId.value == -1) {
                    println("Failed to update force feedback effect")

                    continuation.cancel()
                }

                val play = alloc<input_event>().apply {
                    type = EV_FF.toUShort()
                    code = rumbleEffectId.value.toUShort()
                    value = 1
                }

                if (write(fd, play.ptr, sizeOf<input_event>().toULong()) == -1L) {
                    println("Failed to play force feedback effect")
                }
            }

            continuation.resume(Unit)
        }
    }

    suspend fun clearRumbleEffect(fd: Int) {
        if (rumbleEffectId.value == NO_RUMBLE) return

        suspendCancellableCoroutine { continuation ->
            ioctl(fd, EVIOCRMFF, rumbleEffectId.value)

            rumbleEffectId.value = NO_RUMBLE

            continuation.resume(Unit)
        }
    }
}