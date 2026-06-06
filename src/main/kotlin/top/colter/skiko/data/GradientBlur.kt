package top.colter.skiko.data

import top.colter.skiko.Dp
import top.colter.skiko.dp

/**
 * 渐变模糊插值方式。
 */
public enum class GradientBlurInterpolation {
    LINEAR,
    SMOOTH,
}

/**
 * 渐变模糊控制点。
 *
 * @param position 渐变位置，范围 0..1
 * @param blur 该位置的模糊半径
 */
public data class GradientBlurStop(
    val position: Float,
    val blur: Dp,
) {
    init {
        require(position in 0f..1f) { "渐变模糊位置需要在 0..1 之间" }
        require(blur >= 0.dp) { "渐变模糊半径需要大于等于 0" }
    }
}

/**
 * 图片渐变模糊配置。
 *
 * @param angle 渐变方向角度，0 表示从左到右，90 表示从上到下
 * @param stops 模糊控制点
 * @param steps 多档 sigma 的档位数量
 * @param stripWidth 合成时每条采样带的宽度
 * @param interpolation 控制点之间的插值方式
 */
public data class GradientBlur(
    val angle: Float = 0f,
    val stops: List<GradientBlurStop>,
    val steps: Int = 7,
    val stripWidth: Dp = 4.dp,
    val interpolation: GradientBlurInterpolation = GradientBlurInterpolation.SMOOTH,
) {
    init {
        require(stops.size >= 2) { "渐变模糊至少需要 2 个控制点" }
        require(stops.first().position == 0f) { "渐变模糊第一个控制点位置必须是 0" }
        require(stops.last().position == 1f) { "渐变模糊最后一个控制点位置必须是 1" }
        require(steps in 2..16) { "渐变模糊 steps 需要在 2..16 之间" }
        require(stripWidth > 0.dp) { "渐变模糊 stripWidth 需要大于 0" }

        stops.zipWithNext().forEach { (prev, next) ->
            require(prev.position < next.position) { "渐变模糊控制点位置必须严格递增" }
        }
    }

    internal val maxBlur: Dp
        get() {
            var result = 0.dp
            stops.forEach {
                if (it.blur > result) result = it.blur
            }
            return result
        }

    internal fun blurAt(position: Float): Dp {
        val value = position.coerceIn(0f, 1f)
        val nextIndex = stops.indexOfFirst { it.position >= value }
        if (nextIndex <= 0) return stops.first().blur

        val prev = stops[nextIndex - 1]
        val next = stops[nextIndex]
        val distance = next.position - prev.position
        if (distance <= 0f) return next.blur

        val rawProgress = ((value - prev.position) / distance).coerceIn(0f, 1f)
        val progress = when (interpolation) {
            GradientBlurInterpolation.LINEAR -> rawProgress
            GradientBlurInterpolation.SMOOTH -> rawProgress * rawProgress * (3f - 2f * rawProgress)
        }
        return prev.blur + (next.blur - prev.blur) * progress
    }

    public companion object {
        /**
         * 两侧模糊、中间清晰的常用预设。
         */
        public fun edge(
            maxBlur: Dp,
            angle: Float = 0f,
            clearStart: Float = 0.35f,
            clearEnd: Float = 0.65f,
            steps: Int = 7,
            stripWidth: Dp = 4.dp,
            interpolation: GradientBlurInterpolation = GradientBlurInterpolation.SMOOTH,
        ): GradientBlur {
            require(maxBlur >= 0.dp) { "渐变模糊最大半径需要大于等于 0" }
            require(clearStart in 0f..1f) { "清晰区域起点需要在 0..1 之间" }
            require(clearEnd in 0f..1f) { "清晰区域终点需要在 0..1 之间" }
            require(clearStart > 0f) { "清晰区域起点需要大于 0" }
            require(clearEnd < 1f) { "清晰区域终点需要小于 1" }
            require(clearStart < clearEnd) { "清晰区域起点需要小于终点" }

            return GradientBlur(
                angle = angle,
                stops = listOf(
                    GradientBlurStop(0f, maxBlur),
                    GradientBlurStop(clearStart, 0.dp),
                    GradientBlurStop(clearEnd, 0.dp),
                    GradientBlurStop(1f, maxBlur),
                ),
                steps = steps,
                stripWidth = stripWidth,
                interpolation = interpolation,
            )
        }
    }
}
