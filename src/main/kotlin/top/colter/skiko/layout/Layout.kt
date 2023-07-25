package top.colter.skiko.layout

import org.jetbrains.skia.*
import top.colter.skiko.*


/**
 * 计算大小
 */
fun interface Mensurable {
    /**
     * 进行计算
     *
     * [deep] 是否重新深入计算子元素
     */
    fun measure(deep: Boolean)
}

/**
 * 确定位置
 */
fun interface Placeable {
    fun place(bounds: LayoutBounds)
}

/**
 * 绘制
 */
fun interface Drawable {
    fun draw(canvas: Canvas)
}

/**
 * 布局的抽象实现
 */
abstract class Layout(
    var modifier: Modifier = Modifier(),
    var parentLayout: Layout? = null
) : Mensurable, Placeable, Drawable {

    // 位置
    var position: LayoutPosition = LayoutPosition()
    // 子元素列表
    var child: MutableList<Layout> = mutableListOf()

    // 宽高（不包括外边距）
    var width: Dp = Dp.NULL
    var height: Dp = Dp.NULL

    // 内容宽高（不包括内外边距）
    val contentWidth
        get() = if (width.isNotNull() && width - modifier.padding.horizontal > 0.dp) width - modifier.padding.horizontal else 0.dp
    val contentHeight
        get() = if (height.isNotNull() && height - modifier.padding.vertical > 0.dp) height - modifier.padding.vertical else 0.dp

    // 元素边界（包括内外边距）
    val boxWidth get() = width + modifier.margin.horizontal
    val boxHeight get() = height + modifier.margin.vertical

    /**
     * 第一遍计算宽高
     * 包括手动指定宽高、由父元素继承宽高
     */
    fun preMeasure() {
        // 手动指定宽高
        if (width.isNull() && modifier.width.isNotNull())
            width = modifier.width
        if (height.isNull() && modifier.height.isNotNull())
            height = modifier.height

        // 由父元素继承宽高且父元素宽高确定
        if (width.isNull() && modifier.fillMaxWidth && parentLayout?.modifier?.width?.isNotNull() == true) {
            modifier.width = parentLayout!!.modifier.contentWidth - modifier.margin.horizontal
            width = modifier.width
        }
        if (height.isNull() && modifier.fillMaxHeight && parentLayout?.modifier?.height?.isNotNull() == true) {
            modifier.height = parentLayout!!.modifier.contentHeight - modifier.margin.vertical
            height = modifier.height
        }

        // 按比例继承父元素宽高且父元素宽高确定
        if (width.isNull() && modifier.fillRatioWidth != 0f && parentLayout?.modifier?.width?.isNotNull() == true) {
            modifier.width = parentLayout!!.modifier.contentWidth * modifier.fillRatioWidth - modifier.margin.horizontal
            width = modifier.width
        }
        if (height.isNull() && modifier.fillRatioHeight != 0f && parentLayout?.modifier?.height?.isNotNull() == true) {
            modifier.height = parentLayout!!.modifier.contentHeight * modifier.fillRatioHeight - modifier.margin.vertical
            height = modifier.height
        }
    }

    /**
     * 绘制所有子元素
     */
    override fun draw(canvas: Canvas) {
        for (layout in child) {
            layout.draw(canvas)
        }
    }
}


/**
 *  Layout DSL
 */
inline fun <T : Layout> Layout.Layout(
    layout: T,
    content: T.() -> Unit
) {
    child.add(layout)
    layout.preMeasure()
    layout.content()
    layout.measure(false)
}


/**
 * 布局位置
 */
data class LayoutPosition(
    var x: Dp = Dp.NULL,
    var y: Dp = Dp.NULL
)

/**
 * 边界，用于限制子元素
 */
data class LayoutBounds(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp
) {
    val width: Dp get() = right - left
    val height: Dp get() = bottom - top

    companion object {
        fun makeXYWH(left: Dp = 0.dp, top: Dp = 0.dp, width: Dp = 0.dp, height: Dp = 0.dp): LayoutBounds {
            require(width >= 0.dp) { "width require >= 0, current: $width" }
            require(height >= 0.dp) { "height require >= 0, current: $height" }
            return LayoutBounds(left, top, left + width, top + height)
        }
    }
}


/**
 * 绘制盒子
 */
fun Layout.drawBgBox(canvas: Canvas, content: Canvas.(RRect) -> Unit = {}) {

    // 圆角
    val radius: FloatArray = if (modifier.border == null) floatArrayOf(0f, 0f, 0f, 0f)
    else modifier.border!!.radius.map { it.px }.toTypedArray().toFloatArray()

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
            canvas.drawRectShadowAntiAlias(rrect.inflate(modifier.border?.width?.px ?: 0f), it)
        }
    }

    // 绘制背景
    if (modifier.background != null) {
        val bg = modifier.background!!
        canvas.drawRRect(rrect, Paint().apply {
            color = bg.color
            if (bg.gradient != null) {
                shader = Shader.makeLinearGradient(
                    x0 = rrect.left,
                    y0 = rrect.top,
                    x1 = rrect.right,
                    y1 = rrect.top,
                    colors = bg.gradient.colors.toIntArray(),
                    positions = bg.gradient.positions?.toFloatArray()
                )
            }
        })
    }

    // 绘制内容
    canvas.save()
    canvas.clipRRect(rrect, ClipMode.INTERSECT, true)
    canvas.content(rrect)
    canvas.restore()

    // 绘制边框
    if (modifier.border != null && modifier.border!!.width.isNotNull()) {
        val border = modifier.border!!
        canvas.drawRRect(rrect.inflate(0.5f.px) as RRect, Paint().apply {
            color = border.color
            mode = PaintMode.STROKE
            strokeWidth = border.width.px
            isAntiAlias = true
        })
    }
}


