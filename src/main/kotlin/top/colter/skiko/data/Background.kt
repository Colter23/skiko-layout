package top.colter.skiko.data

import org.jetbrains.skia.Image


/**
 * 背景 (之后可能还会有大的变动)
 *
 * [color] 背景颜色
 *
 * [gradient] 背景渐变
 *
 * [image] 背景图片
 *
 */
public data class Background(
    val color: Int? = null,
    val gradient: Gradient? = null,
    val image: Image? = null
)

/**
 * 渐变色
 *
 * [start] 渐变起始点
 *
 * [end] 渐变终点
 *
 * [colors] 颜色列表
 *
 */
public data class Gradient(
    val start: LayoutAlignment,
    val end: LayoutAlignment,
    val colors: List<Int>
)
