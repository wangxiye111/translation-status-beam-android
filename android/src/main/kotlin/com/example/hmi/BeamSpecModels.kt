package com.example.hmi

data class BeamBlob(
    val color: Int,
    val alpha: Float,
    val xPercent: Float,
    val yPercent: Float,
    val radiusX: Float,
    val radiusY: Float,
)

data class BeamStop(
    val position: Float,
    val alpha: Float,
)
