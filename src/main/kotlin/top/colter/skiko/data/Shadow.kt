package top.colter.skiko.data

import top.colter.skiko.px


/**
 * 阴影
 *
 * 可直接使用内置的 Material Design Elevation [ELEVATION_1] ~ [ELEVATION_12]
 *
 * [elevations] 阴影列表
 */
public data class Shadow(
    val offsetX: Float,
    val offsetY: Float,
    val blur: Float,
    val spread: Float = 0f,
    val shadowColor: Int
) {
    public companion object {

        /**
         * Material Design Elevation
         */
        private const val color1 = 0x33_00_00_00 // 51
        private const val color2 = 0x24_00_00_00 // 36
        private const val color3 = 0x1f_00_00_00 // 31

        public val ELEVATION_1: List<Shadow> = listOf(
            Shadow(0.px, 3.px, 1.px, (-2).px, color1),
            Shadow(0.px, 2.px, 2.px, 0.px, color2),
            Shadow(0.px, 1.px, 5.px, 0.px, color3),
        )
        public val ELEVATION_2: List<Shadow> = listOf(
            Shadow(0.px, 2.px, 4.px, (-1).px, color1),
            Shadow(0.px, 4.px, 5.px, 0.px, color2),
            Shadow(0.px, 1.px, 10.px, 0.px, color3),
        )
        public val ELEVATION_3: List<Shadow> = listOf(
            Shadow(0.px, 3.px, 5.px, (-1).px, color1),
            Shadow(0.px, 6.px, 10.px, 0.px, color2),
            Shadow(0.px, 1.px, 18.px, 0.px, color3),
        )
        public val ELEVATION_4: List<Shadow> = listOf(
            Shadow(0.px, 5.px, 5.px, (-3).px, color1),
            Shadow(0.px, 8.px, 10.px, 1.px, color2),
            Shadow(0.px, 3.px, 14.px, 2.px, color3),
        )
        public val ELEVATION_5: List<Shadow> = listOf(
            Shadow(0.px, 6.px, 6.px, (-3).px, color1),
            Shadow(0.px, 10.px, 14.px, 1.px, color2),
            Shadow(0.px, 4.px, 18.px, 3.px, color3),
        )
        public val ELEVATION_6: List<Shadow> = listOf(
            Shadow(0.px, 7.px, 8.px, (-4).px, color1),
            Shadow(0.px, 12.px, 17.px, 2.px, color2),
            Shadow(0.px, 5.px, 22.px, 4.px, color3),
        )
        public val ELEVATION_7: List<Shadow> = listOf(
            Shadow(0.px, 7.px, 9.px, (-4).px, color1),
            Shadow(0.px, 14.px, 21.px, 2.px, color2),
            Shadow(0.px, 5.px, 26.px, 4.px, color3),
        )
        public val ELEVATION_8: List<Shadow> = listOf(
            Shadow(0.px, 8.px, 10.px, (-5).px, color1),
            Shadow(0.px, 16.px, 24.px, 2.px, color2),
            Shadow(0.px, 6.px, 30.px, 5.px, color3),
        )
        public val ELEVATION_9: List<Shadow> = listOf(
            Shadow(0.px, 9.px, 11.px, (-5).px, color1),
            Shadow(0.px, 18.px, 28.px, 2.px, color2),
            Shadow(0.px, 7.px, 34.px, 6.px, color3),
        )
        public val ELEVATION_10: List<Shadow> = listOf(
            Shadow(0.px, 10.px, 13.px, (-6).px, color1),
            Shadow(0.px, 20.px, 31.px, 3.px, color2),
            Shadow(0.px, 8.px, 38.px, 7.px, color3),
        )
        public val ELEVATION_11: List<Shadow> = listOf(
            Shadow(0.px, 10.px, 14.px, (-6).px, color1),
            Shadow(0.px, 22.px, 35.px, 3.px, color2),
            Shadow(0.px, 8.px, 42.px, 7.px, color3),
        )
        public val ELEVATION_12: List<Shadow> = listOf(
            Shadow(0.px, 11.px, 15.px, (-7).px, color1),
            Shadow(0.px, 24.px, 38.px, 3.px, color2),
            Shadow(0.px, 9.px, 46.px, 8.px, color3),
        )

        public val elevations: List<List<Shadow>> = listOf(
            ELEVATION_1,
            ELEVATION_2,
            ELEVATION_3,
            ELEVATION_4,
            ELEVATION_5,
            ELEVATION_6,
            ELEVATION_7,
            ELEVATION_8,
            ELEVATION_9,
            ELEVATION_10,
            ELEVATION_11,
            ELEVATION_12
        )
    }
}