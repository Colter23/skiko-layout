package top.colter.skiko.data

import org.jetbrains.skia.Color
import top.colter.skiko.Dp
import top.colter.skiko.dp


/**
 * 文字描边。
 *
 * @param width 描边宽度
 * @param color 描边颜色
 */
public data class TextStroke(
    val width: Dp,
    val color: Int = Color.BLACK,
) {
    init {
        require(width >= 0.dp) { "文字描边宽度需大于等于 0" }
    }
}

/**
 * 文字阴影。
 *
 * @param offsetX 横向偏移
 * @param offsetY 纵向偏移
 * @param blur 模糊半径，会作为 Skia Paragraph 的 blurSigma 使用
 * @param color 阴影颜色
 */
public data class TextShadow(
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val blur: Dp = 0.dp,
    val color: Int = Color.BLACK,
) {
    init {
        require(blur >= 0.dp) { "文字阴影模糊半径需大于等于 0" }
    }
}
