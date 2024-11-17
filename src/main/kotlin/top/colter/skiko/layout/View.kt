package top.colter.skiko.layout

import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import java.io.File


/**
 * ## 根布局
 *
 * @param fileStr 文件绝对路径
 * @param modifier 样式
 * @param alignment 对齐
 * @param content 子元素内容
 */
public inline fun View(
    fileStr: String,
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: BoxLayout.() -> Unit
) {
    View(File(fileStr), modifier, alignment, content)
}

/**
 * ## 根布局
 *
 * @param file 文件
 * @param modifier 样式
 * @param alignment 对齐
 * @param content 子元素内容
 */
public inline fun View(
    file: File,
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: BoxLayout.() -> Unit
) {
    val image = View(modifier, alignment, content)
    file.writeBytes(image.encodeToData()!!.bytes)
}

/**
 * ## 根布局
 *
 * @param modifier 样式
 * @param alignment 对齐
 * @param content 子元素内容
 *
 * @return [Image]
 */
public inline fun View(
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.TOP_LEFT,
    content: BoxLayout.() -> Unit
): Image {
    val layout = BoxLayout(
        alignment = alignment,
        modifier = modifier,
        parentLayout = null
    )
    layout.content()
    layout.measure(false)
    layout.place(
        LayoutBounds.makeXYWH(
            width = layout.width,
            height = layout.height
        )
    )

    val surface = Surface.makeRasterN32Premul(
        layout.width.px.toInt(),
        layout.height.px.toInt()
    ).apply {
        layout.draw(canvas)
    }

    return surface.makeImageSnapshot()
}
