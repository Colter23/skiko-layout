package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * ## 视觉偏移
 *
 * 只移动绘制矩形和子元素可用区域，不改变布局占位。
 */
public data class VisualOffset(
    val x: Dp = 0.dp,
    val y: Dp = 0.dp
) {
    public fun isEmpty(): Boolean = x == 0.dp && y == 0.dp
    public fun isNotEmpty(): Boolean = !isEmpty()
}

/**
 * ## 比例视觉偏移
 *
 * 按父级可用内容区解析为 [VisualOffset]。允许负数，用于让元素视觉上移出父级。
 */
public data class VisualOffsetRatio(
    val x: Float = 0f,
    val y: Float = 0f
) {
    public fun isEmpty(): Boolean = x == 0f && y == 0f
    public fun isNotEmpty(): Boolean = !isEmpty()

    internal fun resolve(parentWidth: Dp, parentHeight: Dp): VisualOffset {
        val width = if (parentWidth.isNotNull()) parentWidth else 0.dp
        val height = if (parentHeight.isNotNull()) parentHeight else 0.dp
        return VisualOffset(
            x = width * x,
            y = height * y
        )
    }
}
