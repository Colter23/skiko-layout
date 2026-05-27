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
 *
 * @param modifier 样式
 * @param alignment 对齐
 * @param content 子元素内容
 */
public inline fun Layout.Row(
    modifier: Modifier = Modifier().fillMaxHeight(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
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
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
) : Layout(modifier, parentLayout, fontRegistry) {

    private fun measureChildren(deep: Boolean) {
        if (!deep) return

        if (width.isNull()) {
            child.forEach { it.measure(true) }
            return
        }

        child.forEach { it.preMeasure() }

        val fillChildren = child.filter { it.modifier.fillWidth }
        val fixedChildren = child.filter { !it.modifier.fillWidth }

        fixedChildren.forEach { it.measure(true) }

        fillChildren.ifNotEmpty {
            val fixedWidth = fixedChildren.sumWidth()
            val fillMargin = sumOf { resolvedMargin.horizontal }
            val remainingWidth = (paintContentWidth - fixedWidth - fillMargin).coerceAtLeast(0.dp)
            val itemWidth = remainingWidth / size

            forEach {
                it.width = itemWidth
                it.measure(true)
            }
        }
    }

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        if (child.isNotEmpty()) {
            // 重新计算子元素宽高
            measureChildren(deep)

            // 由子元素确定当前元素宽高
            if (width.isNull()) width = child.sumWidth() + resolvedPadding.horizontal
            if (height.isNull()) height = child.maxHeight() + resolvedPadding.vertical
        }
        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()

        var x = 0.dp
        // 确定子元素位置
        for (layout in child) {
            layout.place(
                // 指定子元素最大边界
                LayoutBounds.makeXYWH(
                    left = paintX + resolvedPadding.left + x,
                    top = paintY + resolvedPadding.top,
                    width = layout.boxWidth,
                    height = paintContentHeight
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
