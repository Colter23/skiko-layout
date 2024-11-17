package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * ## 边距
 *
 * @param top 上边距
 * @param right 右边距
 * @param bottom 下边距
 * @param left 左边距
 *
 * @property horizontal 水平边距
 * @property vertical 垂直边距
 */
public data class Edge(
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp
) {
    public val horizontal: Dp get() = left + right
    public val vertical: Dp get() = top + bottom

    public fun isEmpty(): Boolean = top == 0.dp && right == 0.dp && bottom == 0.dp && left == 0.dp
    public fun isNotEmpty(): Boolean = !isEmpty()
}