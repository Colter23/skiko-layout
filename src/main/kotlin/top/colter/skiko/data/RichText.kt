package top.colter.skiko.data

import org.jetbrains.skia.Image
import org.jetbrains.skia.paragraph.TextStyle


/**
 * 富文本元素
 */
sealed class RichText {
    data class Text(
        val value: String,
        val style: TextStyle? = null,
    ) : RichText()

    data class Emoji(
        val value: String,
        val img: Image,
        val style: TextStyle? = null,
    ) : RichText()
}