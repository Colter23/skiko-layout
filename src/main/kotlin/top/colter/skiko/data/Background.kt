package top.colter.skiko.data

import org.jetbrains.skia.Image
import top.colter.skiko.Dp
import top.colter.skiko.dp


/**
 * 背景 (之后可能还会有大的变动)
 *
 * @param color 背景颜色
 * @param gradient 背景渐变
 * @param image 背景图片
 */
public data class Background(
    val color: Int? = null,
    val gradient: Gradient? = null,
    val image: Image? = null,
    val imageAlpha: Float = 1f,
    val imageGradientBlur: GradientBlur? = null,
    val imageBlur: Dp = 0.dp,
) {
    init {
        require(imageAlpha in 0f..1f) { "背景图片透明度需在 0..1 之间" }
        require(imageBlur >= 0.dp) { "背景图片模糊半径需大于等于 0" }
        require(imageBlur == 0.dp || imageGradientBlur == null) { "背景图片统一模糊和渐变模糊不能同时设置" }
    }
}

/**
 * 渐变色
 *
 * @param start 渐变起始点
 * @param end 渐变终点
 * @param colors 颜色列表
 */
public data class Gradient(
    val start: LayoutAlignment,
    val end: LayoutAlignment,
    val colors: List<Int>
)
