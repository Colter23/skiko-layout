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
import kotlin.math.max

/**
 * 字体排版校准设置。
 *
 * 默认关闭，不改变布局库原有排版行为。业务层可以在需要更稳定视觉表现的绘图场景中开启
 * [normalizeLineHeight]，让不同字体的自然行高向同一个目标体验靠拢。
 */
public data class FontTypographySettings(
    val normalizeLineHeight: Boolean = false,
    val targetLineHeight: Float = DEFAULT_TARGET_LINE_HEIGHT,
    val lineHeightScale: Float = 1f,
    val letterSpacingEm: Float = 0f,
) {
    init {
        require(targetLineHeight in 1.0f..2.0f) { "targetLineHeight 需要在 1.0 到 2.0 之间" }
        require(lineHeightScale in 0.75f..1.5f) { "lineHeightScale 需要在 0.75 到 1.5 之间" }
        require(letterSpacingEm in -0.08f..0.08f) { "letterSpacingEm 需要在 -0.08 到 0.08 之间" }
    }

    public companion object {
        public const val DEFAULT_TARGET_LINE_HEIGHT: Float = 1.34f

        public val DEFAULT: FontTypographySettings = FontTypographySettings()

        public val NORMALIZED: FontTypographySettings = FontTypographySettings(
            normalizeLineHeight = true,
        )
    }
}

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
     * 文本排版校准设置。默认关闭；开启后只影响通过本 registry 解析的 Text/RichText。
     */
    public var typographySettings: FontTypographySettings = FontTypographySettings.DEFAULT

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
     * 从文件加载普通文本缺字回退字体，但不改变默认正文字体。
     */
    public fun loadTextFallbackTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromFile(path, index)
        if (face != null) registerTextFallbackTypeface(face, alias)
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
     * 从内存数据加载普通文本缺字回退字体，但不改变默认正文字体。
     */
    public fun loadTextFallbackTypeface(data: Data, alias: String? = null, index: Int = 0): Typeface? {
        val face = systemFontMgr.makeFromData(data, index)
        if (face != null) registerTextFallbackTypeface(face, alias)
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
     * 注册普通文本缺字回退字体，但不改变默认正文字体。
     *
     * 这些字体会在默认正文字体或显式字体之后参与 Paragraph 字形回退，避免回退字体抢占主字体。
     */
    public fun registerTextFallbackTypeface(typeface: Typeface, alias: String? = null): Typeface {
        registerTypeface(typeface, listOfNotNull(TEXT_FALLBACK_FAMILY, alias?.takeIf { it.isNotBlank() }))
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
     *
     * 当 registry 已加载普通文本缺字回退或 Emoji 字体时，普通文本和显式字体会自动追加回退族，
     * 让普通缺字和 Unicode emoji 在 Paragraph/TextLayout 中尽量走对应字体渲染。
     */
    public fun resolveTextStyle(style: TextStyle, fallbackStyle: TextStyle? = null): TextStyle {
        val result = style.copyStyle()
        val typeface = resolveTextTypeface(style, fallbackStyle) ?: return result
        result.setTypeface(typeface)
        result.fontFamilies = resolvedFamilies(style, fallbackStyle, typeface)
        applyTypography(result, style, fallbackStyle, typeface)
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

    private fun applyTypography(
        result: TextStyle,
        style: TextStyle,
        fallbackStyle: TextStyle?,
        typeface: Typeface,
    ) {
        val settings = typographySettings
        val size = result.fontSize.takeIf { it > 0f }
            ?: style.fontSize.takeIf { it > 0f }
            ?: fallbackStyle?.fontSize?.takeIf { it > 0f }
            ?: DEFAULT_FONT_SIZE

        if (settings.normalizeLineHeight && style.height == null && fallbackStyle?.height == null) {
            result.height = normalizedLineHeight(typeface, size, settings)
        }

        val letterSpacing = settings.letterSpacingEm * size
        if (letterSpacing != 0f) {
            result.letterSpacing = result.letterSpacing + letterSpacing
        }
    }

    private fun normalizedLineHeight(
        typeface: Typeface,
        size: Float,
        settings: FontTypographySettings,
    ): Float {
        val natural = Font(typeface, size).metrics.height
            .takeIf { it.isFinite() && it > 0f }
            ?.let { it / size.coerceAtLeast(1f) }
            ?.takeIf { it.isFinite() && it > 0f }
            ?: settings.targetLineHeight
        val scale = settings.lineHeightScale
        val target = settings.targetLineHeight.coerceIn(MIN_NORMALIZED_LINE_HEIGHT, MAX_NORMALIZED_LINE_HEIGHT) * scale
        val min = MIN_NORMALIZED_LINE_HEIGHT * scale
        val max = MAX_NORMALIZED_LINE_HEIGHT * scale
        val correction = (target / natural).coerceIn(MAX_LINE_HEIGHT_COMPRESSION, MAX_LINE_HEIGHT_EXPANSION)
        return (natural * correction).coerceIn(min, max)
    }

    private fun registerTypeface(typeface: Typeface, aliases: Iterable<String>) {
        val aliasList = aliases.filter { it.isNotBlank() }.distinct()
        if (aliasList.any { it == TEXT_FALLBACK_FAMILY }) {
            hasTextFallbackTypeface = true
        }
        fontProvider.registerTypeface(typeface)
        typeface.familyName.takeIf { it.isNotBlank() }?.let {
            fontProvider.registerTypeface(typeface, it)
        }
        aliasList.forEach {
            fontProvider.registerTypeface(typeface, it)
        }
    }

    private fun resolvedFamilies(
        style: TextStyle,
        fallbackStyle: TextStyle?,
        typeface: Typeface,
    ): Array<String> {
        val styleFamily = explicitFamily(style.fontFamilies)
        val fallbackFamily = explicitFamily(fallbackStyle?.fontFamilies ?: emptyArray())
        val fallbackTypeface = fallbackStyle?.typeface
        val primary = when {
            style.typeface != null -> styleFamily ?: typeface.familyName
            styleFamily != null -> styleFamily
            fallbackTypeface != null -> fallbackFamily ?: fallbackTypeface.familyName
            fallbackFamily != null -> fallbackFamily
            typeface === textTypeface -> TEXT_FAMILY
            else -> typeface.familyName
        }
        return appendFallbackFamilies(primary, typeface)
    }

    private fun appendFallbackFamilies(primaryFamily: String, typeface: Typeface): Array<String> {
        val families = linkedSetOf(primaryFamily)
        val emojiTypeface = isEmojiTypeface(typeface)
        if (hasTextFallbackTypeface() && !emojiTypeface) {
            families += TEXT_FALLBACK_FAMILY
        }
        if (hasEmojiTypeface() && !emojiTypeface) {
            families += EMOJI_FAMILY
        }
        return families.toTypedArray()
    }

    private var hasTextFallbackTypeface: Boolean = false

    private fun hasTextFallbackTypeface(): Boolean = hasTextFallbackTypeface

    private fun hasEmojiTypeface(): Boolean = emojiTypeface != null

    private fun isEmojiTypeface(typeface: Typeface): Boolean {
        val loadedEmojiFamily = emojiTypeface?.familyName ?: return false
        return typeface === emojiTypeface || typeface.familyName.equals(loadedEmojiFamily, ignoreCase = true)
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

        /** 布局库内部使用的普通文本缺字回退字体族别名。 */
        public const val TEXT_FALLBACK_FAMILY: String = "skiko-layout-text-fallback"

        /** 布局库内部使用的默认 Emoji 字体族别名。 */
        public const val EMOJI_FAMILY: String = "skiko-layout-emoji"

        private const val DEFAULT_FONT_SIZE: Float = 14f
        private const val MIN_NORMALIZED_LINE_HEIGHT: Float = 1.16f
        private const val MAX_NORMALIZED_LINE_HEIGHT: Float = 1.58f
        private const val MAX_LINE_HEIGHT_COMPRESSION: Float = 0.86f
        private const val MAX_LINE_HEIGHT_EXPANSION: Float = 1.22f

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
    result.height = height
    result.topRatio = topRatio
    result.letterSpacing = letterSpacing
    result.baselineShift = baselineShift
    result.wordSpacing = wordSpacing
    foreground?.let { result.setForeground(it.makeClone()) }
    background?.let { result.setBackground(it.makeClone()) }
    shadows.forEach { result.addShadow(it) }
    typeface?.let { result.setTypeface(it) }
    return result
}

internal fun TextStyle.lineBoxClipOutset(fallbackStyle: TextStyle? = null): Float {
    height ?: fallbackStyle?.height ?: return 0f
    val size = fontSize.takeIf { it > 0f }
        ?: fallbackStyle?.fontSize?.takeIf { it > 0f }
        ?: DEFAULT_TEXT_CLIP_FONT_SIZE
    val lineHeight = height?.let { it * size }
        ?: fallbackStyle?.height?.let { it * size }
        ?: return 0f
    val naturalHeight = typeface
        ?.let { Font(it, size).metrics.height }
        ?.takeIf { it.isFinite() && it > 0f }
        ?: 0f
    val safety = (size * TEXT_CLIP_SAFETY_RATIO).coerceIn(
        MIN_TEXT_CLIP_SAFETY_OUTSET,
        MAX_TEXT_CLIP_SAFETY_OUTSET,
    )
    val metricOverflow = ((naturalHeight - lineHeight) / 2f).coerceAtLeast(0f)
    return max(metricOverflow, safety)
}

private const val DEFAULT_TEXT_CLIP_FONT_SIZE: Float = 14f
private const val TEXT_CLIP_SAFETY_RATIO: Float = 0.08f
private const val MIN_TEXT_CLIP_SAFETY_OUTSET: Float = 2f
private const val MAX_TEXT_CLIP_SAFETY_OUTSET: Float = 10f
