package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

data class Edge(
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp
) {
    val horizontal get() = left + right
    val vertical get() = top + bottom

    fun isEmpty(): Boolean = top == 0.dp && right == 0.dp && bottom == 0.dp && left == 0.dp
    fun isNotEmpty(): Boolean = !isEmpty()
}