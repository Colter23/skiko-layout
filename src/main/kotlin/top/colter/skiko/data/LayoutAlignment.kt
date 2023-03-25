package top.colter.skiko.data

import org.jetbrains.skia.paragraph.Alignment
import top.colter.skiko.Dp
import top.colter.skiko.Modifier
import top.colter.skiko.layout.LayoutBounds
import top.colter.skiko.layout.LayoutPosition


class LayoutAlignment private constructor(
    val horizontal: AxisAlignment,
    val vertical: AxisAlignment
) {
    companion object {
        val TOP_LEFT = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.START)
        val TOP_CENTER = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.START)
        val TOP_RIGHT = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.START)
        val CENTER_LEFT = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.CENTER)
        val CENTER = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.CENTER)
        val CENTER_RIGHT = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.CENTER)
        val BOTTOM_LEFT = LayoutAlignment(horizontal = AxisAlignment.START, vertical = AxisAlignment.END)
        val BOTTOM_CENTER = LayoutAlignment(horizontal = AxisAlignment.CENTER, vertical = AxisAlignment.END)
        val BOTTOM_RIGHT = LayoutAlignment(horizontal = AxisAlignment.END, vertical = AxisAlignment.END)
    }

}

enum class AxisAlignment {
    START,
    CENTER,
    END
}

//fun LayoutAlignment.toAlignment(): Alignment {
//    return when (this) {
//        LayoutAlignment.TOP_LEFT,
//        LayoutAlignment.CENTER_LEFT,
//        LayoutAlignment.BOTTOM_LEFT -> Alignment.START
//
//        LayoutAlignment.TOP_CENTER,
//        LayoutAlignment.CENTER,
//        LayoutAlignment.BOTTOM_CENTER -> Alignment.CENTER
//
//        LayoutAlignment.TOP_RIGHT,
//        LayoutAlignment.CENTER_RIGHT,
//        LayoutAlignment.BOTTOM_RIGHT -> Alignment.END
//
//        else -> Alignment.START
//    }
//}

fun AxisAlignment.align(isHorizontal: Boolean, width: Dp, height: Dp, modifier: Modifier, bounds: LayoutBounds): Dp {
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

fun LayoutAlignment.place(width: Dp, height: Dp, modifier: Modifier, bounds: LayoutBounds): LayoutPosition {
    return LayoutPosition(
        x = this.horizontal.align(true, width, height, modifier, bounds),
        y = this.vertical.align(false, width, height, modifier, bounds),
    )
}