package top.colter.skiko.data

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.TextStyle
import top.colter.skiko.copyStyle


/**
 * ## 富文本构造器
 *
 * [defaultStyle] 设置默认样式
 *
 * ### 方法
 * [addRichText] 添加富文本 [RichText]
 *
 * [addText] 添加文本、样式
 *
 * [addEmoji] 添加emoji，可指定emoji图片
 *
 * [wrap] 主动换行
 *
 * [build] 构建
 *
 * ### 例子
 * ```kotlin
 * val style = TextStyle().setColor(Color.BLACK)
 * val paragraph = RichParagraphBuilder(style)
 *             .addText("文字混排测试")
 *             .addText("自定义文字样式", style.setColor(Color.RED))
 *             .wrap()
 *             .addEmoji("[emoji]", emojiImg)
 *             .addText("😍❤️", style.setFontFamily(Fonts.default.emojiTypeface!!.familyName))
 *             .build()
 * ```
 *
 */
public class RichParagraphBuilder private constructor(
    private val  defaultStyle: TextStyle,
    private val lines: MutableList<RichLine>,
    private var line: MutableList<RichText>,
) {
    public constructor(defaultStyle: TextStyle): this(defaultStyle.copyStyle(), mutableListOf(), mutableListOf())

    /**
     * 添加富文本
     */
    public fun addRichText(text: RichText): RichParagraphBuilder {
        line.add(text)
        return this
    }

    /**
     * 添加文本、样式
     */
    public fun addText(text: String, style: TextStyle? = null): RichParagraphBuilder {
        return addRichText(RichText.Text(text, style?.copyStyle()))
    }

    /**
     * 添加emoji，可指定emoji图片
     */
    public fun addEmoji(value: String, img: Image, style: TextStyle? = null): RichParagraphBuilder {
        return addRichText(RichText.Emoji(value, img, style?.copyStyle()))
    }

    /**
     * 主动换行
     */
    public fun wrap(): RichParagraphBuilder {
        lines.add(RichLine(line))
        line = mutableListOf()
        return this
    }

    /**
     * 构建
     */
    public fun build(): RichParagraph {
        if (line.isNotEmpty()) wrap()
        return RichParagraph(defaultStyle, lines)
    }
}
