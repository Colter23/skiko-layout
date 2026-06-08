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

public data class RichTextFontSizeCandidate(
    public val fontSize: Dp,
    public val layout: RichParagraphLayout,
)

public fun interface RichTextFontSizeSelector {
    public fun select(
        candidates: List<RichTextFontSizeCandidate>,
        contentWidth: Float,
    ): RichTextFontSizeCandidate
}

public object BalancedRichTextFontSizeSelector : RichTextFontSizeSelector {
    override fun select(
        candidates: List<RichTextFontSizeCandidate>,
        contentWidth: Float,
    ): RichTextFontSizeCandidate {
        var minFontSize = Float.MAX_VALUE
        var maxFontSize = Float.MIN_VALUE
        var largest = candidates.first()
        candidates.forEach { candidate ->
            val fontSize = candidate.fontSize.px
            if (fontSize < minFontSize) minFontSize = fontSize
            if (fontSize > maxFontSize) {
                maxFontSize = fontSize
                largest = candidate
            }
        }
        if (contentWidth <= 0f || largest.layout.lineCount <= 1) return largest

        val fontRange = (maxFontSize - minFontSize).coerceAtLeast(1f)
        var best = largest
        var bestScore = visualScore(largest, minFontSize, fontRange, contentWidth)
        candidates.forEach { candidate ->
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
        require(minFontSize <= maxFontSize) { "minFontSize require <= maxFontSize" }
        require(fontSizeStep > 0.dp) { "fontSizeStep require > 0" }
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
        drawBgBox(canvas) {
            selectedLayout?.print(
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
