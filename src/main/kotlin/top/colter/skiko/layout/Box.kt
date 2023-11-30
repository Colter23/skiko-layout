package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 盒子布局
 *
 * 内部元素绝对定位
 *
 * 最好指定宽和高 [Modifier.width] / [Modifier.fillMaxWidth] / [Modifier.height] / [Modifier.fillMaxHeight]
 */
public inline fun Layout.Box(
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: BoxLayout.() -> Unit = {}
) {
    Layout(
        layout = BoxLayout(
            modifier = modifier,
            alignment = alignment,
            parentLayout = this
        ),
        content = content
    )
}

public class BoxLayout(
    public val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        if (child.isNotEmpty()) {
            // 重新计算子元素宽高
            if (deep) child.forEach { it.measure(true) }

            // 由子元素确定当前元素宽高
            if (width.isNull() && !modifier.fillMaxWidth && !modifier.fillWidth) width =
                child.maxWidth() + modifier.padding.horizontal
            if (height.isNull() && !modifier.fillMaxHeight && !modifier.fillHeight) height =
                child.maxHeight() + modifier.padding.vertical
        }
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)

        // 确定子元素位置
        for (layout in child) {
            layout.place(
                // 指定子元素最大边界
                LayoutBounds.makeXYWH(
                    left = position.x + modifier.padding.left,
                    top = position.y + modifier.padding.top,
                    width = contentWidth,
                    height = contentHeight
                )
            )
        }
    }

    override fun draw(canvas: Canvas) {
        // 绘制当前元素
        drawBgBox(canvas)
        // 绘制子元素
        super.draw(canvas)
    }

}
