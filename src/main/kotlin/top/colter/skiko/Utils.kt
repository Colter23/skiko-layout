package top.colter.skiko

import org.jetbrains.skia.*
import top.colter.skiko.data.Shadow
import top.colter.skiko.layout.Layout
import kotlin.math.PI


/**
 * Emoji正则(可匹配组合emoji)
 */
public const val emojiCharacter: String =
    "(?:[\\uD83C\\uDF00-\\uD83D\\uDDFF]|[\\uD83E\\uDD00-\\uD83E\\uDDFF]|[\\uD83D\\uDE00-\\uD83D\\uDE4F]|[\\uD83D\\uDE80-\\uD83D\\uDEFF]|[\\u2600-\\u26FF]\\uFE0F?|[\\u2700-\\u27BF]\\uFE0F?|\\u24C2\\uFE0F?|[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{1,2}|[\\uD83C\\uDD70\\uD83C\\uDD71\\uD83C\\uDD7E\\uD83C\\uDD7F\\uD83C\\uDD8E\\uD83C\\uDD91-\\uD83C\\uDD9A]\\uFE0F?|[\\u0023\\u002A\\u0030-\\u0039]\\uFE0F?\\u20E3|[\\u2194-\\u2199\\u21A9-\\u21AA]\\uFE0F?|[\\u2B05-\\u2B07\\u2B1B\\u2B1C\\u2B50\\u2B55]\\uFE0F?|[\\u2934\\u2935]\\uFE0F?|[\\u3030\\u303D]\\uFE0F?|[\\u3297\\u3299]\\uFE0F?|[\\uD83C\\uDE01\\uD83C\\uDE02\\uD83C\\uDE1A\\uD83C\\uDE2F\\uD83C\\uDE32-\\uD83C\\uDE3A\\uD83C\\uDE50\\uD83C\\uDE51]\\uFE0F?|[\\u203C\\u2049]\\uFE0F?|[\\u25AA\\u25AB\\u25B6\\u25C0\\u25FB-\\u25FE]\\uFE0F?|[\\u00A9\\u00AE]\\uFE0F?|[\\u2122\\u2139]\\uFE0F?|\\uD83C\\uDC04\\uFE0F?|\\uD83C\\uDCCF\\uFE0F?|[\\u231A\\u231B\\u2328\\u23CF\\u23E9-\\u23F3\\u23F8-\\u23FA]\\uFE0F?)(?:[\\uD83C\\uDFFB-\\uD83C\\uDFFF]|[\\uD83E\\uDDB0-\\uD83E\\uDDB3])?"
public val emojiRegex: Regex = "$emojiCharacter(?:\\u200D$emojiCharacter)*".toRegex()


public fun List<Layout>.sumWidth(): Dp = sumOf { width + modifier.margin.horizontal }
public fun List<Layout>.sumHeight(): Dp = sumOf { height + modifier.margin.vertical }
public fun List<Layout>.sumOf(block: Layout.() -> Dp): Dp {
    var x = 0.dp
    forEach { x += it.block() }
    return x
}

public fun List<Layout>.maxWidth(): Dp = maxOf { width + modifier.margin.horizontal }
public fun List<Layout>.maxHeight(): Dp = maxOf { height + modifier.margin.vertical }
public fun List<Layout>.maxOf(block: Layout.() -> Dp): Dp {
    var x = 0.dp
    forEach { x = Dp.max(x, it.block()) }
    return x
}

public fun <T> List<T>.ifNotEmpty(block: List<T>.() -> Unit): Unit = if (isNotEmpty()) block() else { }
public fun <T> List<T>.ifNotEmptyForEach(block: (T) -> Unit): Unit = ifNotEmpty {
    forEach { block(it) }
}

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
    drawImageRect(image, srcRect, rRect, FilterMipmap(FilterMode.LINEAR, MipmapMode.NEAREST), paint, false)
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
        val offsetY = (image.height - imgH) / 2
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

public fun Rect.centerX(): Float = left + width / 2
public fun Rect.centerY(): Float = top + height / 2
