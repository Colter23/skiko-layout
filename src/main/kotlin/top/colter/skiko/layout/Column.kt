package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 列布局
 *
 * 内部元素会自动向下排列
 *
 * 最好指定宽度 [Modifier.width] / [Modifier.fillMaxWidth]
 */
public inline fun Layout.Column(
    modifier: Modifier = Modifier().fillMaxWidth(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: ColumnLayout.() -> Unit
) {
    Layout(
        layout = ColumnLayout(
            alignment = alignment,
            modifier = modifier,
            parentLayout = this
        ),
        content = content,
    )
}

public class ColumnLayout(
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

            // 指定子元素高度
            if (height.isNotNull()) {
                val sh = modifier.contentHeight - child.sumHeight()
                if (sh > 0.dp) {
                    child.filter { it.modifier.fillHeight }.ifNotEmpty {
                        forEach {
                            it.modifier.height = sh / size
                            it.measure(true)
                        }
                    }
                }
            }

            // 由子元素确定当前元素宽高
            if (width.isNull()) width = child.maxWidth() + modifier.padding.horizontal
            if (height.isNull()) height = child.sumHeight() + modifier.padding.vertical
        }
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)

        var y = 0.dp
        // 确定子元素位置
        for (layout in child) {
            layout.place(
                // 指定子元素最大边界
                LayoutBounds.makeXYWH(
                    left = position.x + modifier.padding.left,
                    top = position.y + modifier.padding.top + y,
                    width = contentWidth,
                    height = layout.boxHeight
                )
            )
            y += layout.boxHeight
        }
    }

    override fun draw(canvas: Canvas) {
        // 绘制当前元素
        drawBgBox(canvas)
        // 绘制子元素
        super.draw(canvas)
    }

}