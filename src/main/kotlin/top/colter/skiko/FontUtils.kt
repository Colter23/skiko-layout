package top.colter.skiko

import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import org.jetbrains.skia.paragraph.TextStyle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 字体初始化配置。
 *
 * family 会优先通过系统字体管理器匹配；fontFile 会直接从指定字体文件加载。
 * 两者同时存在时先使用 fontFile，并用 family 作为可选别名注册。
 */
public data class FontConfig(
    /** 默认正文字体族名，例如 "Noto Sans CJK SC"。 */
    val defaultFamily: String = "",
    /** 默认 Emoji 字体族名，例如 "Noto Color Emoji"。 */
    val emojiFamily: String = "",
    /** 默认正文字体文件路径，支持 ttf/ttc/otf。 */
    val defaultFontFile: String = "",
    /** 默认 Emoji 字体文件路径。 */
    val emojiFontFile: String = "",
)

/**
 * 单次绘图或一棵布局树的字体上下文。
 *
 * 它持有独立的 [FontCollection] 和自定义字体提供器，可以为不同布局套装、
 * 不同测试用例或不同渲染任务隔离字体注册，避免全部写入全局 [Fonts.default]。
 */
public class FontRegistry {
    private val fontMgr: FontMgr = FontMgr.default
    private val fontProvider: TypefaceFontProvider = TypefaceFontProvider()

    /** Skia Paragraph 使用的字体集合，包含自定义字体与系统默认字体管理器。 */
    public val fonts: FontCollection = FontCollection()
        .setDynamicFontManager(fontProvider)
        .setDefaultFontManager(fontMgr)

    /** 默认正文字体。Text 未指定字体族时会优先使用它。 */
    public var defaultFont: Typeface? = null
    /** 默认 Emoji 字体。主要供业务层或富文本样式显式引用。 */
    public var emojiFont: Typeface? = null

    /** 注册字体；首次注册的普通字体会成为默认正文字体。 */
    private fun registerTypeface(typeface: Typeface?, alias: String? = null) {
        if (typeface == null) return
        if (defaultFont == null) defaultFont = typeface
        fontProvider.registerTypeface(typeface)
        if (!alias.isNullOrBlank()) fontProvider.registerTypeface(typeface, alias)
    }

    /** 先从当前 registry 的自定义字体里查找，找不到再交给系统字体管理器。 */
    public fun matchFamily(familyName: String): FontStyleSet {
        val custom = fontProvider.matchFamily(familyName)
        return if (custom.count() != 0) custom else fontMgr.matchFamily(familyName)
    }

    /** 从文件加载 Emoji 字体，并在首次成功时记录为默认 Emoji 字体。 */
    public fun loadEmojiTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        val face = fontMgr.makeFromFile(path, index)
        if (face != null) {
            if (emojiFont == null) emojiFont = face
            registerTypeface(face, alias)
        }
        return face
    }

    /** 从文件加载普通字体。TTC 集合可通过 [index] 选择字体。 */
    public fun loadTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        val face = fontMgr.makeFromFile(path, index)
        if (face != null) registerTypeface(face, alias)
        return face
    }

    /** 从内存数据加载字体。 */
    public fun loadTypeface(data: Data, index: Int = 0): Typeface? {
        val face = fontMgr.makeFromData(data, index)
        if (face != null) registerTypeface(face)
        return face
    }

    /** 注册已经创建好的 [Typeface]，常用于系统 family 匹配后的回填。 */
    public fun loadTypeface(typeface: Typeface, alias: String? = null): Typeface {
        registerTypeface(typeface, alias)
        return typeface
    }

    /**
     * 确保默认字体存在。
     *
     * 顺序为：显式字体文件、显式 family、内置常见系统 family、内置常见系统路径。
     * 找不到时不会抛错，后续仍会交给 Skia/系统字体管理器处理。
     */
    public fun ensureDefaultFont(config: FontConfig = FontConfig()) {
        if (defaultFont == null) {
            loadConfiguredFont(config.defaultFontFile, config.defaultFamily, emoji = false)
                || loadConfiguredFamily(config.defaultFamily, emoji = false)
                || loadSystemFamily()
                || loadSystemFontFile()
        }
        if (emojiFont == null) {
            loadConfiguredFont(config.emojiFontFile, config.emojiFamily, emoji = true)
                || loadConfiguredFamily(config.emojiFamily, emoji = true)
                || loadSystemEmojiFont()
        }
    }

    private fun loadConfiguredFont(path: String, alias: String, emoji: Boolean): Boolean {
        if (path.isBlank()) return false
        val normalized = Paths.get(path).toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) return false
        return runCatching {
            val face = if (emoji) loadEmojiTypeface(normalized.toString(), alias.takeIf { it.isNotBlank() })
            else loadTypeface(normalized.toString(), alias.takeIf { it.isNotBlank() })
            face != null
        }.getOrDefault(false)
    }

    private fun loadConfiguredFamily(family: String, emoji: Boolean): Boolean {
        if (family.isBlank()) return false
        return loadFamily(family, emoji)
    }

    private fun loadSystemFamily(): Boolean {
        return DEFAULT_FONT_FAMILIES.any { loadFamily(it, emoji = false) }
    }

    private fun loadSystemEmojiFont(): Boolean {
        return DEFAULT_EMOJI_FONT_FAMILIES.any { loadFamily(it, emoji = true) } ||
            DEFAULT_EMOJI_FONT_PATHS.any { loadFontFile(it, emoji = true) }
    }

    private fun loadFamily(family: String, emoji: Boolean): Boolean {
        return runCatching {
            val fontSet = matchFamily(family)
            if (fontSet.count() <= 0) return@runCatching false
            val typeface = fontSet.getTypeface(0) ?: return@runCatching false
            loadTypeface(typeface, family)
            if (emoji && emojiFont == null) emojiFont = typeface
            true
        }.getOrDefault(false)
    }

    private fun loadSystemFontFile(): Boolean {
        return DEFAULT_FONT_PATHS.any { loadFontFile(it, emoji = false) }
    }

    private fun loadFontFile(path: Path, emoji: Boolean): Boolean {
        val normalized = path.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) return false
        return runCatching {
            val face = if (emoji) loadEmojiTypeface(normalized.toString()) else loadTypeface(normalized.toString())
            face != null
        }.getOrDefault(false)
    }

    /** 常见跨平台字体候选与兜底路径；只是覆盖主流环境，不等于完整字体发现。 */
    private companion object {
        private val DEFAULT_FONT_FAMILIES: List<String> = listOf(
            "Microsoft YaHei",
            "Microsoft YaHei UI",
            "SimHei",
            "SimSun",
            "PingFang SC",
            "Heiti SC",
            "Noto Sans CJK SC",
            "Noto Sans CJK",
            "Noto Sans",
            "WenQuanYi Micro Hei",
            "DejaVu Sans",
            "Segoe UI",
        )

        private val DEFAULT_EMOJI_FONT_FAMILIES: List<String> = listOf(
            "Segoe UI Emoji",
            "Apple Color Emoji",
            "Noto Color Emoji",
        )

        private val DEFAULT_FONT_PATHS: List<Path> = buildList {
            System.getenv("WINDIR")?.takeIf { it.isNotBlank() }?.let { windowsDir ->
                val fonts = Paths.get(windowsDir, "Fonts")
                add(fonts.resolve("msyh.ttc"))
                add(fonts.resolve("msyhbd.ttc"))
                add(fonts.resolve("simhei.ttf"))
                add(fonts.resolve("simsun.ttc"))
            }
            add(Paths.get("/System/Library/Fonts/PingFang.ttc"))
            add(Paths.get("/System/Library/Fonts/STHeiti Light.ttc"))
            add(Paths.get("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"))
            add(Paths.get("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"))
            add(Paths.get("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"))
        }

        private val DEFAULT_EMOJI_FONT_PATHS: List<Path> = buildList {
            System.getenv("WINDIR")?.takeIf { it.isNotBlank() }?.let { windowsDir ->
                add(Paths.get(windowsDir, "Fonts", "seguiemj.ttf"))
            }
            add(Paths.get("/System/Library/Fonts/Apple Color Emoji.ttc"))
            add(Paths.get("/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf"))
        }
    }
}

/** 默认全局字体上下文。简单场景直接使用它即可。 */
public object Fonts {
    public val default: FontRegistry = FontRegistry()
}

/**
 * 兼容旧 API 的全局字体工具。
 *
 * 新代码如果需要字体隔离，优先创建并传递 [FontRegistry]；
 * 旧代码继续使用 [FontUtils] 时，会读写 [Fonts.default]。
 */
public object FontUtils {

    /** 全局默认字体集合。 */
    public val fonts: FontCollection
        get() = Fonts.default.fonts

    /** 全局默认正文字体。 */
    public var defaultFont: Typeface?
        get() = Fonts.default.defaultFont
        set(value) {
            Fonts.default.defaultFont = value
        }

    /** 全局默认 Emoji 字体。 */
    public var emojiFont: Typeface?
        get() = Fonts.default.emojiFont
        set(value) {
            Fonts.default.emojiFont = value
        }


    /** 从全局字体上下文匹配字体族。 */
    public fun matchFamily(familyName: String): FontStyleSet {
        return Fonts.default.matchFamily(familyName)
    }

    /** 向全局字体上下文加载 Emoji 字体。 */
    public fun loadEmojiTypeface(path: String): Typeface? {
        return Fonts.default.loadEmojiTypeface(path)
    }

    /** 向全局字体上下文加载普通字体。 */
    public fun loadTypeface(path: String, alias: String? = null, index: Int = 0): Typeface? {
        return Fonts.default.loadTypeface(path, alias, index)
    }

    /** 从内存数据向全局字体上下文加载字体。 */
    public fun loadTypeface(data: Data, index: Int = 0): Typeface? {
        return Fonts.default.loadTypeface(data, index)
    }

    /** 向全局字体上下文注册已有 [Typeface]。 */
    public fun loadTypeface(typeface: Typeface): Typeface {
        return Fonts.default.loadTypeface(typeface)
    }

}

/** 当前样式没有指定字体族时，填入给定字体上下文的默认正文字体。 */
public fun TextStyle.withDefaultFontFamily(fontRegistry: FontRegistry = Fonts.default): TextStyle = apply {
    if (fontFamilies.isEmpty()) {
        fontRegistry.defaultFont?.familyName?.let { fontFamilies = arrayOf(it) }
    }
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
