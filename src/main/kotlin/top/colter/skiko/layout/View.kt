package top.colter.skiko.layout

import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import top.colter.skiko.FontRegistry
import top.colter.skiko.Fonts
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import java.io.File


/**
 * ## 根布局
 *
 * @param fileStr 文件绝对路径
 * @param modifier 样式
 * @param alignment 对齐
 * @param fontRegistry 字体上下文，控制本次绘图使用的字体集合、默认字体和 Emoji 字体
 * @param content 子元素内容
 */
public inline fun View(
    fileStr: String,
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    fontRegistry: FontRegistry = Fonts.default,
    content: BoxLayout.() -> Unit
) {
    View(File(fileStr), modifier, alignment, fontRegistry, content)
}

/**
 * ## 根布局
 *
 * @param file 文件
 * @param modifier 样式
 * @param alignment 对齐
 * @param fontRegistry 字体上下文，控制本次绘图使用的字体集合、默认字体和 Emoji 字体
 * @param content 子元素内容
 */
public inline fun View(
    file: File,
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    fontRegistry: FontRegistry = Fonts.default,
    content: BoxLayout.() -> Unit
) {
    val image = View(modifier, alignment, fontRegistry, content)
    file.writeBytes(image.encodeToData()!!.bytes)
}

/**
 * ## 根布局
 *
 * @param modifier 样式
 * @param alignment 对齐
 * @param fontRegistry 字体上下文，控制本次绘图使用的字体集合、默认字体和 Emoji 字体
 * @param content 子元素内容
 *
 * @return [Image]
 */
public inline fun View(
    modifier: Modifier = Modifier(),
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    fontRegistry: FontRegistry = Fonts.default,
    content: BoxLayout.() -> Unit
): Image {
    val layout = BoxLayout(
        alignment = alignment,
        modifier = modifier,
        parentLayout = null,
        fontRegistry = fontRegistry,
    )
    layout.content()
    layout.measure(true)
    layout.place(
        LayoutBounds.makeXYWH(
            width = layout.width,
            height = layout.height
        )
    )

    val surface = Surface.makeRasterN32Premul(
        layout.width.px.toInt().coerceAtLeast(1),
        layout.height.px.toInt().coerceAtLeast(1)
    ).apply {
        layout.draw(canvas)
    }

    return surface.makeImageSnapshot()
}
