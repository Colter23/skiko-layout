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
 *
 * @param modifier 样式
 * @param alignment 对齐
 * @param content 子元素内容
 */
public inline fun Layout.Column(
    modifier: Modifier = Modifier().fillMaxWidth(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
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
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
) : Layout(modifier, parentLayout, fontRegistry) {

    private fun measureChildren(deep: Boolean) {
        if (!deep) return

        if (height.isNull()) {
            child.forEach { it.measure(true) }
            return
        }

        child.forEach { it.preMeasure() }

        val fillChildren = child.filter { it.modifier.fillHeight }
        val fixedChildren = child.filter { !it.modifier.fillHeight }

        fixedChildren.forEach { it.measure(true) }

        fillChildren.ifNotEmpty {
            val fixedHeight = fixedChildren.sumHeight()
            val fillMargin = sumOf { resolvedMargin.vertical }
            val remainingHeight = (paintContentHeight - fixedHeight - fillMargin).coerceAtLeast(0.dp)
            val itemHeight = remainingHeight / size

            forEach {
                it.height = itemHeight
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
            if (width.isNull()) width = child.maxWidth() + resolvedPadding.horizontal
            if (height.isNull()) height = child.sumHeight() + resolvedPadding.vertical
        }
        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()

        var y = 0.dp
        // 确定子元素位置
        for (layout in child) {
            layout.place(
                // 指定子元素最大边界
                LayoutBounds.makeXYWH(
                    left = paintX + resolvedPadding.left,
                    top = paintY + resolvedPadding.top + y,
                    width = paintContentWidth,
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
