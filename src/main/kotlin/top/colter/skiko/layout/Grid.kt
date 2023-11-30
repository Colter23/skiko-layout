package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 宫格布局
 *
 * 子元素尽量不要指定宽高
 *
 * [itemModifier] 可以为每个子元素设置样式，会与子元素自身的样式合并
 *
 * [maxLineCount] 每行最多有几个元素
 *
 * [space] 元素之间的间隔，最好使用此属性设置间隔，不要使用 margin 和 padding
 */
public inline fun Layout.Grid(
    maxLineCount: Int = 3,
    space: Dp = 10.dp,
    modifier: Modifier = Modifier(),
    itemModifier: Modifier? = null,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: GridLayout.() -> Unit
) {
    require(maxLineCount > 1) { "max line count require > 1" }

    Layout(
        layout = GridLayout(
            maxLineCount = maxLineCount,
            itemModifier = itemModifier,
            space = space,
            alignment = alignment,
            modifier = modifier,
            parentLayout = this
        ),
        content = content
    )
}

public class GridLayout(
    public val maxLineCount: Int,
    public val itemModifier: Modifier?,
    public val space: Dp,
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

            // 合并子元素样式
            if (itemModifier != null) {
                child.forEach {
                    it.modifier.merge(itemModifier)
                    it.measure(true)
                }
            }

            // 指定子元素宽高
            if (width.isNotNull() || height.isNotNull()) {
                val lineCount = if (child.size >= maxLineCount) maxLineCount else child.size
                val itemWidth: Dp
                val itemHeight: Dp
                if (width.isNotNull()) {
                    itemWidth = (contentWidth - space * (lineCount - 1)) / lineCount
                    itemHeight = if (lineCount > 1) itemWidth else child.first().height
                } else {
                    itemHeight = (contentHeight - space * (lineCount - 1)) / lineCount
                    itemWidth = if (lineCount > 1) itemHeight else child.first().width
                }
                child.forEach {
                    it.width = itemWidth
                    it.height = itemHeight
                    it.modifier.width(itemWidth).height(itemHeight)
                    it.measure(true)
                }
            }

            // 由子元素确定当前元素宽高
            if (width.isNull()) {
                width = modifier.padding.horizontal
                child.forEachIndexed { index, layout ->
                    if (index < maxLineCount) {
                        width += layout.width + layout.modifier.margin.horizontal + space
                    }
                }
                if (width.isNotNull()) width -= space
            }
            if (height.isNull()) {
                height = modifier.padding.vertical
                child.forEachIndexed { index, layout ->
                    if ((index + 1) % maxLineCount == 1) {
                        height += layout.height + layout.modifier.margin.vertical + space
                    }
                }
                if (height.isNotNull()) height -= space
            }
        }
    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)

        var x = 0.dp
        var y = 0.dp
        // 确定子元素位置
        child.forEachIndexed { index, layout ->
            layout.place(
                LayoutBounds.makeXYWH(
                    left = position.x + modifier.padding.left + x,
                    top = position.y + modifier.padding.top + y,
                    width = layout.boxWidth,
                    height = layout.boxHeight
                )
            )
            x += layout.width + space
            if ((index + 1) % maxLineCount == 0) {
                x = 0.dp
                y += layout.height + space
            }

        }
    }

    override fun draw(canvas: Canvas) {
        // 绘制当前元素
        drawBgBox(canvas)
        // 绘制子元素
        super.draw(canvas)
    }

}

