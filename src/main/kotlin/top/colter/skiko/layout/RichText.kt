package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import top.colter.skiko.Modifier
import top.colter.skiko.data.*
import top.colter.skiko.toDp


/**
 * ## 富文本
 *
 * [paragraph] 文章 使用 [RichParagraphBuilder] 富文本构造器构建
 *
 * [maxLinesCount] 最大行数 默认 50 行
 */
public fun Layout.RichText(
    paragraph: RichParagraph,
    maxLinesCount: Int = 50,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    modifier: Modifier = Modifier(),
) {
    Layout(
        layout = RichTextLayout(
            paragraph = paragraph,
            alignment = alignment,
            maxLinesCount = maxLinesCount,
            modifier = modifier,
            parentLayout = this
        ),
        content = {},
    )
}

public class RichTextLayout(
    public val paragraph: RichParagraph,
    public val alignment: LayoutAlignment,
    public val maxLinesCount: Int,
    modifier: Modifier,
    parentLayout: Layout
) : Layout(modifier, parentLayout) {

    private var layoutParagraph: RichParagraph? = null

    override fun measure(deep: Boolean) {
        // 第一遍计算宽高
        preMeasure()

        // 确定宽度
        val maxWidth = if (modifier.width.isNotNull()) modifier.contentWidth.px
        else if (modifier.maxWidth.isNotNull()) modifier.maxWidth.px
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth.px - modifier.margin.horizontal.px
        else 0f

        // 进行布局 确定高度
        if (layoutParagraph == null && maxWidth != 0f) {
            layoutParagraph = paragraph.layout(maxWidth)
            if (height.isNull()) height = layoutParagraph!!.height.toDp()
        }

    }

    override fun place(bounds: LayoutBounds) {
        // 确定当前元素位置
        position = alignment.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        // 绘制文章
        layoutParagraph?.print(canvas, position.x.px, position.y.px)
    }

}
