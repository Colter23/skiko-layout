package top.colter.skiko.layout

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.paragraph.Alignment
import top.colter.skiko.Dp
import top.colter.skiko.FontRegistry
import top.colter.skiko.Fonts
import top.colter.skiko.Modifier
import top.colter.skiko.data.LayoutAlignment
import top.colter.skiko.data.RichParagraph
import top.colter.skiko.data.RichParagraphLayout
import top.colter.skiko.data.layout
import top.colter.skiko.data.print
import top.colter.skiko.data.place
import top.colter.skiko.dp
import top.colter.skiko.toDp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 自适应字号候选。
 *
 * [fontSize] 是当前候选字号，[layout] 是使用该字号完成 Skia Paragraph 排版后的结果。
 * 选择器可以根据行数、行宽、是否超出最大行数等信息决定最终使用哪个候选。
 */
public data class RichTextFontSizeCandidate(
    public val fontSize: Dp,
    public val layout: RichParagraphLayout,
)

/**
 * 自定义自适应字号选择器。
 *
 * [candidates] 会按字号从大到小生成，并且包含最小字号候选。
 * [contentWidth] 是富文本实际参与排版的内容宽度，单位为 px。
 */
public fun interface RichTextFontSizeSelector {
    public fun select(
        candidates: List<RichTextFontSizeCandidate>,
        contentWidth: Float,
    ): RichTextFontSizeCandidate
}

/**
 * 默认的均衡字号选择器。
 *
 * 选择策略偏向视觉稳定：先排除已经超出 [AutoSizeRichText] 最大行数限制的候选；
 * 如果存在单行可展示的最大字号，直接使用最大字号；否则综合比较非最后一行的平均填充度、
 * 最差填充度、行间填充差异和字号大小，尽量减少右侧大块留白，同时避免字号过小。
 */
public object BalancedRichTextFontSizeSelector : RichTextFontSizeSelector {
    override fun select(
        candidates: List<RichTextFontSizeCandidate>,
        contentWidth: Float,
    ): RichTextFontSizeCandidate {
        val selectableCandidates = candidates
            .filter { !it.layout.didExceedMaxLines }
            .ifEmpty { candidates }
        var minFontSize = Float.MAX_VALUE
        var maxFontSize = Float.MIN_VALUE
        var largest = selectableCandidates.first()
        selectableCandidates.forEach { candidate ->
            val fontSize = candidate.fontSize.px
            if (fontSize < minFontSize) minFontSize = fontSize
            if (fontSize > maxFontSize) {
                maxFontSize = fontSize
                largest = candidate
            }
        }
        if (contentWidth <= 0f || (largest.layout.lineCount <= 1 && !largest.layout.didExceedMaxLines)) return largest

        val fontRange = (maxFontSize - minFontSize).coerceAtLeast(1f)
        var best = largest
        var bestScore = visualScore(largest, minFontSize, fontRange, contentWidth)
        selectableCandidates.forEach { candidate ->
            val score = visualScore(candidate, minFontSize, fontRange, contentWidth)
            if (score > bestScore || score == bestScore && candidate.fontSize.px > best.fontSize.px) {
                best = candidate
                bestScore = score
            }
        }
        return best
    }

    private fun visualScore(
        candidate: RichTextFontSizeCandidate,
        minFontSize: Float,
        fontRange: Float,
        contentWidth: Float,
    ): Float {
        val fill = lineFill(candidate, contentWidth)
        val sizeRatio = (candidate.fontSize.px - minFontSize) / fontRange
        if (fill.count == 0) return sizeRatio

        val averageGap = 1f - fill.average
        val worstGap = 1f - fill.worst
        return -(averageGap * 4.0f) -
            (worstGap * 1.8f) -
            (fill.spread * 0.35f) +
            (sizeRatio * 0.12f)
    }

    private fun lineFill(
        candidate: RichTextFontSizeCandidate,
        contentWidth: Float,
    ): LineFill {
        val lineMetrics = candidate.layout.lineMetrics
        var count = 0
        var total = 0f
        var worst = Float.MAX_VALUE
        var best = Float.MIN_VALUE

        for (index in 0 until lineMetrics.lastIndex) {
            val line = lineMetrics[index]
            if (line.isHardBreak || line.width <= 0f) continue

            val ratio = (line.width / contentWidth).coerceIn(0f, 1f)
            count++
            total += ratio
            if (ratio < worst) worst = ratio
            if (ratio > best) best = ratio
        }
        if (count == 0) return LineFill()

        val average = total / count
        return LineFill(
            count = count,
            average = average,
            worst = worst,
            spread = best - worst,
        )
    }

    private data class LineFill(
        val count: Int = 0,
        val average: Float = 0f,
        val worst: Float = 0f,
        val spread: Float = 0f,
    )
}

/**
 * 自适应字号富文本。
 *
 * 该组件会在 [minFontSize] 到 [maxFontSize] 之间按 [fontSizeStep] 全量生成候选字号，
 * 每个候选都会调用 [paragraph] 重新构建并排版。最终字号由 [fontSizeSelector] 决定。
 *
 * 常用于动态正文、标题等需要“短文本放大、长文本缩小”的场景。为了保证视觉结果稳定，
 * 默认实现不使用粗排/细排等启发式搜索；如果调用方更关注性能，可以适当调大
 * [fontSizeStep]，减少参与排版的候选数量。
 */
public fun Layout.AutoSizeRichText(
    minFontSize: Dp,
    maxFontSize: Dp,
    fontSizeStep: Dp = 1.dp,
    maxLinesCount: Int = 50,
    alignment: LayoutAlignment = LayoutAlignment.DEFAULT,
    intrinsicAlignment: Alignment = Alignment.START,
    modifier: Modifier = Modifier(),
    fontSizeSelector: RichTextFontSizeSelector = BalancedRichTextFontSizeSelector,
    paragraph: (fontSize: Dp) -> RichParagraph,
) {
    Layout(
        layout = AutoSizeRichTextLayout(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            fontSizeStep = fontSizeStep,
            maxLinesCount = maxLinesCount,
            alignment = alignment,
            intrinsicAlignment = intrinsicAlignment,
            fontSizeSelector = fontSizeSelector,
            paragraph = paragraph,
            modifier = modifier,
            parentLayout = this,
        ),
        content = {},
    )
}

/**
 * [AutoSizeRichText] 的实际布局节点。
 *
 * 测量时会根据可用宽度缓存最终选中的 [RichParagraphLayout]，同一宽度重复测量不会再次排版。
 */
public class AutoSizeRichTextLayout(
    public val minFontSize: Dp,
    public val maxFontSize: Dp,
    public val fontSizeStep: Dp,
    public val maxLinesCount: Int,
    public val alignment: LayoutAlignment,
    public val intrinsicAlignment: Alignment,
    public val fontSizeSelector: RichTextFontSizeSelector,
    public val paragraph: (fontSize: Dp) -> RichParagraph,
    modifier: Modifier,
    parentLayout: Layout?,
    fontRegistry: FontRegistry = parentLayout?.fontRegistry ?: Fonts.default,
) : Layout(modifier, parentLayout, fontRegistry) {

    public var selectedFontSize: Dp = Dp.NULL
        private set

    private var selectedLayout: RichParagraphLayout? = null
    private var layoutWidthPx: Float = -1f

    init {
        require(minFontSize <= maxFontSize) { "minFontSize 需要小于等于 maxFontSize" }
        require(fontSizeStep > 0.dp) { "fontSizeStep 需要大于 0" }
    }

    private fun resolveParagraph(widthPx: Float): RichParagraphLayout {
        val safeWidth = widthPx.coerceAtLeast(1f)
        val cached = selectedLayout
        if (cached != null && layoutWidthPx == safeWidth) return cached

        val candidates = buildCandidates(safeWidth)
        val selected = fontSizeSelector.select(candidates, safeWidth)
        selectedFontSize = selected.fontSize
        selectedLayout = selected.layout
        layoutWidthPx = safeWidth
        return selected.layout
    }

    private fun buildCandidates(widthPx: Float): List<RichTextFontSizeCandidate> {
        val minPx = minFontSize.px
        val maxPx = maxFontSize.px
        val stepPx = fontSizeStep.px.coerceAtLeast(0.1f)
        val result = ArrayList<RichTextFontSizeCandidate>(((maxPx - minPx) / stepPx).toInt().coerceAtLeast(0) + 2)
        var current = maxPx
        var minIncluded = false
        var lastKey: Int? = null

        // 从最大字号开始扫描，优先保留大字号候选；浮点误差导致的重复字号用 key 去重。
        while (current >= minPx - FONT_SIZE_EPSILON) {
            val fontPx = current.coerceAtLeast(minPx)
            val key = (fontPx * 100).roundToInt()
            if (key != lastKey) {
                result.add(fontPx.toDp().toCandidate(widthPx))
                if (abs(fontPx - minPx) <= FONT_SIZE_EPSILON) minIncluded = true
                lastKey = key
            }
            current -= stepPx
        }
        // 当步长不能整除字号范围时，显式补上最小字号，保证始终有兜底候选。
        if (!minIncluded) {
            result.add(minPx.toDp().toCandidate(widthPx))
        }
        return result
    }

    private fun Dp.toCandidate(widthPx: Float): RichTextFontSizeCandidate {
        return RichTextFontSizeCandidate(
            fontSize = this,
            layout = paragraph(this).layout(widthPx, maxLinesCount, fontRegistry, intrinsicAlignment),
        )
    }

    override fun measure(deep: Boolean) {
        preMeasure()

        if (modifier.fillWidth && width.isNull()) {
            finishMeasure()
            if (width.isNull()) return
        }

        val availableWidth = availableParentWidth()
        val maxOuterWidth = when {
            width.isNotNull() -> width
            modifier.maxWidth.isNotNull() -> modifier.maxWidth
            availableWidth.isNotNull() -> availableWidth
            else -> Dp.NULL
        }
        val maxContentWidth = if (maxOuterWidth.isNotNull())
            (maxOuterWidth - resolvedPadding.horizontal).coerceAtLeast(0.dp)
        else 10000.dp

        val paragraphWidth = (maxContentWidth.px - RICH_TEXT_VISUAL_OUTSET_HORIZONTAL).coerceAtLeast(1f)
        val layout = resolveParagraph(paragraphWidth)
        if (width.isNull()) {
            width = (layout.width + RICH_TEXT_VISUAL_OUTSET_HORIZONTAL)
                .coerceAtMost(maxContentWidth.px.coerceAtLeast(0f))
                .toDp() +
                    resolvedPadding.horizontal
        }
        if (height.isNull()) height = (layout.height + RICH_TEXT_VISUAL_OUTSET_VERTICAL).toDp() + resolvedPadding.vertical

        finishMeasure()
    }

    override fun place(bounds: LayoutBounds) {
        position = alignment.place(width, height, resolvedMargin, bounds)
        resolvePaintBounds()
    }

    override fun draw(canvas: Canvas) {
        drawBgBox(canvas, contentClipOutset = richTextContentClipOutset(selectedLayout)) {
            val layout = selectedLayout ?: return@drawBgBox
            layout.print(
                canvas = canvas,
                x = it.left + RICH_TEXT_VISUAL_OUTSET,
                y = it.top + RICH_TEXT_VISUAL_OUTSET,
            )
        }
    }

    private companion object {
        private const val FONT_SIZE_EPSILON: Float = 0.01f
    }
}
