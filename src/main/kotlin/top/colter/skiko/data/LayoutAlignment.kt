package top.colter.skiko.data

import org.jetbrains.skia.paragraph.Alignment
import top.colter.skiko.Dp
import top.colter.skiko.Modifier
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.layout.LayoutPosition


/**
 * ## 布局对齐方式
 *
 * [TOP_LEFT] ↖
 * [TOP_CENTER] ↑
 * [TOP_RIGHT] ↗
 *
 * [CENTER_LEFT] ←
 * [CENTER] ·
 * [CENTER_RIGHT] →
 *
 * [BOTTOM_LEFT] ↙
 * [BOTTOM_CENTER] ↓
 * [BOTTOM_RIGHT] ↘
 */
public class LayoutAlignment private constructor(
    public val horizontal: AxisAlignment,
    public val vertical: AxisAlignment
) {
    public companion object {
        public val TOP_LEFT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.START)
        public val TOP_CENTER: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.START)
        public val TOP_RIGHT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.START)
        public val CENTER_LEFT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.CENTER)
        public val CENTER: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.CENTER)
        public val CENTER_RIGHT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.CENTER)
        public val BOTTOM_LEFT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.END)
        public val BOTTOM_CENTER: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.END)
        public val BOTTOM_RIGHT: LayoutAlignment = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.END)
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
        LayoutAlignment.TOP_LEFT,
        LayoutAlignment.CENTER_LEFT,
        LayoutAlignment.BOTTOM_LEFT -> Alignment.START

        LayoutAlignment.TOP_CENTER,
        LayoutAlignment.CENTER,
        LayoutAlignment.BOTTOM_CENTER -> Alignment.CENTER

        LayoutAlignment.TOP_RIGHT,
        LayoutAlignment.CENTER_RIGHT,
        LayoutAlignment.BOTTOM_RIGHT -> Alignment.END

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