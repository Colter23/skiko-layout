package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * ## 比例视觉外扩
 *
 * 按父级可用内容区解析为实际 [Edge]。正数表示向对应方向额外绘制，
 * 只影响视觉矩形，不参与父级测量和兄弟元素排布。
 *
 * @param top 上侧外扩比例，要求 >= 0
 * @param right 右侧外扩比例，要求 >= 0
 * @param bottom 下侧外扩比例，要求 >= 0
 * @param left 左侧外扩比例，要求 >= 0
 */
public data class BleedRatio(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
) {
    init {
        require(top >= 0f) { "top bleed ratio require >= 0" }
        require(right >= 0f) { "right bleed ratio require >= 0" }
        require(bottom >= 0f) { "bottom bleed ratio require >= 0" }
        require(left >= 0f) { "left bleed ratio require >= 0" }
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
