package top.colter.skiko.data

import org.jetbrains.skia.paragraph.Alignment
import top.colter.skiko.Dp
import top.colter.skiko.Modifier
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.layout.LayoutPosition


/**
 * ## 布局对齐方式
 *
 * [LEFT_TOP] ↖
 * [LEFT] ←
 * [LEFT_BOTTOM] ↙
 *
 * [TOP] ↑
 * [CENTER] ·
 * [BOTTOM] ↓
 *
 * [RIGHT_TOP] ↗
 * [RIGHT] →
 * [RIGHT_BOTTOM] ↘
 */
public class LayoutAlignment private constructor(
    public val horizontal: AxisAlignment,
    public val vertical: AxisAlignment
) {
    public companion object {
        public val LEFT_TOP: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.START)
        public val LEFT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.CENTER)
        public val LEFT_BOTTOM: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.END)
        public val TOP: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.START)
        public val CENTER: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.CENTER)
        public val BOTTOM: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.END)
        public val RIGHT_TOP: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.START)
        public val RIGHT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.CENTER)
        public val RIGHT_BOTTOM: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.END)

        /**
         * 默认对其 左上
         */
        public val DEFAULT: LayoutAlignment = LEFT_TOP
    }

}

/**
 * ## 对齐方式
 *
 * [START] 左对齐
 *
 * [CENTER] 居中
 *
 * [END] 右对齐
 */
public enum class AxisAlignment {
    START,
    CENTER,
    END
}

public fun LayoutAlignment.toAlignment(): Alignment {
    return when (this) {
        LayoutAlignment.LEFT_TOP,
        LayoutAlignment.LEFT,
        LayoutAlignment.LEFT_BOTTOM -> Alignment.START

        LayoutAlignment.TOP,
        LayoutAlignment.CENTER,
        LayoutAlignment.BOTTOM -> Alignment.CENTER

        LayoutAlignment.RIGHT_TOP,
        LayoutAlignment.RIGHT,
        LayoutAlignment.RIGHT_BOTTOM -> Alignment.END

        else -> Alignment.START
    }
}

public fun AxisAlignment.align(isHorizontal: Boolean, width: Dp, height: Dp, modifier: Modifier, bounds: LayoutBounds): Dp {
    val start = if (isHorizontal) bounds.left else bounds.top
    val boundsWidth = if (isHorizontal) bounds.width else bounds.height
    val w = if (isHorizontal) width else height
    val end = if (isHorizontal) bounds.right else bounds.bottom
    val marginStart = if (isHorizontal) modifier.margin.left else modifier.margin.top
    val marginEnd = if (isHorizontal) modifier.margin.right else modifier.margin.bottom

    return when (this) {
        AxisAlignment.START -> start + marginStart
        AxisAlignment.CENTER -> start + boundsWidth / 2 - w / 2
        AxisAlignment.END -> end - w - marginEnd
    }
}

public fun LayoutAlignment.place(width: Dp, height: Dp, modifier: Modifier, bounds: LayoutBounds): LayoutPosition {
    return LayoutPosition(
        x = this.horizontal.align(true, width, height, modifier, bounds),
        y = this.vertical.align(false, width, height, modifier, bounds),
    )
}