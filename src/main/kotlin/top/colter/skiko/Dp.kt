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
public class Dp(private val value: Float) {
    public companion object {
        public var factor: Float = 1f
        public val NULL: Dp = Dp(0f)

        public fun max(a: Dp, b: Dp): Dp = Dp(a.value.coerceAtLeast(b.value))
    }

    public val px: Float get() = value * factor

    public fun isNull(): Boolean = this == NULL
    public fun isNotNull(): Boolean = !isNull()

    public operator fun plus(other: Dp): Dp = Dp(value + other.value)
    public operator fun minus(other: Dp): Dp = Dp(value - other.value)

    public operator fun times(other: Dp): Dp = Dp(value * other.value)
    public operator fun times(other: Int): Dp = Dp(value * other)
    public operator fun times(other: Float): Dp = Dp(value * other)

    public operator fun div(other: Dp): Dp = Dp(value / other.value)
    public operator fun div(other: Int): Dp = Dp(value / other)
    public operator fun div(other: Float): Dp = Dp(value / other)
    public operator fun compareTo(other: Dp): Int = if (value == other.value) 0 else if (value > other.value) 1 else -1

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

public inline val Int.dp: Dp get() = Dp(this.toFloat())
public inline val Float.dp: Dp get() = Dp(this)

public inline val Float.px: Float get() = this * Dp.factor
public inline val Int.px: Float get() = this * Dp.factor

public fun Float.toDp(): Dp = Dp(this / Dp.factor)