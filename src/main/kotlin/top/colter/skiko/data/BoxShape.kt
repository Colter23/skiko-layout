package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * 盒子形状语义。
 *
 * 用于描述背景裁剪、圆角和圆形盒子的最终绘制外形，形状会在测量完成后按实际宽高解析。
 */
public sealed class BoxShape {
    /**
     * 矩形。
     *
     * 默认形状，不做额外圆角裁剪。
     */
    public data object Rectangle : BoxShape()

    /**
     * 固定圆角。
     *
     * 四个值顺序为左上、右上、右下、左下。
     */
    public data class Rounded(
        public val radius: List<Dp>
    ) : BoxShape() {
        init {
            require(radius.all { it >= 0.dp }) { "radius require >= 0" }
        }
    }

    /**
     * 相对圆角。
     *
     * 半径按最终盒子短边乘以 [ratio] 计算，适合做“胶囊”或自适应圆角。
     */
    public data class RelativeRounded(
        public val ratio: Float
    ) : BoxShape() {
        init {
            require(ratio in 0f..0.5f) { "radius ratio require in 0..0.5" }
        }
    }

    /**
     * 圆形。
     *
     * 按最终盒子的短边生成最大圆角，通常配合正方形布局使用。
     */
    public data object Circle : BoxShape()
}
