package com.example.hmi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationBeamStateTest {

    @Test
    fun strengthIsClampedToTheSupportedRange() {
        assertEquals(0f, TranslationBeamState(strength = -0.2f).strength, 0f)
        assertEquals(1f, TranslationBeamState(strength = 1.4f).strength, 0f)
    }

    @Test
    fun nonPositiveDurationUsesTheReferenceCycle() {
        assertEquals(
            TranslationBeamState.DEFAULT_DURATION_MS,
            TranslationBeamState(durationMs = 0L).durationMs
        )
        assertEquals(
            TranslationBeamState.DEFAULT_DURATION_MS,
            TranslationBeamState(durationMs = -100L).durationMs
        )
    }

    @Test
    fun inactiveStateDoesNotRunTheAnimator() {
        assertFalse(TranslationBeamState(active = false).shouldAnimate)
        assertTrue(TranslationBeamState(active = true).shouldAnimate)
        assertFalse(TranslationBeamState(active = true, beamEnabled = false).shouldAnimate)
    }
}
