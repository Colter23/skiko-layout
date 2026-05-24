package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 宫格布局
 *
 * 主要用于宫格图片
 *
 * 当子元素数量大于一，且 [lockRatio] 为true时，锁定宽高比例为1:1，子元素平分宽度
 *
 * 最好指定元素本身宽度，尽量不要指定子元素宽高
 *
 * 元素之间的间隔，最好使用 [space] 属性设置，不要使用 margin 和 padding
 *
 * @param maxLineCount 每行最多有几个元素
 * @param space 元素之间的间隔
 * @param lockRatio 是否锁定元素比例为1:1
 * @param modifier 样式
 * @param itemModifier 子元素样式
 * @param alignment 对齐
 * @param content 子元素内容
 */
public inline fun Layout.Grid(
    maxLineCount: Int = 3,
    space: Dp = 10.dp,
    lockRatio: Boolean = true,
    modifier: Modifier = Modifier(),
    itemModifier: Modifier? = null,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    content: GridLayout.() -> Unit
) {
    require(maxLineCount >= 1) { "max line count require >= 1" }

    Layout(
        layout = GridLayout(
            maxLineCount = maxLineCount,
            itemModifier = itemModifier,
            space = space,
            lockRatio = lockRatio,
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
    public val lockRatio: Boolean,
    public val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?
) : Layout(modifier, parentLayout) {

    private fun rows(): List<List<Layout>> = child.chunked(maxLineCount)

    private fun rowWidth(row: List<Layout>): Dp {
        if (row.isEmpty()) return 0.dp
        val cells = row.sumOf { boxWidth }
        val gaps = if (row.size > 1) space * (row.size - 1) else 0.dp
        return cells + gaps
    }

    private fun rowHeight(row: List<Layout>): Dp = row.maxOf { boxHeight }

    private fun squareSide(): Dp {
        if (child.isEmpty()) return 0.dp
        return child.maxOf { if (width > height) width else height }
    }

    private fun lockedItemSize(): Dp? {
        if (!lockRatio || child.isEmpty()) return null

        val rowCount = rows().size.coerceAtLeast(1)
        val columnCount = child.size.coerceAtMost(maxLineCount).coerceAtLeast(1)

        val widthLimit = if (width.isNotNull()) {
            ((contentWidth - space * (columnCount - 1)).coerceAtLeast(0.dp) / columnCount)
        } else null

        val heightLimit = if (height.isNotNull()) {
            ((contentHeight - space * (rowCount - 1)).coerceAtLeast(0.dp) / rowCount)
        } else null

        return when {
            widthLimit != null && heightLimit != null -> Dp.min(widthLimit, heightLimit)
            widthLimit != null -> widthLimit
            heightLimit != null -> heightLimit
            else -> squareSide()
        }
    }

    override fun measure(deep: Boolean) {
        preMeasure()

        if (child.isEmpty()) {
            finishMeasure()
            return
        }

        itemModifier?.let { extra ->
            child.forEach { it.modifier.merge(extra) }
        }

        if (deep) {
            child.forEach { it.measure(true) }
        }

        lockedItemSize()?.takeIf { it.isNotNull() && it > 0.dp }?.let { itemSize ->
            child.forEach {
                it.width = itemSize
                it.height = itemSize
                it.modifier.width(itemSize).height(itemSize)
                it.measure(true)
            }
        }

        val rowList = rows()
        val totalWidth = rowList.fold(0.dp) { acc, row -> Dp.max(acc, rowWidth(row)) }
        val totalHeight = rowList.fold(0.dp) { acc, row -> acc + rowHeight(row) } +
                if (rowList.size > 1) space * (rowList.size - 1) else 0.dp

        if (width.isNull()) width = totalWidth + modifier.padding.horizontal
        if (height.isNull()) height = totalHeight + modifier.padding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)

        var y = 0.dp
        rows().forEach { row ->
            var x = 0.dp
            val rowTop = position.y + modifier.padding.top + y
            val rowHeight = rowHeight(row)

            row.forEach { layout ->
                layout.place(
                    LayoutBounds.makeXYWH(
                        left = position.x + modifier.padding.left + x,
                        top = rowTop,
                        width = layout.boxWidth,
                        height = rowHeight
                    )
                )
                x += layout.boxWidth + space
            }

            y += rowHeight + space
        }
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas)
        super.draw(canvas)
    }
}
