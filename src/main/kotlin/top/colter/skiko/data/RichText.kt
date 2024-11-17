package top.colter.skiko.data

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.TextStyle


/**
 * ## 富文本元素
 *
 * 请使用下面具体的元素
 *
 * [Text] 文本
 *
 * [Emoji] Emoji
 *
 */
public sealed class RichText {
    /**
     * 文本元素
     *
     * @param value 文本内容
     * @param style 文本样式
     */
    public data class Text(
        val value: String,
        val style: TextStyle? = null,
    ) : RichText()

    /**
     * Emoji元素
     *
     * @param value Emoji文本
     * @param img Emoji图片
     * @param style 文本样式
     */
    public data class Emoji(
        val value: String,
        val img: Image,
        val style: TextStyle? = null,
    ) : RichText()
}