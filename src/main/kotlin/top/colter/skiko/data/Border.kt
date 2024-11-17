package top.colter.skiko.data

import org.jetbrains.skia.Color
import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * ## 边框
 *
 * @param width 宽度
 * @param radius 四角弧度 顺序为左上开始顺时针 左上↖ 右上↗ 右下↘ 左下↙
 * @param color 颜色
 */
public data class Border(
    val width: Dp = 0.dp,
    val radius: List<Dp> = listOf(0.dp, 0.dp, 0.dp, 0.dp),
    val color: Int = Color.WHITE
)