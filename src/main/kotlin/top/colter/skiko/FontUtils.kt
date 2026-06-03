package top.colter.skiko

import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyleSet
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TextStyle
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import java.util.Locale

/**
 * 单次绘图或一棵布局树的字体上下文。
 *
 * 它持有独立的字体提供器和 [FontCollection]。布局库中的 Text/RichText 会通过它统一解析
 * 正文字体，业务层只需要在根 View 或根 Layout 上传入同一个 [FontRegistry]。
 */
public class FontRegistry {
    private val systemFontMgr: FontMgr = FontMgr.default
    private val fontProvider: TypefaceFontProvider = TypefaceFontProvider()

    /**
     * Skia Paragraph 使用的字体集合。自定义字体先注册到 [fontProvider]，未命中时再交给系统字体。
     */
    public val fonts: FontCollection = FontCollection()
        .setAssetFontManager(fontProvider)
        .setDynamicFontManager(systemFontMgr)
        .setDefaultFontManager(systemFontMgr)

    /**
     * 默认正文字体。Text/RichText 遇到空字体族或通用字体族时会优先使用它。
     */
    public var textTypeface: Typeface? = null
        set(value) {
            field = value
            if (value == null) {
                fonts.setDefaultFontManager(systemFontMgr)
            } else {
                registerTypeface(value, DEFAULT_TEXT_ALIASES)
                fonts.setDefaultFontManager(fontProvider, TEXT_FAMILY)
            }
        }

    /**
     * 默认 Emoji 字体。业务层可显式使用它，布局库中的自定义 Emoji 图片也会用它估算尺寸。
     */
    public var emojiTypeface: Typeface? = null
        set(value) {
            field = value
            if (value != null) registerTypeface(value, DEFAULT_EMOJI_ALIASES)
        }

    /**
     * 按 family 查询字体，优先匹配当前 registry 中注册过的字体，再查询系统字体。
     */
    public fun matchFamily(familyName: String): FontStyleSet {
        val custom = fontProvider.matchFamily(familyName)
        return if (custom.count() > 0) custom else systemFontMgr.matchFamily(familyName)
    }

    /**
     * 从文件加载并设置默认正文字体。TTC/OTC 集合可通过 [index] 选择字体。
     */
    public fun loadTextTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromFile(path, index)
        if (face != null) registerTextTypeface(face, alias)
        return face
    }

    /**
     * 从文件加载并设置默认 Emoji 字体。TTC/OTC 集合可通过 [index] 选择字体。
     */
    public fun loadEmojiTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromFile(path, index)
        if (face != null) registerEmojiTypeface(face, alias)
        return face
    }

    /**
     * 从内存数据加载并设置默认正文字体。
     */
    public fun loadTextTypeface(data: Data, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromData(data, index)
        if (face != null) registerTextTypeface(face, alias)
        return face
    }

    /**
     * 从内存数据加载并设置默认 Emoji 字体。
     */
    public fun loadEmojiTypeface(data: Data, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromData(data, index)
        if (face != null) registerEmojiTypeface(face, alias)
        return face
    }

    /**
     * 注册字体但不改变默认正文字体或 Emoji 字体。
     */
    public fun registerTypeface(typeface: Typeface, alias: String? = null): Typeface {
        registerTypeface(typeface, alias.asAliasList())
        return typeface
    }

    /**
     * 注册并设置默认正文字体。
     */
    public fun registerTextTypeface(typeface: Typeface, alias: String? = null): Typeface {
        textTypeface = typeface
        if (!alias.isNullOrBlank()) registerTypeface(typeface, alias)
        return typeface
    }

    /**
     * 注册并设置默认 Emoji 字体。
     */
    public fun registerEmojiTypeface(typeface: Typeface, alias: String? = null): Typeface {
        emojiTypeface = typeface
        if (!alias.isNullOrBlank()) registerTypeface(typeface, alias)
        return typeface
    }

    /**
     * 为 Paragraph/TextLine 解析实际使用的正文字体。
     *
     * 空字体族以及 Skiko 在 Linux 上默认填入的 sans-serif 等通用字体族会被视为未显式指定。
     */
    public fun resolveTextTypeface(style: TextStyle, fallbackStyle: TextStyle? = null): Typeface? {
        style.typeface?.let { return it }
        explicitFamily(style.fontFamilies)?.let { family ->
            matchFamily(family).firstTypeface()?.let { return it }
        }
        fallbackStyle?.typeface?.let { return it }
        fallbackStyle?.let {
            explicitFamily(it.fontFamilies)?.let { family ->
                matchFamily(family).firstTypeface()?.let { face -> return face }
            }
        }
        return textTypeface
    }

    /**
     * 返回带有已解析字体信息的样式副本，不修改传入的 [style]。
     */
    public fun resolveTextStyle(style: TextStyle, fallbackStyle: TextStyle? = null): TextStyle {
        val result = style.copyStyle()
        val typeface = resolveTextTypeface(style, fallbackStyle) ?: return result
        result.setTypeface(typeface)
        result.fontFamilies = resolvedFamilies(style, fallbackStyle, typeface)
        return result
    }

    /**
     * 将 [TextStyle] 解析为 Skia [Font]，用于 TextLine 测量。
     */
    public fun resolveTextFont(
        style: TextStyle,
        fallbackStyle: TextStyle? = null,
        defaultSize: Float = DEFAULT_FONT_SIZE,
    ): Font {
        val size = style.fontSize.takeIf { it > 0f }
            ?: fallbackStyle?.fontSize?.takeIf { it > 0f }
            ?: defaultSize
        return Font(resolveTextTypeface(style, fallbackStyle), size)
    }

    private fun registerTypeface(typeface: Typeface, aliases: Iterable<String>) {
        fontProvider.registerTypeface(typeface)
        typeface.familyName.takeIf { it.isNotBlank() }?.let {
            fontProvider.registerTypeface(typeface, it)
        }
        aliases.filter { it.isNotBlank() }.distinct().forEach {
            fontProvider.registerTypeface(typeface, it)
        }
    }

    private fun resolvedFamilies(
        style: TextStyle,
        fallbackStyle: TextStyle?,
        typeface: Typeface,
    ): Array<String> {
        style.typeface?.let {
            return explicitFamily(style.fontFamilies)
                ?.let { arrayOf(it) }
                ?: arrayOf(typeface.familyName)
        }
        explicitFamily(style.fontFamilies)?.let { return arrayOf(it) }
        fallbackStyle?.typeface?.let {
            return explicitFamily(fallbackStyle.fontFamilies)
                ?.let { arrayOf(it) }
                ?: arrayOf(it.familyName)
        }
        explicitFamily(fallbackStyle?.fontFamilies ?: emptyArray())?.let { return arrayOf(it) }
        return if (typeface === textTypeface) arrayOf(TEXT_FAMILY) else arrayOf(typeface.familyName)
    }

    private fun explicitFamily(families: Array<String>): String? {
        return families.firstOrNull { family ->
            family.isNotBlank() && family.lowercase(Locale.ROOT) !in GENERIC_FONT_FAMILIES
        }
    }

    private fun FontStyleSet.firstTypeface(): Typeface? {
        return if (count() > 0) getTypeface(0) else null
    }

    private fun String?.asAliasList(): List<String> {
        return if (isNullOrBlank()) emptyList() else listOf(this)
    }

    public companion object {
        /** 布局库内部使用的默认正文字体族别名。 */
        public const val TEXT_FAMILY: String = "skiko-layout-text"

        /** 布局库内部使用的默认 Emoji 字体族别名。 */
        public const val EMOJI_FAMILY: String = "skiko-layout-emoji"

        private const val DEFAULT_FONT_SIZE: Float = 14f

        private val DEFAULT_TEXT_ALIASES: List<String> = listOf(
            TEXT_FAMILY,
            "sans-serif",
        )

        private val DEFAULT_EMOJI_ALIASES: List<String> = listOf(
            EMOJI_FAMILY,
            "emoji",
        )

        private val GENERIC_FONT_FAMILIES: Set<String> = setOf(
            "default",
            "sans",
            "sans-serif",
            "serif",
            "monospace",
            "system-ui",
        )
    }
}

/** 默认全局字体上下文。简单场景可以直接使用，复杂场景建议显式创建并传递 [FontRegistry]。 */
public object Fonts {
    public val default: FontRegistry = FontRegistry()
}

/** 复制常用文本样式字段，避免复用同一个 [TextStyle] 时后续修改污染前面的富文本节点。 */
public fun TextStyle.copyStyle(): TextStyle {
    val result = TextStyle()
    result.color = color
    result.fontSize = fontSize
    result.fontStyle = fontStyle
    result.fontFamilies = fontFamilies.copyOf()
    result.letterSpacing = letterSpacing
    typeface?.let { result.setTypeface(it) }
    return result
}
