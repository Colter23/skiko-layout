package top.colter.skiko

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider


public object FontUtils {

    private val fontMgr = FontMgr.default
    private val fontProvider = TypefaceFontProvider()
    public val fonts: FontCollection = FontCollection().setDynamicFontManager(fontProvider).setDefaultFontManager(fontMgr)

    public var defaultFont: Typeface? = null
    public var emojiFont: Typeface? = null

    private fun registerTypeface(typeface: Typeface?, alias: String? = null) {
        if (defaultFont == null) defaultFont = typeface
        fontProvider.registerTypeface(typeface)
        if (alias != null) fontProvider.registerTypeface(typeface, alias)
    }


    public fun matchFamily(familyName: String): FontStyleSet {
        val fa = fontProvider.matchFamily(familyName)
        return if (fa.count() != 0) {
            fa
        } else {
            fontMgr.matchFamily(familyName)
        }
    }

    public fun loadEmojiTypeface(path: String): Typeface {
        val face = Typeface.makeFromFile(path, 0)
        if (emojiFont == null) emojiFont = face
        registerTypeface(face, null)
        return face
    }

    public fun loadTypeface(path: String, alias: String? = null, index: Int = 0): Typeface {
        val face = Typeface.makeFromFile(path, index)
        registerTypeface(face, alias)
        return face
    }

    public fun loadTypeface(data: Data, index: Int = 0): Typeface {
        val face = Typeface.makeFromData(data, index)
        registerTypeface(face)
        return face
    }

    public fun loadTypeface(typeface: Typeface): Typeface {
        registerTypeface(typeface)
        return typeface
    }

}