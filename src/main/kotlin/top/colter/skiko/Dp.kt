package top.colter.skiko


/**
 *
 * 通过设置 [factor] 整体放大缩小图片
 *
 * 最终绘图时使用 [Dp.px] 把 dp 转换成 px
 *
 * 如果一些skiko的参数需要 float 可以使用 [Float.px] / [Int.px] 直接换算成最终的数值
 *
 * 在内部使用skiko的结果需使用 [Float.toDp] 转成dp
 *
 */
class Dp(private val value: Float) {
    companion object {
        var factor = 1f
        val NULL = Dp(0f)

        fun max(a: Dp, b: Dp): Dp = Dp(a.value.coerceAtLeast(b.value))
    }

    val px get() = value * factor

    fun isNull(): Boolean = this == NULL
    fun isNotNull(): Boolean = !isNull()

    operator fun plus(other: Dp): Dp = Dp(value + other.value)
    operator fun minus(other: Dp): Dp = Dp(value - other.value)

    operator fun times(other: Dp): Dp = Dp(value * other.value)
    operator fun times(other: Int): Dp = Dp(value * other)
    operator fun times(other: Float): Dp = Dp(value * other)

    operator fun div(other: Dp): Dp = Dp(value / other.value)
    operator fun div(other: Int): Dp = Dp(value / other)
    operator fun div(other: Float): Dp = Dp(value / other)
    operator fun compareTo(other: Dp): Int = if (value == other.value) 0 else if (value > other.value) 1 else -1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Dp
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Dp(value=$value)"
    }

}

inline val Int.dp: Dp get() = Dp(this.toFloat())
inline val Float.dp: Dp get() = Dp(this)

inline val Float.px: Float get() = this * Dp.factor
inline val Int.px: Float get() = this * Dp.factor

fun Float.toDp() = Dp(this / Dp.factor)