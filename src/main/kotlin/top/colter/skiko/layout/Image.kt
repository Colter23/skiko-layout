package top.colter.skiko.layout

import org.jetbrains.skia.*
import top.colter.skiko.*
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.place


/**
 * 图片元素
 *
 * [ratio] 宽高比 如: 16f/10f。 为 0 时表示保持图片原比例
 */
fun Layout.Image(
    image: Image,
    ratio: Float = 0f,
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    modifier: Modifier = Modifier(),
) {
    Layout(
        layout = ImageLayout(
            image = image,
            ratio = ratio,
            alignment = alignment,
            modifier = modifier,
            parentLayout = this
        ),
        content = {},
    )
}

class ImageLayout(
    val image: Image,
    val ratio: Float,
    val alignment: LayoutAlignment,
    modifier: Modifier,
    parentLayout: Layout,
) : Layout(modifier, parentLayout) {

    override fun measure(deep: Boolean) {
        preMeasure()

        val w = if (width.isNotNull()) width
        else if (!modifier.fillWidth) parentLayout!!.modifier.contentWidth
        else 0.dp

        if (w.isNotNull() && height.isNull()) {
            height = if (ratio != 0f) w / ratio else (image.height * w.px / image.width).toDp()
            if (width.isNull()) width = w
        }

        val h = if (height.isNotNull()) height
        else if (!modifier.fillHeight) parentLayout!!.modifier.contentHeight
        else 0.dp

        if (h.isNotNull() && width.isNull()) {
            width = if (ratio != 0f) h * ratio else (image.width * h.px / image.height).toDp()
            if (height.isNull()) height = h
        }

    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, modifier, bounds)
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas) {
            drawImageClip(image, it)
        }
    }

}
