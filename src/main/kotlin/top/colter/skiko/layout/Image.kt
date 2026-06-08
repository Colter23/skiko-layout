package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import top.colter.skiko.*
import top.colter.skiko.data.GradientBlur
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.Ratio
import top.colter.skiko.data.place


/**
 * ## 图片元素
 *
 * [ratio] 宽高比 例: 16f/10f。为 0 时表示保持图片原比例。可使用 [Ratio] 对象内置的比例
 *
 * @param image 图片
 * @param ratio 宽高比
 * @param modifier 样式
 * @param alignment 对齐
 * @param cropAlignment cover 裁剪图片时源图的取景位置，默认为居中裁剪
 */
public fun Layout.Image(
    image: Image,
    ratio: Float = 0f,
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    alpha: Float = 1f,
    gradientBlur: GradientBlur? = null,
    cropAlignment: LayoutAlignment = LayoutAlignment.CENTER,
) {
    require(alpha in 0f..1f) { "图片透明度需在 0..1 之间" }
    Layout(
        layout = ImageLayout(
            image = image,
            ratio = ratio,
            alignment = alignment,
            alpha = alpha,
            gradientBlur = gradientBlur,
            cropAlignment = cropAlignment,
            modifier = modifier,
            parentLayout = this
        ),
        content = {},
    )
}
public class ImageLayout(
    public val image: Image,
    public val ratio: Float,
    public val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
    public val alpha: Float = 1f,
    public val gradientBlur: GradientBlur? = null,
    public val cropAlignment: LayoutAlignment = LayoutAlignment.CENTER,
) : Layout(modifier, parentLayout, fontRegistry) {
    init {
        require(alpha in 0f..1f) { "图片透明度需在 0..1 之间" }
    }

    private var cachedGradientBlurKey: GradientBlurCacheKey? = null
    private var cachedGradientBlurImage: Image? = null

    private fun naturalRatio(): Float = if (ratio != 0f) ratio else image.width.toFloat() / image.height.toFloat()

    private fun contentWidthFromOuter(outerWidth: Dp): Dp =
        (outerWidth - resolvedPadding.horizontal).coerceAtLeast(0.dp)

    private fun contentHeightFromOuter(outerHeight: Dp): Dp =
        (outerHeight - resolvedPadding.vertical).coerceAtLeast(0.dp)

    private fun fitRatioSize(
        preferredWidth: Float,
        preferredHeight: Float,
        minWidth: Float,
        minHeight: Float,
        maxWidth: Float,
        maxHeight: Float
    ): Pair<Float, Float> {
        var widthPx = preferredWidth.coerceAtLeast(0f)
        var heightPx = preferredHeight.coerceAtLeast(0f)

        fun scaleTo(limitWidth: Float, limitHeight: Float, shrinkOnly: Boolean): Boolean {
            if (widthPx <= 0f || heightPx <= 0f) return false
            val scale = minOf(limitWidth / widthPx, limitHeight / heightPx)
            if (scale.isNaN() || scale.isInfinite()) return false
            if (shrinkOnly && scale >= 1f) return false
            if (!shrinkOnly && scale <= 1f) return false
            widthPx *= scale
            heightPx *= scale
            return true
        }

        if (maxWidth.isFinite() || maxHeight.isFinite()) {
            val widthLimit = if (maxWidth.isFinite()) maxWidth else Float.POSITIVE_INFINITY
            val heightLimit = if (maxHeight.isFinite()) maxHeight else Float.POSITIVE_INFINITY
            scaleTo(widthLimit, heightLimit, shrinkOnly = true)
        }

        if (minWidth > 0f || minHeight > 0f) {
            val widthLimit = if (minWidth > 0f) minWidth else 0f
            val heightLimit = if (minHeight > 0f) minHeight else 0f
            if (widthPx < widthLimit || heightPx < heightLimit) {
                val scale = maxOf(
                    if (widthPx > 0f && widthLimit > 0f) widthLimit / widthPx else 0f,
                    if (heightPx > 0f && heightLimit > 0f) heightLimit / heightPx else 0f,
                    1f
                )
                widthPx *= scale
                heightPx *= scale
            }
        }

        if (maxWidth.isFinite() || maxHeight.isFinite()) {
            val widthLimit = if (maxWidth.isFinite()) maxWidth else Float.POSITIVE_INFINITY
            val heightLimit = if (maxHeight.isFinite()) maxHeight else Float.POSITIVE_INFINITY
            scaleTo(widthLimit, heightLimit, shrinkOnly = true)
        }

        return widthPx to heightPx
    }

    override fun measure(deep: Boolean) {
        preMeasure()

        val aspectRatio = naturalRatio()
        val naturalWidth = image.width.toFloat().toDp()
        val naturalHeight = image.height.toFloat().toDp()

        val explicitWidth = width.isNotNull()
        val explicitHeight = height.isNotNull()

        if (explicitWidth && explicitHeight) {
            finishMeasure()
            return
        }

        val availableWidth = availableParentWidth()
        val preferredOuterWidth = when {
            explicitWidth -> width
            modifier.maxWidth.isNotNull() -> modifier.maxWidth
            availableWidth.isNotNull() && !modifier.fillWidth && !modifier.fillMaxWidth -> availableWidth
            else -> naturalWidth + resolvedPadding.horizontal
        }

        val preferredOuterHeight = when {
            explicitHeight -> height
            modifier.maxHeight.isNotNull() -> modifier.maxHeight
            else -> Dp.NULL
        }

        var contentWidth = if (preferredOuterWidth.isNotNull()) contentWidthFromOuter(preferredOuterWidth) else 0.dp
        var contentHeight = if (preferredOuterHeight.isNotNull()) contentHeightFromOuter(preferredOuterHeight) else 0.dp

        if (!explicitWidth && !explicitHeight) {
            contentWidth = when {
                contentWidth.isNotNull() && contentWidth > 0.dp -> contentWidth
                naturalWidth > 0.dp -> naturalWidth
                else -> 0.dp
            }
            contentHeight = if (contentWidth > 0.dp) (contentWidth.px / aspectRatio).toDp() else naturalHeight
        } else if (explicitWidth && !explicitHeight) {
            contentHeight = if (contentWidth > 0.dp) (contentWidth.px / aspectRatio).toDp() else naturalHeight
        } else if (!explicitWidth && explicitHeight) {
            contentWidth = if (contentHeight > 0.dp) (contentHeight.px * aspectRatio).toDp() else naturalWidth
        }

        val minContentWidth = (modifier.minWidth - resolvedPadding.horizontal).coerceAtLeast(0.dp).px
        val minContentHeight = (modifier.minHeight - resolvedPadding.vertical).coerceAtLeast(0.dp).px
        val maxContentWidth = if (modifier.maxWidth.isNotNull()) contentWidthFromOuter(modifier.maxWidth).px else Float.POSITIVE_INFINITY
        val maxContentHeight = if (modifier.maxHeight.isNotNull()) contentHeightFromOuter(modifier.maxHeight).px else Float.POSITIVE_INFINITY

        val fitted = fitRatioSize(
            preferredWidth = contentWidth.px,
            preferredHeight = contentHeight.px,
            minWidth = minContentWidth,
            minHeight = minContentHeight,
            maxWidth = maxContentWidth,
            maxHeight = maxContentHeight
        )

        width = fitted.first.toDp() + resolvedPadding.horizontal
        height = fitted.second.toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas) {
            if (gradientBlur == null) {
                drawImageClip(image, it, imageAlphaPaint(alpha), cropAlignment)
            } else {
                val (targetWidth, targetHeight) = gradientBlurTargetSize(it)
                val key = gradientBlurCacheKey(image, targetWidth, targetHeight, gradientBlur, cropAlignment)
                val blurredImage = if (cachedGradientBlurKey == key && cachedGradientBlurImage != null) {
                    cachedGradientBlurImage!!
                } else {
                    image.gradientBlurred(targetWidth, targetHeight, gradientBlur, cropAlignment).also { result ->
                        cachedGradientBlurKey = key
                        cachedGradientBlurImage = result
                    }
                }
                drawImageRRect(blurredImage, it, imageAlphaPaint(alpha))
            }
        }
    }
}
