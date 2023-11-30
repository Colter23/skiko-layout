package top.colter.skiko.data

import org.jetbrains.skia.Color
import org.jetbrains.skia.Image


/**
 * 背景
 *
 * [color] 背景颜色
 *
 * [gradient] 背景渐变 (角度暂未实现)
 *
 * [image] 背景图片 (暂未实现)
 *
 */
public data class Background(
    val color: Int = Color.WHITE,
    val gradient: Gradient? = null,
    val image: Image? = null,
)

public data class Gradient(
    val deg: Int = 45,
    val colors: List<Int>,
    val positions: List<Float>? = null
)
