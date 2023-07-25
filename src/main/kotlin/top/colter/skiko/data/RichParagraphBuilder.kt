package top.colter.skiko.data

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.TextStyle
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties


/**
 * 富文本构造器
 */
class RichParagraphBuilder private constructor(
    private val  defaultStyle: TextStyle,
    private val lines: MutableList<RichLine>,
    private var line: MutableList<RichText>,
) {
    constructor(defaultStyle: TextStyle): this(defaultStyle.clone(), mutableListOf(), mutableListOf())

    /**
     * 添加富文本
     */
    fun addRichText(text: RichText): RichParagraphBuilder {
        line.add(text)
        return this
    }

    /**
     * 添加文本、样式
     */
    fun addText(text: String, style: TextStyle? = null): RichParagraphBuilder {
        return addRichText(RichText.Text(text, style?.clone()))
    }

    /**
     * 添加emoji，可指定emoji图片
     */
    fun addEmoji(value: String, img: Image, style: TextStyle? = null): RichParagraphBuilder {
        return addRichText(RichText.Emoji(value, img, style?.clone()))
    }

    /**
     * 主动换行
     */
    fun wrap(): RichParagraphBuilder {
        lines.add(RichLine(line))
        line = mutableListOf()
        return this
    }

    /**
     * 构建
     */
    fun build(): RichParagraph {
        if (line.isNotEmpty()) wrap()
        return RichParagraph(defaultStyle, lines)
    }
}

/**
 * 深度克隆字体样式
 */
private fun TextStyle.clone(): TextStyle {
    val new = this::class.constructors.find { it.parameters.isEmpty() }!!.call()
    val newFun = new::class.declaredMemberFunctions
    this::class.declaredMemberProperties.forEach {
        if (!it.name.startsWith("_") && !it.name.startsWith("is")) {
            val funName = if (it.name == "fontFamilies") "setFontFamilies"
            else (if (it.name.last() == 's') "add" else "set") + it.name.replaceFirstChar { it.uppercaseChar() }
            newFun.find { it.name == funName }?.call(new, it.getter.call(this))
        }
    }
    return new
}
