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
inline fun Layout.Column(
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

class ColumnLayout(
    val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        preMeasure()

        if (child.isNotEmpty()) {
            if (deep) child.forEach { it.measure(true) }

            if (height.isNotNull()) {
                val sh = modifier.contentHeight - child.sumHeight() // modifier.contentHeight
                if (sh > 0.dp) {
                    child.filter { it.modifier.fillHeight }.ifNotEmpty {
                        forEach {
                            it.modifier.height = sh / size
                            it.measure(true)
                        }
                    }
                }
            }

            if (width.isNull()) width = child.maxWidth() + modifier.padding.horizontal
            if (height.isNull()) height = child.sumHeight() + modifier.padding.vertical
        }
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)

        var y = 0.dp
        for (layout in child) {
            layout.place(
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
        drawBgBox(canvas)
        super.draw(canvas)
    }

}