package top.colter.skiko

import org.jetbrains.skia.*
import top.colter.skiko.data.GradientBlur
import top.colter.skiko.data.Shadow
import top.colter.skiko.layout.Layout
import java.io.File
import kotlin.jvm.JvmName
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin


/**
 * Emoji正则(可匹配组合emoji)
 */
public const val emojiCharacter: String =
    "(?:[\\uD83C\\uDF00-\\uD83D\\uDDFF]|[\\uD83E\\uDD00-\\uD83E\\uDDFF]|[\\uD83D\\uDE00-\\uD83D\\uDE4F]|[\\uD83D\\uDE80-\\uD83D\\uDEFF]|[\\u2600-\\u26FF]\\uFE0F?|[\\u2700-\\u27BF]\\uFE0F?|\\u24C2\\uFE0F?|[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{1,2}|[\\uD83C\\uDD70\\uD83C\\uDD71\\uD83C\\uDD7E\\uD83C\\uDD7F\\uD83C\\uDD8E\\uD83C\\uDD91-\\uD83C\\uDD9A]\\uFE0F?|[\\u0023\\u002A\\u0030-\\u0039]\\uFE0F?\\u20E3|[\\u2194-\\u2199\\u21A9-\\u21AA]\\uFE0F?|[\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50\\u2B55]\\uFE0F?|[\\u2934\\u2935]\\uFE0F?|[\\u3030\\u303D]\\uFE0F?|[\\u3297\\u3299]\\uFE0F?|[\\uD83C\\uDE01\\uD83C\\uDE02\\uD83C\\uDE1A\\uD83C\\uDE2F\\uD83C\\uDE32-\\uD83C\\uDE3A\\uD83C\\uDE50\\uD83C\\uDE51]\\uFE0F?|[\\u203C\\u2049]\\uFE0F?|[\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB-\\u25FE]\\uFE0F?|[\\u00A9\\u00AE]\\uFE0F?|[\\u2122\\u2139]\\uFE0F?|\\uD83C\\uDC04\\uFE0F?|\\uD83C\\uDCCF\\uFE0F?|[\\u231A\\u231B\\u2328\\u23CF\\u23E9-\\u23F3\\u23F8-\\u23FA]\\uFE0F?)(?:[\\uD83C\\uDFFB-\\uD83C\\uDFFF]|[\\uD83E\\uDDB0-\\uD83E\\uDDB3])?"
public val emojiRegex: Regex = "$emojiCharacter(?:\\u200D$emojiCharacter)*".toRegex()

private val defaultImageFilterMipmap = FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST)

internal fun imageAlphaPaint(alpha: Float): Paint? {
    require(alpha in 0f..1f) { "图片透明度需在 0..1 之间" }
    return if (alpha == 1f) null else Paint().setAlphaf(alpha)
}

internal data class GradientBlurCacheKey(
    val imageId: Int,
    val width: Int,
    val height: Int,
    val blur: GradientBlur,
    val dpFactor: Float,
)

internal fun gradientBlurCacheKey(image: Image, width: Int, height: Int, blur: GradientBlur): GradientBlurCacheKey =
    GradientBlurCacheKey(
        imageId = System.identityHashCode(image),
        width = width,
        height = height,
        blur = blur,
        dpFactor = Dp.factor,
    )

internal fun gradientBlurTargetSize(dstRect: RRect): Pair<Int, Int> =
    ceil(dstRect.width).toInt().coerceAtLeast(1) to ceil(dstRect.height).toInt().coerceAtLeast(1)

/**
 * 预生成图片渐变模糊结果。
 *
 * 生成时会先按目标尺寸做 cover 裁剪，再按 [blur] 做线性方向的多档近似模糊。
 */
public fun Image.gradientBlurred(width: Int, height: Int, blur: GradientBlur): Image {
    require(width > 0) { "渐变模糊输出宽度需要大于 0" }
    require(height > 0) { "渐变模糊输出高度需要大于 0" }

    val maxBlur = blur.maxBlur.px
    if (maxBlur <= 0f) return renderCoverImage(width, height, 0f)

    val levels = gradientBlurLevels(maxBlur, blur.steps)
    val layers = levels.map { sigma ->
        renderCoverImage(width, height, sigma)
    }
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    val fullSrc = Rect.makeWH(width.toFloat(), height.toFloat())
    val fullDst = Rect.makeWH(width.toFloat(), height.toFloat())
    val geometry = GradientBlurGeometry(width.toFloat(), height.toFloat(), blur.angle)
    val stripWidth = blur.stripWidth.px.coerceAtLeast(1f)
    val sampleCount = ceil(geometry.span / stripWidth).toInt().coerceIn(2, 2048)
    val plusPaint = Paint().apply {
        blendMode = BlendMode.PLUS
    }

    canvas.clear(Color.TRANSPARENT)

    layers.forEachIndexed { index, layer ->
        val mask = geometry.maskPaint(blur, maxBlur, index, layers.lastIndex, sampleCount)
        if (mask.maxAlpha <= 0f) return@forEachIndexed

        canvas.saveLayer(fullDst, plusPaint)
        canvas.drawImageRect(layer, fullSrc, fullDst, defaultImageFilterMipmap, null, false)
        canvas.drawRect(fullDst, mask.paint)
        canvas.restore()
    }

    return surface.makeImageSnapshot()
}

/**
 * 预生成图片渐变模糊结果并写入文件。
 */
public fun Image.writeGradientBlurred(file: File, width: Int, height: Int, blur: GradientBlur): Unit {
    file.parentFile?.mkdirs()
    file.writeBytes(gradientBlurred(width, height, blur).encodeToData()!!.bytes)
}

private fun gradientBlurLevels(maxBlur: Float, steps: Int): List<Float> =
    if (maxBlur <= 0f) {
        listOf(0f)
    } else {
        (0 until steps).map { index ->
            maxBlur * index / (steps - 1)
        }
    }

private fun Image.renderCoverImage(width: Int, height: Int, sigma: Float): Image {
    val surface = Surface.makeRasterN32Premul(width, height)
    val dst = Rect.makeWH(width.toFloat(), height.toFloat())
    val paint = if (sigma <= 0f) null else Paint().apply {
        imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP, null, null)
    }

    surface.canvas.clear(Color.TRANSPARENT)
    surface.canvas.drawImageRect(this, coverSourceRect(width.toFloat(), height.toFloat()), dst, defaultImageFilterMipmap, paint, false)
    return surface.makeImageSnapshot()
}

private fun Image.coverSourceRect(dstWidth: Float, dstHeight: Float): Rect {
    val ratio = width.toFloat() / height.toFloat()
    return if (dstWidth / ratio < dstHeight) {
        val imgW = dstWidth * height / dstHeight
        val offsetX = (width - imgW) / 2f
        Rect.makeXYWH(offsetX, 0f, imgW, height.toFloat())
    } else {
        val imgH = dstHeight * width / dstWidth
        val offsetY = (height - imgH) / 2f
        Rect.makeXYWH(0f, offsetY, width.toFloat(), imgH)
    }
}

private class GradientBlurGeometry(
    width: Float,
    height: Float,
    angle: Float,
) {
    private val angleArc = angle / 180f * PI.toFloat()
    private val dx = cos(angleArc)
    private val dy = sin(angleArc)
    private val minProjection: Float
    private val maxProjection: Float

    val span: Float

    init {
        val projections = floatArrayOf(
            project(0f, 0f),
            project(width, 0f),
            project(0f, height),
            project(width, height),
        )
        minProjection = projections.minOrNull() ?: 0f
        maxProjection = projections.maxOrNull() ?: 0f
        span = (maxProjection - minProjection).coerceAtLeast(1f)
    }

    fun maskPaint(
        blur: GradientBlur,
        maxBlur: Float,
        layerIndex: Int,
        lastLayerIndex: Int,
        sampleCount: Int,
    ): GradientBlurMask {
        var maxAlpha = 0f
        val positions = FloatArray(sampleCount + 1)
        val colors = Array(sampleCount + 1) { index ->
            val position = index / sampleCount.toFloat()
            val targetBlur = blur.blurAt(position).px.coerceIn(0f, maxBlur)
            val layerPosition = targetBlur / maxBlur * lastLayerIndex
            val alpha = (1f - abs(layerPosition - layerIndex)).coerceIn(0f, 1f)
            positions[index] = position
            maxAlpha = maxOf(maxAlpha, alpha)
            Color4f(1f, 1f, 1f, alpha)
        }

        val paint = Paint().apply {
            blendMode = BlendMode.DST_IN
            shader = Shader.makeLinearGradient(
                dx * minProjection,
                dy * minProjection,
                dx * maxProjection,
                dy * maxProjection,
                Gradient(
                    Gradient.Colors(
                        colors = colors,
                        positions = positions,
                        tileMode = FilterTileMode.CLAMP,
                        colorSpace = ColorSpace.sRGB,
                    )
                )
            )
        }
        return GradientBlurMask(paint, maxAlpha)
    }

    private fun project(x: Float, y: Float): Float = x * dx + y * dy
}

private data class GradientBlurMask(
    val paint: Paint,
    val maxAlpha: Float,
)


public fun List<Layout>.sumWidth(): Dp = sumOf { boxWidth }
public fun List<Layout>.sumHeight(): Dp = sumOf { boxHeight }
public fun List<Layout>.sumOf(block: Layout.() -> Dp): Dp {
    var x = 0.dp
    forEach { x += it.block() }
    return x
}

public fun List<Layout>.maxWidth(): Dp = maxOf { boxWidth }
public fun List<Layout>.maxHeight(): Dp = maxOf { boxHeight }
public fun List<Layout>.maxOf(block: Layout.() -> Dp): Dp {
    var x = 0.dp
    forEach { x = Dp.max(x, it.block()) }
    return x
}

public fun <T> List<T>.ifNotEmpty(block: List<T>.() -> Unit): Unit = if (isNotEmpty()) block() else { }
public fun <T> List<T>.ifNotEmptyForEach(block: (T) -> Unit): Unit = ifNotEmpty {
    forEach { block(it) }
}

@JvmName("sumFloatOf")
public fun <E> List<E>.sumOf(function: (E) -> Float): Float {
    var c = 0f
    forEach { c += function(it) }
    return c
}


/**
 * 绘制矩形阴影
 */
public fun Canvas.drawRectShadowAntiAlias(r: Rect, dx: Float, dy: Float, blur: Float, spread: Float, color: Int): Canvas {
    val insides = r.inflate((-1f).px)
    if (!insides.isEmpty) {
        save()
        if (insides is RRect) clipRRect(insides, ClipMode.DIFFERENCE, true)
        else clipRect(insides, ClipMode.DIFFERENCE, true)
        drawRectShadowNoclip(r, dx, dy, blur, spread, color)
        restore()
    } else drawRectShadowNoclip(r, dx, dy, blur, spread, color)
    return this
}

/**
 * 绘制矩形阴影
 */
public fun Canvas.drawRectShadowAntiAlias(r: Rect, shadow: Shadow): Canvas =
    drawRectShadowAntiAlias(r, shadow.offsetX, shadow.offsetY, shadow.blur, shadow.spread, shadow.shadowColor)

/**
 * 绘制圆角图片
 */
public fun Canvas.drawImageRRect(image: Image, srcRect: Rect, rRect: RRect, paint: Paint? = null) {
    save()
    clipRRect(rRect, true)
    drawImageRect(image, srcRect, rRect, defaultImageFilterMipmap, paint, false)
    restore()
}

/**
 * 绘制圆角图片
 */
public fun Canvas.drawImageRRect(image: Image, rRect: RRect, paint: Paint? = null): Unit =
    drawImageRRect(image, Rect(0f, 0f, image.width.toFloat(), image.height.toFloat()), rRect, paint)

/**
 * 裁剪绘制图片
 */
public fun Canvas.drawImageClip(
    image: Image,
    dstRect: RRect,
    paint: Paint? = null
) {
    if (dstRect.width == 0f || dstRect.height == 0f) return
    val ratio = image.width.toFloat() / image.height.toFloat()

    val srcRect = if (dstRect.width / ratio < dstRect.height) {
        val imgW = dstRect.width * image.height / dstRect.height
        val offsetX = (image.width - imgW) / 2f
        Rect.makeXYWH(offsetX, 0f, imgW, image.height.toFloat())
    } else {
        val imgH = dstRect.height * image.width / dstRect.width
        val offsetY = (image.height - imgH) / 2f
        Rect.makeXYWH(0f, offsetY, image.width.toFloat(), imgH)
    }

    drawImageRRect(image, srcRect, dstRect, paint)
}

/**
 * 修改透明度，透明度介于 0 ~ 1之间
 *
 * 用法: Color.WHITE.withAlpha(0.5f)
 */
public fun Int.withAlpha(alpha: Float = 1f): Int {
    require(alpha in 0f .. 1f) { "透明度介于 0 ~ 1之间" }
    return (255 * alpha).toInt() and 0xFF shl 24 or (this and 0x00FFFFFF)
}

/**
 * 角度转弧度
 */
public fun deg2Arc(deg: Int): Float = (deg * PI / 180).toFloat()

/**
 * 计算矩形横向中心
 */
public fun Rect.centerX(): Float = left + width / 2
/**
 * 计算矩形纵向中心
 */
public fun Rect.centerY(): Float = top + height / 2
