package top.colter.skiko.layout

import org.jetbrains.skia.*
import top.colter.skiko.*
import top.colter.skiko.data.BoxShape
import top.colter.skiko.data.AxisAlignment
import top.colter.skiko.data.Border


/**
 * 计算大小
 */
public fun interface Mensurable {
    /**
     * 进行计算
     *
     * [deep] 是否重新深入计算子元素
     */
    public fun measure(deep: Boolean)
}

/**
 * 确定位置
 */
public fun interface Placeable {
    public fun place(bounds: LayoutBounds)
}

/**
 * 绘制
 */
public fun interface Drawable {
    public fun draw(canvas: Canvas)
}

/**
 * ## 环境变量
 *
 * 只操作当前节点，创建节点时会自动继承父节点的数据
 */
public interface Environment{
    public fun containsEnv(key: String): Boolean
    public fun getEnv(key: String): Any?
    public fun getAllEnv(): Map<String, Any>?
    public fun putEnv(key: String, value: Any)
    public fun removeEnv(key: String): Any?
}


/**
 * 布局的抽象实现
 */
public abstract class Layout(
    public var modifier: Modifier = Modifier(),
    public var parentLayout: Layout? = null
) : Mensurable, Placeable, Drawable, Environment {

    // 环境变量
    private val environment: MutableMap<String, Any> = parentLayout?.getAllEnv()?.let { LinkedHashMap(it) } ?: LinkedHashMap()

    // 位置
    public var position: LayoutPosition = LayoutPosition()
    // 子元素列表
    public var child: MutableList<Layout> = mutableListOf()

    internal val drawFillPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    internal val drawStrokePaint: Paint = Paint().apply {
        isAntiAlias = true
        mode = PaintMode.STROKE
    }
    internal val drawGradientPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    internal val drawCornerRadii: FloatArray = FloatArray(4)

    // 宽高（不包括外边距）
    public var width: Dp = Dp.NULL
    public var height: Dp = Dp.NULL

    // 内容宽高（不包括内外边距）
    public val contentWidth: Dp
        get() = if (width.isNotNull() && width - modifier.padding.horizontal > 0.dp) width - modifier.padding.horizontal else 0.dp
    public val contentHeight: Dp
        get() = if (height.isNotNull() && height - modifier.padding.vertical > 0.dp) height - modifier.padding.vertical else 0.dp

    // 元素边界（包括内外边距）
    public val boxWidth: Dp get() = width + modifier.margin.horizontal
    public val boxHeight: Dp get() = height + modifier.margin.vertical

    protected fun availableParentWidth(): Dp {
        val parentWidth = parentLayout?.contentWidth ?: Dp.NULL
        return if (parentWidth.isNotNull()) (parentWidth - modifier.margin.horizontal).coerceAtLeast(0.dp) else Dp.NULL
    }

    protected fun availableParentHeight(): Dp {
        val parentHeight = parentLayout?.contentHeight ?: Dp.NULL
        return if (parentHeight.isNotNull()) (parentHeight - modifier.margin.vertical).coerceAtLeast(0.dp) else Dp.NULL
    }

    protected fun constrainWidth(value: Dp): Dp = constrainSize(value, modifier.minWidth, modifier.maxWidth, "width")

    protected fun constrainHeight(value: Dp): Dp = constrainSize(value, modifier.minHeight, modifier.maxHeight, "height")

    protected fun finishMeasure() {
        width = constrainWidth(width)
        height = constrainHeight(height)
    }

    private fun constrainSize(value: Dp, min: Dp, max: Dp, axis: String): Dp {
        require(min.isNull() || max.isNull() || min <= max) { "$axis min require <= max" }

        var result = value.coerceAtLeast(0.dp)
        if (min.isNotNull()) result = result.coerceAtLeast(min)
        if (max.isNotNull()) result = result.coerceAtMost(max)
        return result
    }

    /**
     * 第一遍计算宽高
     * 包括手动指定宽高、由父元素继承宽高
     */
    public fun preMeasure() {
        // 手动指定宽高
        if (width.isNull() && modifier.width.isNotNull()) width = constrainWidth(modifier.width)
        if (height.isNull() && modifier.height.isNotNull()) height = constrainHeight(modifier.height)

        val parentContentWidth = parentLayout?.contentWidth ?: Dp.NULL
        val parentContentHeight = parentLayout?.contentHeight ?: Dp.NULL

        // 由父元素继承宽高且父元素宽高确定
        if (width.isNull() && modifier.fillMaxWidth && parentContentWidth.isNotNull())
            width = constrainWidth((parentContentWidth - modifier.margin.horizontal).coerceAtLeast(0.dp))
        if (height.isNull() && modifier.fillMaxHeight && parentContentHeight.isNotNull())
            height = constrainHeight((parentContentHeight - modifier.margin.vertical).coerceAtLeast(0.dp))

        // 按比例继承父元素宽高且父元素宽高确定
        if (width.isNull() && modifier.fillRatioWidth != 0f && parentContentWidth.isNotNull())
            width = constrainWidth((parentContentWidth * modifier.fillRatioWidth - modifier.margin.horizontal).coerceAtLeast(0.dp))
        if (height.isNull() && modifier.fillRatioHeight != 0f && parentContentHeight.isNotNull())
            height = constrainHeight((parentContentHeight * modifier.fillRatioHeight - modifier.margin.vertical).coerceAtLeast(0.dp))

        if (modifier.aspectRatio > 0f) {
            when {
                width.isNotNull() && height.isNull() -> {
                    height = constrainHeight((width.px / modifier.aspectRatio).toDp())
                }
                height.isNotNull() && width.isNull() -> {
                    width = constrainWidth((height.px * modifier.aspectRatio).toDp())
                }
            }
        }

        if (width.isNotNull()) width = constrainWidth(width)
        if (height.isNotNull()) height = constrainHeight(height)
    }

    /**
     * 绘制所有子元素
     */
    override fun draw(canvas: Canvas) {
        for (layout in child) {
            layout.draw(canvas)
        }
    }

    override fun containsEnv(key: String): Boolean {
        return environment.contains(key)
    }

    override fun getEnv(key: String): Any? {
        return environment[key]
    }

    override fun getAllEnv(): Map<String, Any>? {
        if (environment.isEmpty()) return null
        return environment.toMap()
    }

    override fun putEnv(key: String, value: Any) {
        environment[key] = value
    }

    override fun removeEnv(key: String): Any? {
        return environment.remove(key)
    }

}


/**
 *  Layout DSL
 */
public inline fun <T : Layout> Layout.Layout(
    layout: T,
    content: T.() -> Unit
) {
    child.add(layout)
    layout.content()
}


/**
 * 布局位置
 */
public data class LayoutPosition(
    var x: Dp = Dp.NULL,
    var y: Dp = Dp.NULL
)

/**
 * 边界，用于限制子元素
 */
public data class LayoutBounds(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp
) {
    val width: Dp get() = right - left
    val height: Dp get() = bottom - top

    public companion object {
        public fun makeXYWH(left: Dp = 0.dp, top: Dp = 0.dp, width: Dp = 0.dp, height: Dp = 0.dp): LayoutBounds {
            require(width >= 0.dp) { "width require >= 0, current: $width" }
            require(height >= 0.dp) { "height require >= 0, current: $height" }
            return LayoutBounds(left, top, left + width, top + height)
        }
    }
}


/**
 * 绘制盒子
 */
public fun Layout.drawBgBox(canvas: Canvas, content: Canvas.(RRect) -> Unit = {}) {

    // 圆角 / 形状
    val border = modifier.border
    val radius = resolveCornerRadius(border)

    val rrect = RRect.makeComplexXYWH(
        position.x.px,
        position.y.px,
        width.px,
        height.px,
        radius
    )

    // 绘制阴影
    if (modifier.shadows.isNotEmpty()) {
        modifier.shadows.forEach {
            canvas.drawRectShadowAntiAlias(rrect.inflate(border?.width?.px ?: 0f), it)
        }
    }

    // 绘制背景
    if (modifier.background != null) {
        val bg = modifier.background!!

        // 绘制背景图片
        if (bg.image != null) {
            canvas.drawImageClip(bg.image, rrect)
        }

        // 绘制渐变色
        if (bg.gradient != null) {
            val start = bg.gradient.start
            val end = bg.gradient.end

            val x0 = when (start.horizontal) {
                AxisAlignment.START -> rrect.left
                AxisAlignment.CENTER -> rrect.left + (rrect.right - rrect.left) / 2
                AxisAlignment.END -> rrect.right
            }
            val y0 = when (start.vertical) {
                AxisAlignment.START -> rrect.top
                AxisAlignment.CENTER -> rrect.top + (rrect.bottom - rrect.top) / 2
                AxisAlignment.END -> rrect.bottom
            }
            val x1 = when (end.horizontal) {
                AxisAlignment.START -> rrect.left
                AxisAlignment.CENTER -> rrect.left + (rrect.right - rrect.left) / 2
                AxisAlignment.END -> rrect.right
            }
            val y1 = when (end.vertical) {
                AxisAlignment.START -> rrect.top
                AxisAlignment.CENTER -> rrect.top + (rrect.bottom - rrect.top) / 2
                AxisAlignment.END -> rrect.bottom
            }
            canvas.drawRRect(rrect, drawGradientPaint.apply {
                shader = Shader.makeLinearGradient(
                    x0 = x0,
                    y0 = y0,
                    x1 = x1,
                    y1 = y1,
                    colors = bg.gradient.colors.toIntArray(),
                )
            })
        }

        // 绘制纯色
        if (bg.color != null) {
            canvas.drawRRect(rrect, drawFillPaint.apply {
                shader = null
                color = bg.color
            })
        }

    }

    // 绘制内容
    if (contentWidth > 0.dp && contentHeight > 0.dp) {
        val contentRRect = RRect.makeComplexXYWH(
            position.x.px + modifier.padding.left.px,
            position.y.px + modifier.padding.top.px,
            contentWidth.px,
            contentHeight.px,
            floatArrayOf(0f, 0f, 0f, 0f)
        )
        canvas.save()
        canvas.clipRRect(rrect, ClipMode.INTERSECT, true)
        canvas.clipRRect(contentRRect, ClipMode.INTERSECT, true)
        canvas.content(contentRRect)
        canvas.restore()
    }

    // 绘制边框
    if (border != null && border.width.isNotNull()) {
        canvas.drawRRect(rrect.inflate(0.5f.px) as RRect, drawStrokePaint.apply {
            shader = null
            color = border.color
            mode = PaintMode.STROKE
            strokeWidth = border.width.px
            isAntiAlias = true
        })
    }
}

private fun Layout.resolveCornerRadius(border: Border?): FloatArray {
    val radius = drawCornerRadii
    val maxRadius = if (width.isNotNull() && height.isNotNull()) minOf(width.px, height.px) / 2f else Float.MAX_VALUE

    fun clamp(value: Float): Float = value.coerceAtLeast(0f).coerceAtMost(maxRadius)

    when (val shape = modifier.shape) {
        BoxShape.Rectangle -> {
            val source = border?.radius ?: emptyList()
            for (index in 0 until radius.size) {
                radius[index] = clamp(source.getOrElse(index) { 0.dp }.px)
            }
        }

        is BoxShape.Rounded -> {
            for (index in 0 until radius.size) {
                radius[index] = clamp(shape.radius.getOrElse(index) { 0.dp }.px)
            }
        }

        is BoxShape.RelativeRounded -> {
            val value = clamp(minOf(width.px, height.px) * shape.ratio)
            radius.fill(value)
        }

        BoxShape.Circle -> {
            val value = clamp(minOf(width.px, height.px) / 2f)
            radius.fill(value)
        }
    }

    return radius
}


