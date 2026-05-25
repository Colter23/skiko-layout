package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * ## 比例边距
 *
 * 按父级内容区解析为实际 [Edge]。水平方向的 [left] / [right] 使用父级内容宽度，
 * 垂直方向的 [top] / [bottom] 使用父级内容高度。
 *
 * @param top 上边距比例，范围 0..1
 * @param right 右边距比例，范围 0..1
 * @param bottom 下边距比例，范围 0..1
 * @param left 左边距比例，范围 0..1
 */
public data class EdgeRatio(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
) {
    init {
        require(top in 0f..1f) { "top ratio require in 0..1" }
        require(right in 0f..1f) { "right ratio require in 0..1" }
        require(bottom in 0f..1f) { "bottom ratio require in 0..1" }
        require(left in 0f..1f) { "left ratio require in 0..1" }
    }

    public val horizontal: Float get() = left + right
    public val vertical: Float get() = top + bottom

    public fun isEmpty(): Boolean = top == 0f && right == 0f && bottom == 0f && left == 0f
    public fun isNotEmpty(): Boolean = !isEmpty()

    internal fun resolve(parentWidth: Dp, parentHeight: Dp): Edge {
        val width = if (parentWidth.isNotNull()) parentWidth else 0.dp
        val height = if (parentHeight.isNotNull()) parentHeight else 0.dp
        return Edge(
            top = height * top,
            right = width * right,
            bottom = height * bottom,
            left = width * left
        )
    }
}
