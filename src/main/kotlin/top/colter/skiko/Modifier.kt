package top.colter.skiko

import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import top.colter.skiko.data.*


/**
 * ## 样式
 *
 * @property width 宽度
 * @property height 高度
 * @property minWidth 最小宽度
 * @property minHeight 最小高度
 * @property maxWidth 最大宽度
 * @property maxHeight 最大高度
 * @property fillWidth 填充剩余宽度
 * @property fillHeight 填充剩余高度
 * @property fillMaxWidth 继承父元素宽度
 * @property fillMaxHeight 继承父元素高度
 * @property fillRatioWidth 按比例继承父元素宽度
 * @property fillRatioHeight 按比例继承父元素高度
 * @property padding 内边距
 * @property margin 外边距
 * @property background 背景
 * @property border 边框
 * @property shadows 阴影
 */
public class Modifier(
    public var width: Dp = Dp.NULL,
    public var height: Dp = Dp.NULL,

    public var minWidth: Dp = Dp.NULL,
    public var minHeight: Dp = Dp.NULL,

    public var maxWidth: Dp = Dp.NULL,
    public var maxHeight: Dp = Dp.NULL,

    public var fillRatioWidth: Float = 0f,
    public var fillRatioHeight: Float = 0f,

    public var fillWidth: Boolean = false,
    public var fillMaxWidth: Boolean = false,
    public var fillHeight: Boolean = false,
    public var fillMaxHeight: Boolean = false,

    public var padding: Edge = Edge(),
    public var margin: Edge = Edge(),

    public var background: Background? = null,
    public var border: Border? = null,
    public var shadows: ArrayList<Shadow> = arrayListOf()

) {
    public val contentWidth: Dp
        get() = if (width.isNotNull() && width - padding.horizontal > 0.dp) width - padding.horizontal else 0.dp
    public val contentHeight: Dp
        get() = if (height.isNotNull() && height - padding.vertical > 0.dp) height - padding.vertical else 0.dp

    public val boxWidth: Dp get() = width + margin.horizontal
    public val boxHeight: Dp get() = height + margin.vertical
}

/**
 * 指定宽度
 * @param width 宽度（必须大于0）
 */
public fun Modifier.width(width: Dp): Modifier {
    require(width > 0.dp) { "宽度必须大于0" }
    this.width = width
    return this
}
/**
 * 最小宽度
 */
public fun Modifier.minWidth(width: Dp): Modifier = apply { this.minWidth = width }
/**
 * 最大宽度
 */
public fun Modifier.maxWidth(width: Dp): Modifier = apply { this.maxWidth = width }


/**
 * 指定高度
 * @param height 高度（必须大于0）
 */
public fun Modifier.height(height: Dp): Modifier  {
    require(height > 0.dp) { "高度必须大于0" }
    this.height = height
    return this
}
/**
 * 最小高度
 */
public fun Modifier.minHeight(height: Dp): Modifier = apply { this.minHeight = height }
/**
 * 最大高度
 */
public fun Modifier.maxHeight(height: Dp): Modifier = apply { this.maxHeight = height }


/**
 * 按比例填充宽度
 * @param ratio 比例（介于 0 ~ 1之间）
 */
public fun Modifier.fillRatioWidth(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioWidth = ratio
    return this
}
/**
 * 按比例填充高度
 * @param ratio 比例（介于 0 ~ 1之间）
 */
public fun Modifier.fillRatioHeight(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioHeight = ratio
    return this
}


/**
 * 外边距
 * @param edge [Edge]
 */
public fun Modifier.margin(edge: Edge): Modifier = apply { this.margin = edge }
/**
 * 外边距
 * @param top 上边距
 * @param right 右边距
 * @param bottom 下边距
 * @param left 左边距
 */
public fun Modifier.margin(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return margin(Edge(top, right, bottom, left))
}
/**
 * 外边距
 * @param horizontal 水平边距
 * @param vertical 垂直边距
 */
public fun Modifier.margin(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return margin(Edge(vertical, horizontal, vertical, horizontal))
}
/**
 * 外边距
 * @param edge 四边边距
 */
public fun Modifier.margin(edge: Dp): Modifier {
    return margin(Edge(edge, edge, edge, edge))
}


/**
 * 内边距
 * @param edge [Edge]
 */
public fun Modifier.padding(edge: Edge): Modifier = apply { this.padding = edge }
/**
 * 内边距
 * @param top 上边距
 * @param right 右边距
 * @param bottom 下边距
 * @param left 左边距
 */
public fun Modifier.padding(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return padding(Edge(top, right, bottom, left))
}
/**
 * 内边距
 * @param horizontal 水平边距
 * @param vertical 垂直边距
 */
public fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return padding(Edge(vertical, horizontal, vertical, horizontal))
}
/**
 * 内边距
 * @param edge 四边边距
 */
public fun Modifier.padding(edge: Dp): Modifier {
    return padding(Edge(edge, edge, edge, edge))
}


/**
 * 填充最大宽高
 */
public fun Modifier.fillMaxSize(): Modifier = apply {
    fillMaxHeight = true
    fillMaxWidth = true
}
/**
 * 填充最大宽度
 */
public fun Modifier.fillMaxWidth(): Modifier = apply { fillMaxWidth = true }
/**
 * 填充最大高度
 */
public fun Modifier.fillMaxHeight(): Modifier = apply { fillMaxHeight = true }
/**
 * 填充剩余宽度
 */
public fun Modifier.fillWidth(): Modifier = apply { fillWidth = true }
/**
 * 填充剩余高度
 */
public fun Modifier.fillHeight(): Modifier = apply { fillHeight = true }


/**
 * 背景
 * @param background [Background]
 */
public fun Modifier.background(background: Background): Modifier = apply { this.background = background }
/**
 * 背景
 *
 * 最少指定一个属性
 * @param color 颜色
 * @param gradient 渐变色
 * @param image 图片
 */
public fun Modifier.background(color: Int? = null, gradient: Gradient? = null, image: Image? = null): Modifier {
    return background(Background(color, gradient, image))
}


/**
 * 边框
 * @param border [Border]
 */
public fun Modifier.border(border: Border): Modifier = apply { this.border = border }
/**
 * 边框
 * @param width 宽度
 * @param radius 四角弧度列表 顺序为左上开始顺时针 左上↖ 右上↗ 右下↘ 左下↙
 * @param color 颜色
 */
public fun Modifier.border(
    width: Dp,
    radius: List<Dp> = listOf(0.dp, 0.dp, 0.dp, 0.dp),
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, radius, color))
}
/**
 * 边框
 * @param width 宽度
 * @param radius 四角弧度
 * @param color 颜色
 */
public fun Modifier.border(
    width: Dp,
    radius: Dp,
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, listOf(radius, radius, radius, radius), color))
}


/**
 * 阴影
 * @param shadow [Shadow]
 */
public fun Modifier.shadow(shadow: Shadow): Modifier = apply { this.shadows.add(shadow) }
/**
 * 阴影
 * @param offsetX X偏移
 * @param offsetY Y便宜
 * @param blur 模糊
 * @param spread 扩散
 * @param shadowColor 阴影颜色
 */
public fun Modifier.shadow(
    offsetX: Float,
    offsetY: Float,
    blur: Float,
    spread: Float = 0f,
    shadowColor: Int
): Modifier {
    return shadow(Shadow(offsetX, offsetY, blur, spread, shadowColor))
}
/**
 * 阴影列表
 * @param shadows 多个阴影
 */
public fun Modifier.shadows(shadows: List<Shadow>): Modifier = apply {
    this.shadows.addAll(shadows)
}


/**
 * 合并样式
 */
public fun Modifier.merge(modifier: Modifier) {
    if (modifier.width.isNotNull()) width = modifier.width
    if (modifier.height.isNotNull()) height = modifier.height
    if (modifier.minWidth.isNotNull()) minWidth = modifier.minWidth
    if (modifier.minHeight.isNotNull()) minHeight = modifier.minHeight
    if (modifier.maxWidth.isNotNull()) maxWidth = modifier.maxWidth
    if (modifier.maxHeight.isNotNull()) maxHeight = modifier.maxHeight
    if (modifier.fillRatioWidth != 0f) fillRatioWidth = modifier.fillRatioWidth
    if (modifier.fillRatioHeight != 0f) fillRatioHeight = modifier.fillRatioHeight

    if (modifier.fillWidth) fillWidth = true
    if (modifier.fillMaxWidth) fillMaxWidth = true
    if (modifier.fillHeight) fillHeight = true
    if (modifier.fillMaxHeight) fillMaxHeight = true

    if (modifier.padding.isNotEmpty()) padding = modifier.padding
    if (modifier.margin.isNotEmpty()) margin = modifier.margin

    if (modifier.background != null) background = modifier.background
    if (modifier.border != null) border = modifier.border
    if (modifier.shadows.isNotEmpty()) shadows = modifier.shadows
}