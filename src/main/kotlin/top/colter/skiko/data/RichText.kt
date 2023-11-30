package top.colter.skiko.data

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.TextStyle


/**
 * ## 富文本元素
 *
 * [Text] 文本
 *
 * [Emoji] Emoji
 *
 */
public sealed class RichText {
    public data class Text(
        val value: String,
        val style: TextStyle? = null,
    ) : RichText()

    public data class Emoji(
        val value: String,
        val img: Image,
        val style: TextStyle? = null,
    ) : RichText()
}