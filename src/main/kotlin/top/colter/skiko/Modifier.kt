package top.colter.skiko

import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import top.colter.skiko.data.*


/**
 * 样式
 */
class Modifier(
    var width: Dp = Dp.NULL,
    var height: Dp = Dp.NULL,

    var minWidth: Dp = Dp.NULL,
    var minHeight: Dp = Dp.NULL,

    var maxWidth: Dp = Dp.NULL,
    var maxHeight: Dp = Dp.NULL,

    var fillRatioWidth: Float = 0f,
    var fillRatioHeight: Float = 0f,

    var fillWidth: Boolean = false,
    var fillMaxWidth: Boolean = false,
    var fillHeight: Boolean = false,
    var fillMaxHeight: Boolean = false,

    var padding: Edge = Edge(),
    var margin: Edge = Edge(),

    var background: Background? = null,
    var border: Border? = null,
    var shadows: ArrayList<Shadow> = arrayListOf()

) {
    val contentWidth
        get() = if (width.isNotNull() && width - padding.horizontal > 0.dp) width - padding.horizontal else 0.dp
    val contentHeight
        get() = if (height.isNotNull() && height - padding.vertical > 0.dp) height - padding.vertical else 0.dp

    val boxWidth get() = width + margin.horizontal
    val boxHeight get() = height + margin.vertical
}

fun Modifier.width(width: Dp): Modifier {
    require(width > 0.dp) { "宽度必须大于0" }
    this.width = width
    return this
}
fun Modifier.minWidth(width: Dp): Modifier = apply { this.minWidth = width }
fun Modifier.maxWidth(width: Dp): Modifier = apply { this.maxWidth = width }

fun Modifier.height(height: Dp): Modifier  {
    require(height > 0.dp) { "高度必须大于0" }
    this.height = height
    return this
}
fun Modifier.minHeight(height: Dp): Modifier = apply { this.minHeight = height }
fun Modifier.maxHeight(height: Dp): Modifier = apply { this.maxHeight = height }

fun Modifier.fillRatioWidth(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioWidth = ratio
    return this
}
fun Modifier.fillRatioHeight(ratio: Float): Modifier  {
    require(ratio in 0f .. 1f) { "比例介于 0 ~ 1之间" }
    this.fillRatioHeight = ratio
    return this
}

fun Modifier.margin(edge: Edge): Modifier = apply { this.margin = edge }
fun Modifier.margin(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return margin(Edge(top, right, bottom, left))
}

fun Modifier.margin(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return margin(Edge(vertical, horizontal, vertical, horizontal))
}

fun Modifier.margin(edge: Dp): Modifier {
    return margin(Edge(edge, edge, edge, edge))
}


fun Modifier.padding(edge: Edge): Modifier = apply { this.padding = edge }
fun Modifier.padding(top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp, left: Dp = 0.dp): Modifier {
    return padding(Edge(top, right, bottom, left))
}

fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp): Modifier {
    return padding(Edge(vertical, horizontal, vertical, horizontal))
}

fun Modifier.padding(edge: Dp): Modifier {
    return padding(Edge(edge, edge, edge, edge))
}


fun Modifier.fillMaxSize(): Modifier = apply {
    fillMaxHeight = true
    fillMaxWidth = true
}

fun Modifier.fillMaxWidth(): Modifier = apply { fillMaxWidth = true }
fun Modifier.fillMaxHeight(): Modifier = apply { fillMaxHeight = true }

fun Modifier.fillWidth(): Modifier = apply { fillWidth = true }
fun Modifier.fillHeight(): Modifier = apply { fillHeight = true }

fun Modifier.background(background: Background): Modifier = apply { this.background = background }
fun Modifier.background(color: Int = Color.WHITE, gradient: Gradient? = null, image: Image? = null): Modifier {
    return background(Background(color, gradient, image))
}

fun Modifier.border(border: Border): Modifier = apply { this.border = border }
fun Modifier.border(
    width: Dp,
    radius: List<Dp> = listOf(0.dp, 0.dp, 0.dp, 0.dp),
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, radius, color))
}

fun Modifier.border(
    width: Dp,
    radius: Dp,
    color: Int = Color.WHITE,
): Modifier {
    return border(Border(width, listOf(radius, radius, radius, radius), color))
}

fun Modifier.shadow(shadow: Shadow): Modifier = apply { this.shadows.add(shadow) }
fun Modifier.shadow(
    offsetX: Float,
    offsetY: Float,
    blur: Float,
    spread: Float = 0f,
    shadowColor: Int
): Modifier {
    return shadow(Shadow(offsetX, offsetY, blur, spread, shadowColor))
}
fun Modifier.shadows(shadows: List<Shadow>) = apply {
    this.shadows.addAll(shadows)
}

fun Modifier.merge(modifier: Modifier) {
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