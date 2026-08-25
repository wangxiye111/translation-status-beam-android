package com.example.hmi

/**
 * Sanitized, UI-independent state for the translation beam.
 *
 * Keeping this small value object separate from the Android View makes the
 * public behavior testable without a device or a rendering surface.
 */
class TranslationBeamState(
    val active: Boolean = true,
    strength: Float = DEFAULT_STRENGTH,
    durationMs: Long = DEFAULT_DURATION_MS,
    val beamEnabled: Boolean = true,
) {

    val strength: Float = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)
    val durationMs: Long = durationMs.takeIf { it > 0L } ?: DEFAULT_DURATION_MS

    val shouldAnimate: Boolean
        get() = active && beamEnabled

    companion object {
        const val DEFAULT_DURATION_MS: Long = 1960L
        const val DEFAULT_STRENGTH: Float = 1f
        const val MIN_STRENGTH: Float = 0f
        const val MAX_STRENGTH: Float = 1f
    }
}
