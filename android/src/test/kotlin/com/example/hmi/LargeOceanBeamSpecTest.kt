package com.example.hmi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeOceanBeamSpecTest {

    @Test
    fun largeOceanUsesTheMdSourceGeometry() {
        assertEquals(9, LargeOceanBeamSpec.borderBlobs.size)
        assertEquals(9, LargeOceanBeamSpec.innerBlobs.size)
        assertEquals(1960L, LargeOceanBeamSpec.durationMs)
        assertEquals(9f, LargeOceanBeamSpec.innerShadowBlurPx, 0f)
        assertEquals(8f, LargeOceanBeamSpec.bloomBlurPx, 0f)
        assertEquals(70f, LargeOceanBeamSpec.borderBlobs.first().radiusX, 0f)
        assertEquals(40f, LargeOceanBeamSpec.borderBlobs.first().radiusY, 0f)
    }

    @Test
    fun whiteThemeKeepsTheInnerBloomVisibleOnLight() {
        assertEquals(0.26f, LargeOceanBeamSpec.strokeOpacity, 0f)
        assertEquals(0.42f, LargeOceanBeamSpec.innerOpacity, 0f)
        assertEquals(0.24f, LargeOceanBeamSpec.bloomOpacity, 0f)
        assertTrue(LargeOceanBeamSpec.innerOpacity > LargeOceanBeamSpec.bloomOpacity)
    }

    @Test
    fun whiteThemeUsesASolidSurfaceAndReadableForeground() {
        assertEquals(0xFFFFFFFF.toInt(), LargeOceanBeamSpec.baseColor)
        assertEquals(0xFF1D1D1D.toInt(), LargeOceanBeamSpec.labelColor)
        assertEquals(0.26f, LargeOceanBeamSpec.strokeOpacity, 0f)
        assertEquals(0.42f, LargeOceanBeamSpec.innerOpacity, 0f)
        assertEquals(0.24f, LargeOceanBeamSpec.bloomOpacity, 0f)
        assertEquals(0.16f, LargeOceanBeamSpec.innerShadowAlpha, 0f)
        assertEquals(0xFF000000.toInt(), LargeOceanBeamSpec.innerShadowColor)
        assertEquals(0xFF3228B4.toInt(), LargeOceanBeamSpec.bloomColor)
        assertEquals(13, LargeOceanBeamSpec.darkBloomStops.size)
    }

    @Test
    fun whiteModeUsesBlueHueRangeAndFastShiftPeriod() {
        assertEquals(50f, LargeOceanBeamSpec.hueRangeDegrees, 0f)
        assertEquals(-25f, LargeOceanBeamSpec.hueStartDegrees, 0f)
        assertEquals(25f, LargeOceanBeamSpec.hueEndDegrees, 0f)
        assertEquals(2_000L, LargeOceanBeamSpec.hueShiftPeriodMs)
        assertEquals(8f, LargeOceanBeamSpec.saturation, 0f)
        assertEquals(12f, LargeOceanBeamSpec.innerSaturation, 0f)
        assertEquals(28f, LargeOceanBeamSpec.innerEdgeMaskPx, 0f)
    }

    @Test
    fun whiteInnerPaletteMatchesTheOriginalOceanPalette() {
        assertEquals(0xFF6450DC.toInt(), LargeOceanBeamSpec.borderBlobs.first().color)
        assertTrue(LargeOceanBeamSpec.innerBlobs.indices.all { index ->
            LargeOceanBeamSpec.innerBlobs[index].color ==
                LargeOceanBeamSpec.borderBlobs[index].color
        })
    }

    @Test
    fun sourceWhiteBaseIsPureColorAndBeamWindowUsesPercentOfTurn() {
        assertTrue(LargeOceanBeamSpec.baseColors.all { it == LargeOceanBeamSpec.baseColor })
        assertEquals(0.20f, LargeOceanBeamSpec.beamMaskStops[1].position, 0f)
        assertEquals(0.98f, LargeOceanBeamSpec.beamMaskStops[8].position, 0f)
    }

    @Test
    fun whiteInnerGradientUsesOnlyATightEdgeFeather() {
        assertEquals(2f, LargeOceanBeamSpec.innerEdgeFeatherPx, 0f)
        assertEquals(0.72f, LargeOceanBeamSpec.innerBlobs.first().radiusX / LargeOceanBeamSpec.borderBlobs.first().radiusX, 0f)
    }

    @Test
    fun largeMaskHasTheWiderSourceBeamWindow() {
        assertEquals(10, LargeOceanBeamSpec.beamMaskStops.size)
        assertEquals(0f, LargeOceanBeamSpec.beamMaskStops.first().position, 0f)
        assertEquals(1f, LargeOceanBeamSpec.beamMaskStops[4].alpha, 0f)
        assertEquals(48f / 100f, LargeOceanBeamSpec.beamMaskStops[4].position, 0f)
        assertEquals(84f / 100f, LargeOceanBeamSpec.beamMaskStops[5].position, 0f)
        assertEquals(0f, LargeOceanBeamSpec.beamMaskStops.last().alpha, 0f)
    }
}
