package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * ## 画板
 *
 * 最好指定宽和高 [Modifier.width] / [Modifier.fillMaxWidth] / [Modifier.height] / [Modifier.fillMaxHeight]
 */
public fun Layout.Canvas(
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    canvasContent: Canvas.(Rect) -> Unit = {}
) {
    Layout(
        layout = CanvasLayout(
            modifier = modifier,
            alignment = alignment,
            parentLayout = this,
            canvasContent = canvasContent
        ),
        content = {}
    )
}

public class CanvasLayout(
    public val alignment: LayoutAlignment,
    public val canvasContent: Canvas.(Rect) -> Unit,
    modifier: Modifier,
    parentLayout: Layout
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()
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
        drawBgBox(canvas) {
            canvasContent(canvas, it)
        }
    }

}
