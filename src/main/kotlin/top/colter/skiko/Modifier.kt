package top.colter.skiko

import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import top.colter.skiko.data.*


/**
 * ## 样式
 *
 * [width] 宽度
 * [height] 高度
 *
 * [minWidth] 最小宽度
 * [minHeight] 最小高度
 *
 * [maxWidth] 最大宽度
 * [maxHeight] 最大高度
 *
 * [fillWidth] 填充剩余宽度
 * [fillHeight] 填充剩余高度
 *
 * [fillMaxWidth] 继承父元素宽度
 * [fillMaxHeight] 继承父元素高度
 *
 * [fillRatioWidth] 按比例继承父元素宽度
 * [fillRatioHeight] 按比例继承父元素高度
 *
 * [padding] 内边距
 * [margin] 外边距
 *
 * [background] 背景
 * [border] 边框
 * [shadows] 阴影
 *
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

public fun Modifier.width(width: Dp): Modifier {
    require(width > 0.dp) { "宽度必须大于0" }
    this.width = width
    return this
}
public fun Modifier.minWidth(width: Dp): Modifier = apply { this.minWidth = width }
public fun Modifier.maxWidth(width: Dp): Modifier = apply { this.maxWidth = width }

public fun Modifier.height(height: Dp): Modifier  {
    require(height > 0.dp) { "高度必须大于0" }
    this.height = height
    return this
}
public fun Modifier.minHeight(height: Dp): Modifier = apply { this.minHeight = height }
public fun Modifier.maxHeight(height: Dp): Modifier = apply { this.maxHeight = height }

public fun Modifier.fillRatioWidth(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioWidth = ratio
    return this
}
public fun Modifier.fillRatioHeight(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioHeight = ratio
    return this
}

public fun Modifier.margin(edge: Edge): Modifier = apply { this.margin = edge }
public fun Modifier.margin(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return margin(Edge(top, right, bottom, left))
}

public fun Modifier.margin(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return margin(Edge(vertical, horizontal, vertical, horizontal))
}

public fun Modifier.margin(edge: Dp): Modifier {
    return margin(Edge(edge, edge, edge, edge))
}


public fun Modifier.padding(edge: Edge): Modifier = apply { this.padding = edge }
public fun Modifier.padding(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return padding(Edge(top, right, bottom, left))
}

public fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return padding(Edge(vertical, horizontal, vertical, horizontal))
}

public fun Modifier.padding(edge: Dp): Modifier {
    return padding(Edge(edge, edge, edge, edge))
}


public fun Modifier.fillMaxSize(): Modifier = apply {
    fillMaxHeight = true
    fillMaxWidth = true
}

public fun Modifier.fillMaxWidth(): Modifier = apply { fillMaxWidth = true }
public fun Modifier.fillMaxHeight(): Modifier = apply { fillMaxHeight = true }

public fun Modifier.fillWidth(): Modifier = apply { fillWidth = true }
public fun Modifier.fillHeight(): Modifier = apply { fillHeight = true }

public fun Modifier.background(background: Background): Modifier = apply { this.background = background }
public fun Modifier.background(color: Int = Color.WHITE, gradient: Gradient? = null, image: Image? = null): Modifier {
    return background(Background(color, gradient, image))
}

public fun Modifier.border(border: Border): Modifier = apply { this.border = border }
public fun Modifier.border(
    width: Dp,
    radius: List<Dp> = listOf(0.dp, 0.dp, 0.dp, 0.dp),
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, radius, color))
}

public fun Modifier.border(
    width: Dp,
    radius: Dp,
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, listOf(radius, radius, radius, radius), color))
}

public fun Modifier.shadow(shadow: Shadow): Modifier = apply { this.shadows.add(shadow) }
public fun Modifier.shadow(
    offsetX: Float,
    offsetY: Float,
    blur: Float,
    spread: Float = 0f,
    shadowColor: Int
): Modifier {
    return shadow(Shadow(offsetX, offsetY, blur, spread, shadowColor))
}
public fun Modifier.shadows(shadows: List<Shadow>): Modifier = apply {
    this.shadows.addAll(shadows)
}

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