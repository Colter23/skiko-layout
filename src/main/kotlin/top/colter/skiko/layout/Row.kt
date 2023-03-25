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
inline fun Layout.Row(
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

class RowLayout(
    val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        preMeasure()

        if (child.isNotEmpty()) {
            if (deep) child.forEach { it.measure(true) }

            if (width.isNotNull()) {
                val sw = modifier.contentWidth - child.sumWidth() // modifier.contentWidth - w
                if (sw > 0.dp) {
                    child.filter { it.modifier.fillWidth }.ifNotEmpty {
                        forEach {
                            it.modifier.width = sw / size
                            it.measure(true)
                        }
                    }
                }
            }

            if (width.isNull()) width =
                child.sumWidth() + modifier.padding.horizontal // && child.none { it.width.isNull() }
            if (height.isNull()) height =
                child.maxHeight() + modifier.padding.vertical //  && child.none { it.height.isNull() }
        }
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)

        var x = 0.dp
        for (layout in child) {
            layout.place(
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
        drawBgBox(canvas)
        super.draw(canvas)
    }

}
