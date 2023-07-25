package top.colter.skiko

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider


object FontUtils {

    private val fontMgr = FontMgr.default
    private val fontProvider = TypefaceFontProvider()
    val fonts = FontCollection().setDynamicFontManager(fontProvider).setDefaultFontManager(fontMgr)

    var defaultFont: Typeface? = null
    var emojiFont: Typeface? = null

    private fun registerTypeface(typeface: Typeface?, alias: String? = null) {
        fontProvider.registerTypeface(typeface)
        if (alias != null) fontProvider.registerTypeface(typeface, alias)
    }


    fun matchFamily(familyName: String): FontStyleSet {
        val fa = fontProvider.matchFamily(familyName)
        return if (fa.count() != 0) {
            fa
        } else {
            fontMgr.matchFamily(familyName)
        }
    }

    fun loadEmojiTypeface(path: String): Typeface {
        val face = Typeface.makeFromFile(path, 0)
        if (emojiFont == null) emojiFont = face
        registerTypeface(face, null)
        return face
    }

    fun loadTypeface(path: String, alias: String? = null, index: Int = 0): Typeface {
        val face = Typeface.makeFromFile(path, index)
        if (defaultFont == null) defaultFont = face
        registerTypeface(face, alias)
        return face
    }

    fun loadTypeface(data: Data, index: Int = 0): Typeface {
        val face = Typeface.makeFromData(data, index)
        registerTypeface(face)
        return face
    }

}