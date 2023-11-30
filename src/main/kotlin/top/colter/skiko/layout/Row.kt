package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 行布局
 *
 * 内部元素会自动向右排列
 *
 * 最好指定高度 [Modifier.height] / [Modifier.fillMaxHeight]
 */
public inline fun Layout.Row(
    modifier: Modifier = Modifier().fillMaxHeight(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: RowLayout.() -> Unit
) {
    Layout(
        layout = RowLayout(
            alignment = alignment,
            modifier = modifier,
            parentLayout = this
        ),
        content = content,
    )
}

public class RowLayout(
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

            // 指定子元素宽度
            if (width.isNotNull()) {
                val sw = modifier.contentWidth - child.sumWidth()
                if (sw > 0.dp) {
                    child.filter { it.modifier.fillWidth }.ifNotEmpty {
                        forEach {
                            it.modifier.width = sw / size
                            it.measure(true)
                        }
                    }
                }
            }

            // 由子元素确定当前元素宽高
            if (width.isNull()) width = child.sumWidth() + modifier.padding.horizontal
            if (height.isNull()) height = child.maxHeight() + modifier.padding.vertical
        }
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)

        var x = 0.dp
        // 确定子元素位置
        for (layout in child) {
            layout.place(
                // 指定子元素最大边界
                LayoutBounds.makeXYWH(
                    left = position.x + modifier.padding.left + x,
                    top = position.y + modifier.padding.top,
                    width = layout.boxWidth,
                    height = contentHeight
                )
            )
            x += layout.boxWidth
        }
    }

    override fun draw(canvas: Canvas) {
        // 绘制当前元素
        drawBgBox(canvas)
        // 绘制子元素
        super.draw(canvas)
    }

}
