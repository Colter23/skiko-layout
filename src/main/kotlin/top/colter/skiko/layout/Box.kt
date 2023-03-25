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
 * 最好指定宽高 [Modifier.width] / [Modifier.fillMaxWidth] / [Modifier.height] / [Modifier.fillMaxHeight]
 */
inline fun Layout.Box(
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

class BoxLayout(
    val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout?
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        preMeasure()

        if (child.isNotEmpty()) {
            if (deep) child.forEach { it.measure(true) }

            if (width.isNull() && !modifier.fillMaxWidth && !modifier.fillWidth) width =
                child.maxWidth() + modifier.padding.horizontal
            if (height.isNull() && !modifier.fillMaxHeight && !modifier.fillHeight) height =
                child.maxHeight() + modifier.padding.vertical
        }
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)

        for (layout in child) {
            layout.place(
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
        drawBgBox(canvas)
        super.draw(canvas)
    }

}
