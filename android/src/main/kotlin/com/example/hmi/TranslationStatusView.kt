package com.example.hmi

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Native XML host for the source border-beam rotate/md(Large)/ocean effect.
 *
 * The source web/Skia implementation uses three layers: inner glow, stroke
 * ring, and bloom. This View preserves that order and consumes the source
 * Large Ocean blob/mask data from [LargeOceanBeamSpec].
 */
class TranslationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class BeamVariant {
        OCEAN,
        MONO,
    }

    private var beamState = TranslationBeamState(
        durationMs = LargeOceanBeamSpec.durationMs,
        strength = 1f,
    )
    private var beamOpacity = 1f
    private var angleDegrees = 0f
    private var currentVariant = BeamVariant.OCEAN

    private var angleAnimator: ValueAnimator? = null
    private var opacityAnimator: ValueAnimator? = null

    private val capsuleBounds = RectF()
    private val innerBounds = RectF()
    private val ringPath = Path()
    private val capsulePath = Path()
    private var capsuleRadius = 0f
    private var baseShader: LinearGradient? = null
    private var beamMaskShader: SweepGradient? = null
    private var bloomShader: SweepGradient? = null
    private val shaderMatrix = Matrix()
    private val hueMatrix = ColorMatrix()
    private val saturationMatrix = ColorMatrix()
    private var hueFilter: ColorMatrixColorFilter? = null
    private var innerHueFilter: ColorMatrixColorFilter? = null

    private var innerBitmap: Bitmap? = null
    private var strokeBitmap: Bitmap? = null
    private var bloomBitmap: Bitmap? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = LargeOceanBeamSpec.strokeWidthPx
        color = Color.argb(
            (LargeOceanBeamSpec.innerShadowAlpha * 255f).toInt(),
            (LargeOceanBeamSpec.innerShadowColor shr 16) and 0xFF,
            (LargeOceanBeamSpec.innerShadowColor shr 8) and 0xFF,
            LargeOceanBeamSpec.innerShadowColor and 0xFF,
        )
        maskFilter = BlurMaskFilter(
            LargeOceanBeamSpec.innerShadowBlurPx,
            BlurMaskFilter.Blur.NORMAL,
        )
    }
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = LargeOceanBeamSpec.strokeWidthPx
        isDither = true
        maskFilter = BlurMaskFilter(
            LargeOceanBeamSpec.bloomBlurPx,
            BlurMaskFilter.Blur.NORMAL,
        )
    }

    init {
        // The source effect needs a small blur. This View is only 174×64 px,
        // so a software layer keeps behavior consistent on older head units.
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.TranslationStatusView,
            defStyleAttr,
            0,
        )
        try {
            beamState = TranslationBeamState(
                active = typedArray.getBoolean(
                    R.styleable.TranslationStatusView_beamActive,
                    true,
                ),
                strength = typedArray.getFloat(
                    R.styleable.TranslationStatusView_beamStrength,
                    1f,
                ),
                durationMs = typedArray.getInt(
                    R.styleable.TranslationStatusView_beamDurationMs,
                    LargeOceanBeamSpec.durationMs.toInt(),
                ).toLong(),
                beamEnabled = typedArray.getBoolean(
                    R.styleable.TranslationStatusView_beamEnabled,
                    true,
                ),
            )
            currentVariant = when (
                typedArray.getInt(
                    R.styleable.TranslationStatusView_beamVariant,
                    BEAM_VARIANT_OCEAN,
                )
            ) {
                BEAM_VARIANT_MONO -> BeamVariant.MONO
                else -> BeamVariant.OCEAN
            }
        } finally {
            typedArray.recycle()
        }
    }

    var active: Boolean
        get() = beamState.active
        set(value) {
            if (value == beamState.active) return
            updateState(active = value)
        }

    var strength: Float
        get() = beamState.strength
        set(value) {
            val next = stateWith(strength = value)
            if (next.strength == beamState.strength) return
            beamState = next
            invalidate()
        }

    var durationMs: Long
        get() = beamState.durationMs
        set(value) {
            val next = stateWith(durationMs = value)
            if (next.durationMs == beamState.durationMs) return
            beamState = next
            stopAngleAnimator()
            syncAngleAnimator()
        }

    var beamEnabled: Boolean
        get() = beamState.beamEnabled
        set(value) {
            if (value == beamState.beamEnabled) return
            updateState(beamEnabled = value)
        }

    var beamVariant: BeamVariant
        get() = currentVariant
        set(value) {
            if (value == currentVariant) return
            currentVariant = value
            rebuildShaders()
            invalidate()
        }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return

        // Keep the outer capsule at the requested 174×64 bounds. The ring
        // itself is carved inward by the full 4 px stroke width.
        val strokeInset = 0f
        capsuleBounds.set(
            strokeInset,
            strokeInset,
            width.toFloat() - strokeInset,
            height.toFloat() - strokeInset,
        )
        innerBounds.set(
            capsuleBounds.left + LargeOceanBeamSpec.strokeWidthPx,
            capsuleBounds.top + LargeOceanBeamSpec.strokeWidthPx,
            capsuleBounds.right - LargeOceanBeamSpec.strokeWidthPx,
            capsuleBounds.bottom - LargeOceanBeamSpec.strokeWidthPx,
        )
        capsuleRadius = (height / 2f).coerceAtLeast(0f)

        capsulePath.reset()
        capsulePath.addRoundRect(
            capsuleBounds,
            capsuleRadius,
            capsuleRadius,
            Path.Direction.CW,
        )

        ringPath.reset()
        ringPath.fillType = Path.FillType.EVEN_ODD
        ringPath.addRoundRect(
            capsuleBounds,
            capsuleRadius,
            capsuleRadius,
            Path.Direction.CW,
        )
        val innerRadius = (
            capsuleRadius - LargeOceanBeamSpec.strokeWidthPx
            ).coerceAtLeast(0f)
        ringPath.addRoundRect(
            innerBounds,
            innerRadius,
            innerRadius,
            Path.Direction.CW,
        )

        baseShader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            0f,
            BASE_COLORS,
            BASE_POSITIONS,
            Shader.TileMode.CLAMP,
        )
        backgroundPaint.shader = baseShader

        allocateBitmaps(width, height)
        rebuildShaders()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (capsuleBounds.isEmpty) return

        canvas.drawRoundRect(
            capsuleBounds,
            capsuleRadius,
            capsuleRadius,
            backgroundPaint,
        )

        if (beamOpacity <= 0f || !beamState.beamEnabled) return

        val angle = angleDegrees + SOURCE_SWEEP_START_OFFSET_DEGREES
        shaderMatrix.setRotate(angle, width / 2f, height / 2f)
        beamMaskShader?.setLocalMatrix(shaderMatrix)
        bloomShader?.setLocalMatrix(shaderMatrix)
        updateHueFilter()

        renderBlobLayer(innerBitmap, LargeOceanBeamSpec.innerBlobs, capsulePath)
        applyInnerEdgeFeather(innerBitmap)
        renderBlobLayer(strokeBitmap, LargeOceanBeamSpec.borderBlobs, ringPath)
        renderBloomLayer(bloomBitmap)

        drawLayer(canvas, innerBitmap, LargeOceanBeamSpec.innerOpacity, innerHueFilter)
        drawInnerShadow(canvas)
        drawLayer(canvas, strokeBitmap, LargeOceanBeamSpec.strokeOpacity)
        drawLayer(canvas, bloomBitmap, LargeOceanBeamSpec.bloomOpacity)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        syncAngleAnimator()
        if (beamState.shouldAnimate) animateOpacityTo(1f)
    }

    override fun onDetachedFromWindow() {
        stopAngleAnimator()
        opacityAnimator?.cancel()
        opacityAnimator = null
        recycleBitmaps()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this) syncAngleAnimator()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        syncAngleAnimator()
    }

    private fun stateWith(
        active: Boolean = beamState.active,
        strength: Float = beamState.strength,
        durationMs: Long = beamState.durationMs,
        beamEnabled: Boolean = beamState.beamEnabled,
    ): TranslationBeamState = TranslationBeamState(
        active = active,
        strength = strength,
        durationMs = durationMs,
        beamEnabled = beamEnabled,
    )

    private fun updateState(
        active: Boolean = beamState.active,
        beamEnabled: Boolean = beamState.beamEnabled,
    ) {
        beamState = stateWith(active = active, beamEnabled = beamEnabled)
        syncAngleAnimator()
        animateOpacityTo(if (beamState.shouldAnimate) 1f else 0f)
    }

    private fun syncAngleAnimator() {
        val canAnimate = isAttachedToWindow &&
            visibility == VISIBLE &&
            isShown &&
            beamState.shouldAnimate

        if (canAnimate) {
            if (angleAnimator == null) {
                angleAnimator = ValueAnimator.ofFloat(angleDegrees, angleDegrees + 360f).apply {
                    duration = beamState.durationMs
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    addUpdateListener { animator ->
                        angleDegrees = animator.animatedValue as Float
                        postInvalidateOnAnimation()
                    }
                    start()
                }
            }
        } else {
            stopAngleAnimator()
        }
    }

    private fun stopAngleAnimator() {
        angleAnimator?.cancel()
        angleAnimator = null
    }

    private fun animateOpacityTo(target: Float) {
        if (beamOpacity == target) {
            invalidate()
            return
        }

        opacityAnimator?.cancel()
        opacityAnimator = ValueAnimator.ofFloat(beamOpacity, target).apply {
            duration = FADE_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                beamOpacity = animator.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    private fun allocateBitmaps(width: Int, height: Int) {
        recycleBitmaps()
        innerBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        strokeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bloomBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun recycleBitmaps() {
        listOf(innerBitmap, strokeBitmap, bloomBitmap).forEach { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
        innerBitmap = null
        strokeBitmap = null
        bloomBitmap = null
    }

    private fun rebuildShaders() {
        if (width <= 0 || height <= 0) return

        val maskStops = LargeOceanBeamSpec.beamMaskStops
        beamMaskShader = SweepGradient(
            width / 2f,
            height / 2f,
            maskStops.map { Color.argb((it.alpha * 255f).toInt(), 255, 255, 255) }.toIntArray(),
            maskStops.map { it.position }.toFloatArray(),
        )

        val bloomStops = LargeOceanBeamSpec.darkBloomStops
        bloomShader = SweepGradient(
            width / 2f,
            height / 2f,
            bloomStops.map {
                Color.argb(
                    (it.alpha * 255f).toInt(),
                    (LargeOceanBeamSpec.bloomColor shr 16) and 0xFF,
                    (LargeOceanBeamSpec.bloomColor shr 8) and 0xFF,
                    LargeOceanBeamSpec.bloomColor and 0xFF,
                )
            }.toIntArray(),
            bloomStops.map { it.position }.toFloatArray(),
        )
    }

    private fun renderBlobLayer(
        bitmap: Bitmap?,
        blobs: List<BeamBlob>,
        clipPath: Path,
    ) {
        if (bitmap == null || bitmap.isRecycled) return

        val layerCanvas = Canvas(bitmap)
        layerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        layerCanvas.save()
        layerCanvas.clipPath(clipPath)

        // The upstream shader composites the first blob on top, so draw the
        // source list in reverse order with Canvas source-over semantics.
        for (index in blobs.lastIndex downTo 0) {
            drawBlob(layerCanvas, blobs[index])
        }
        layerCanvas.restore()
        applyBeamMask(layerCanvas)
    }

    private fun drawBlob(canvas: Canvas, blob: BeamBlob) {
        val centerX = blob.xPercent * width
        val centerY = blob.yPercent * height
        val blobColor = colorForVariant(blob.color)
        val transparent = blobColor and 0x00FFFFFF
        val gradient = RadialGradient(
            0f,
            0f,
            1f,
            intArrayOf(
                blobColorWithAlpha(blob, blobColor),
                transparent,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        blobPaint.shader = gradient
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(blob.radiusX, blob.radiusY)
        canvas.drawCircle(0f, 0f, 1f, blobPaint)
        canvas.restore()
        blobPaint.shader = null
    }

    private fun blobColorWithAlpha(blob: BeamBlob, color: Int): Int =
        (color and 0x00FFFFFF) or ((blob.alpha * 255f).toInt().coerceIn(0, 255) shl 24)

    private fun colorForVariant(color: Int): Int {
        if (currentVariant == BeamVariant.OCEAN) return color
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val luminance = (red * 0.299f + green * 0.587f + blue * 0.114f).toInt()
        return (0xFF shl 24) or (luminance shl 16) or (luminance shl 8) or luminance
    }

    private fun applyBeamMask(canvas: Canvas) {
        val shader = beamMaskShader ?: return
        maskPaint.shader = shader
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        maskPaint.xfermode = null
        maskPaint.shader = null
    }

    private fun renderBloomLayer(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return

        val layerCanvas = Canvas(bitmap)
        layerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        bloomPaint.shader = bloomShader
        layerCanvas.drawRoundRect(
            capsuleBounds,
            capsuleRadius,
            capsuleRadius,
            bloomPaint,
        )
        bloomPaint.shader = null
    }

    private fun applyInnerEdgeFeather(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val edgeFeather = LargeOceanBeamSpec.innerEdgeFeatherPx
        for (y in 0 until height) {
            for (x in 0 until width) {
                val distanceToEdge = minOf(
                    x,
                    y,
                    width - 1 - x,
                    height - 1 - y,
                ).toFloat()
                val edge = (distanceToEdge / edgeFeather).coerceIn(0f, 1f)
                val index = y * width + x
                val pixel = pixels[index]
                val alpha = (((pixel ushr 24) and 0xFF) * edge).toInt()
                pixels[index] = (pixel and 0x00FFFFFF) or (alpha shl 24)
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun drawLayer(
        canvas: Canvas,
        bitmap: Bitmap?,
        opacity: Float,
        colorFilter: ColorFilter? = hueFilter,
    ) {
        if (bitmap == null || bitmap.isRecycled) return
        layerPaint.alpha = (
            opacity * beamState.strength * beamOpacity * 255f
            ).toInt().coerceIn(0, 255)
        layerPaint.colorFilter = colorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, layerPaint)
        layerPaint.colorFilter = null
    }

    private fun updateHueFilter() {
        if (currentVariant != BeamVariant.OCEAN) {
            hueFilter = null
            innerHueFilter = null
            return
        }

        val elapsedMs = (angleDegrees / 360f) * beamState.durationMs
        val hueDegrees = (
            (elapsedMs % LargeOceanBeamSpec.hueShiftPeriodMs) /
                LargeOceanBeamSpec.hueShiftPeriodMs
            ) * LargeOceanBeamSpec.hueRangeDegrees + LargeOceanBeamSpec.hueStartDegrees
        val radians = Math.toRadians(hueDegrees.toDouble())
        val cosine = kotlin.math.cos(radians).toFloat()
        val sine = kotlin.math.sin(radians).toFloat()
        val lumRed = 0.213f
        val lumGreen = 0.715f
        val lumBlue = 0.072f
        hueMatrix.set(floatArrayOf(
            lumRed + cosine * (1f - lumRed) + sine * -lumRed,
            lumGreen + cosine * -lumGreen + sine * -lumGreen,
            lumBlue + cosine * -lumBlue + sine * (1f - lumBlue),
            0f,
            0f,
            lumRed + cosine * -lumRed + sine * 0.143f,
            lumGreen + cosine * (1f - lumGreen) + sine * 0.140f,
            lumBlue + cosine * -lumBlue + sine * -0.283f,
            0f,
            0f,
            lumRed + cosine * -lumRed + sine * -(1f - lumRed),
            lumGreen + cosine * -lumGreen + sine * lumGreen,
            lumBlue + cosine * (1f - lumBlue) + sine * lumBlue,
            0f,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f,
        ))
        hueFilter = createHueFilter(LargeOceanBeamSpec.saturation)
        innerHueFilter = createHueFilter(LargeOceanBeamSpec.innerSaturation)
    }

    private fun createHueFilter(saturation: Float): ColorMatrixColorFilter {
        val matrix = ColorMatrix(hueMatrix)
        saturationMatrix.setSaturation(saturation)
        matrix.postConcat(saturationMatrix)
        return ColorMatrixColorFilter(matrix)
    }

    private fun drawInnerShadow(canvas: Canvas) {
        innerShadowPaint.alpha = (
            LargeOceanBeamSpec.innerShadowAlpha * beamState.strength * beamOpacity * 255f
            ).toInt().coerceIn(0, 255)
        val innerRadius = (
            capsuleRadius - LargeOceanBeamSpec.strokeWidthPx
            ).coerceAtLeast(0f)
        canvas.drawRoundRect(innerBounds, innerRadius, innerRadius, innerShadowPaint)
    }

    companion object {
        private const val BEAM_VARIANT_OCEAN = 0
        private const val BEAM_VARIANT_MONO = 1
        private const val FADE_DURATION_MS = 180L
        private const val SOURCE_SWEEP_START_OFFSET_DEGREES = 90f

        private val BASE_COLORS = LargeOceanBeamSpec.baseColors
        private val BASE_POSITIONS = floatArrayOf(0f, 0.5f, 1f)
    }
}
