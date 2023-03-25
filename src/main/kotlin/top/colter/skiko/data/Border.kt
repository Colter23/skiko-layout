package top.colter.skiko.data

import org.jetbrains.skia.Color
import top.colter.skiko.Dp
import top.colter.skiko.dp

data class Border(
    val width: Dp = 0.dp,
    val radius: List<Dp> = listOf(0.dp, 0.dp, 0.dp, 0.dp),
    val color: Int = Color.WHITE
)