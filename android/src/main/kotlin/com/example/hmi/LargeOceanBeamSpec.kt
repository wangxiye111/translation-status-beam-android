package com.example.hmi

/**
 * Values copied from the upstream BorderBeam rotate/md(often shown as
 * Large)/ocean specification, using a white HMI presentation.
 *
 * Source: packages/border-beam/spec/beam-spec.json in
 * https://github.com/Jakubantalik/Libraries
 */
object LargeOceanBeamSpec {

    const val durationMs: Long = 1960L
    // Blue-only motion for the white HMI presentation.
    const val hueRangeDegrees: Float = 50f
    const val hueStartDegrees: Float = -25f
    const val hueEndDegrees: Float = 25f
    const val hueShiftPeriodMs: Long = 2_000L
    const val saturation: Float = 8f
    const val innerSaturation: Float = 12f
    // White HMI adaptation: keep the inner glow visually connected to the ring.
    const val innerEdgeFeatherPx: Float = 2f
    const val innerEdgeMaskPx: Float = 28f
    const val innerShadowBlurPx: Float = 9f
    const val bloomBlurPx: Float = 8f

    // White HMI presentation: the source beam layers remain Ocean-colored.
    const val baseColor: Int = 0xFFFFFFFF.toInt()
    const val labelColor: Int = 0xFF1D1D1D.toInt()
    const val strokeOpacity: Float = 0.26f
    const val strokeWidthPx: Float = 4f
    const val innerOpacity: Float = 0.42f
    const val bloomOpacity: Float = 0.24f
    const val innerShadowAlpha: Float = 0.16f
    const val innerShadowColor: Int = 0xFF000000.toInt()
    // Source Light Ocean spike/bloom color, kept blue-purple on the white base.
    const val bloomColor: Int = 0xFF3228B4.toInt()
    const val innerGradientSizeScale: Float = 0.72f

    // The dark source capsule is a solid surface; the beam is composited on top.
    val baseColors: IntArray = intArrayOf(baseColor, baseColor, baseColor)

    // Source palettes.border.ocean. The website labels this md preset Large.
    val borderBlobs: List<BeamBlob> = listOf(
        blob(100, 80, 220, 1f, 0.33f, -0.074f, 70f, 40f),
        blob(60, 120, 255, 1f, 0.12f, -0.05f, 60f, 35f),
        blob(80, 100, 200, 1f, 0.021f, 0.683f, 40f, 70f),
        blob(50, 140, 220, 1f, 0.021f, 0.683f, 20f, 35f),
        blob(120, 80, 255, 1f, 0.744f, 1.00f, 180f, 32f),
        blob(70, 130, 255, 1f, 0.55f, 1.00f, 85f, 26f),
        blob(140, 100, 240, 1f, 0.939f, 0.00f, 74f, 32f),
        blob(90, 110, 230, 1f, 1.00f, 0.271f, 26f, 42f),
        blob(130, 70, 255, 1f, 1.00f, 0.271f, 52f, 48f),
    )

    // White HMI adaptation of source innerGradientDerivation: smaller size to
    // keep the diffusion away from the label while preserving edge contact.
    val innerBlobs: List<BeamBlob> = borderBlobs.map { blob ->
        blob.copy(
            alpha = 0.45f,
            radiusX = blob.radiusX * innerGradientSizeScale,
            radiusY = blob.radiusY * innerGradientSizeScale,
        )
    }

    // White HMI adaptation of source rotate.beamMaskStops: longer visible tail.
    val beamMaskStops: List<BeamStop> = listOf(
        BeamStop(0.00f, 0.00f),
        BeamStop(0.20f, 0.00f),
        BeamStop(0.28f, 0.10f),
        BeamStop(0.38f, 0.35f),
        BeamStop(0.48f, 1.00f),
        BeamStop(0.84f, 1.00f),
        BeamStop(0.92f, 0.35f),
        BeamStop(0.96f, 0.10f),
        BeamStop(0.98f, 0.00f),
        BeamStop(1.00f, 0.00f),
    )

    // Source rotate.bloomGradientStops.dark.
    val darkBloomStops: List<BeamStop> = listOf(
        BeamStop(0.00f, 0.00f),
        BeamStop(0.58f, 0.00f),
        BeamStop(0.62f, 0.02f),
        BeamStop(0.65f, 0.08f),
        BeamStop(0.67f, 0.20f),
        BeamStop(0.69f, 0.45f),
        BeamStop(0.70f, 0.85f),
        BeamStop(0.705f, 0.85f),
        BeamStop(0.715f, 0.45f),
        BeamStop(0.73f, 0.20f),
        BeamStop(0.75f, 0.08f),
        BeamStop(0.78f, 0.02f),
        BeamStop(0.82f, 0.00f),
    )

    private fun blob(
        red: Int,
        green: Int,
        blue: Int,
        alpha: Float,
        xPercent: Float,
        yPercent: Float,
        radiusX: Float,
        radiusY: Float,
    ): BeamBlob = BeamBlob(
        color = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue,
        alpha = alpha,
        xPercent = xPercent,
        yPercent = yPercent,
        radiusX = radiusX,
        radiusY = radiusY,
    )

}
