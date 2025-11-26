package com.projectu.ui.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Pixiv官方收藏图标
 * 从Pixiv官网提取的SVG图标
 */
object PixivBookmarkIcons {
    
    /**
     * 未收藏状态 - 空心心形（#1f1f1f 描边，透明填充）
     */
    val NotBookmarked: ImageVector by lazy {
        ImageVector.Builder(
            name = "PixivBookmarkNotBookmarked",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            // 外部心形轮廓 - 使用描边方式
            path(
                fill = SolidColor(Color.Transparent), // 透明填充
                fillAlpha = 1f,
                stroke = SolidColor(Color(0xFF1F1F1F)), // #1f1f1f 描边
                strokeAlpha = 1f,
                strokeLineWidth = 2f, // 描边宽度
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21f, 5.5f)
                curveTo(24.8659932f, 5.5f, 28f, 8.63400675f, 28f, 12.5f)
                curveTo(28f, 18.2694439f, 24.2975093f, 23.1517313f, 17.2206059f, 27.1100183f)
                curveTo(16.4622493f, 27.5342993f, 15.5379984f, 27.5343235f, 14.779626f, 27.110148f)
                curveTo(7.70250208f, 23.1517462f, 4f, 18.2694529f, 4f, 12.5f)
                curveTo(4f, 8.63400691f, 7.13400681f, 5.5f, 11f, 5.5f)
                curveTo(12.829814f, 5.5f, 14.6210123f, 6.4144028f, 16f, 7.8282366f)
                curveTo(17.3789877f, 6.4144028f, 19.170186f, 5.5f, 21f, 5.5f)
                close()
            }
        }.build()
    }
    
    /**
     * 公开收藏状态 - 实心红色心形
     */
    val PublicBookmarked: ImageVector by lazy {
        ImageVector.Builder(
            name = "PixivBookmarkPublic",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            // 外部心形轮廓
            path(
                fill = SolidColor(Color(0xFFFF4060)), // Pixiv红色 #ff4060
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21f, 5.5f)
                curveTo(24.8659932f, 5.5f, 28f, 8.63400675f, 28f, 12.5f)
                curveTo(28f, 18.2694439f, 24.2975093f, 23.1517313f, 17.2206059f, 27.1100183f)
                curveTo(16.4622493f, 27.5342993f, 15.5379984f, 27.5343235f, 14.779626f, 27.110148f)
                curveTo(7.70250208f, 23.1517462f, 4f, 18.2694529f, 4f, 12.5f)
                curveTo(4f, 8.63400691f, 7.13400681f, 5.5f, 11f, 5.5f)
                curveTo(12.829814f, 5.5f, 14.6210123f, 6.4144028f, 16f, 7.8282366f)
                curveTo(17.3789877f, 6.4144028f, 19.170186f, 5.5f, 21f, 5.5f)
                close()
            }
            // 内部填充部分（同样使用 #ff4060）
            path(
                fill = SolidColor(Color(0xFFFF4060)), // 与外部相同的红色 #ff4060
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16f, 11.3317089f)
                curveTo(15.0857201f, 9.28334665f, 13.0491506f, 7.5f, 11f, 7.5f)
                curveTo(8.23857625f, 7.5f, 6f, 9.73857647f, 6f, 12.5f)
                curveTo(6f, 17.4386065f, 9.2519779f, 21.7268174f, 15.7559337f, 25.3646328f)
                curveTo(15.9076021f, 25.4494645f, 16.092439f, 25.4494644f, 16.2441073f, 25.3646326f)
                curveTo(22.7480325f, 21.7268037f, 26f, 17.4385986f, 26f, 12.5f)
                curveTo(26f, 9.73857625f, 23.7614237f, 7.5f, 21f, 7.5f)
                curveTo(18.9508494f, 7.5f, 16.9142799f, 9.28334665f, 16f, 11.3317089f)
                close()
            }
        }.build()
    }
    
    /**
     * 私人收藏状态 - 实心红色心形 + 锁图标
     */
    val PrivateBookmarked: ImageVector by lazy {
        ImageVector.Builder(
            name = "PixivBookmarkPrivate",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            // 外部心形轮廓
            path(
                fill = SolidColor(Color(0xFFFF4060)), // Pixiv红色 #FF4060
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(21f, 5.5f)
                curveTo(24.8659932f, 5.5f, 28f, 8.63400675f, 28f, 12.5f)
                curveTo(28f, 18.2694439f, 24.2975093f, 23.1517313f, 17.2206059f, 27.1100183f)
                curveTo(16.4622493f, 27.5342993f, 15.5379984f, 27.5343235f, 14.779626f, 27.110148f)
                curveTo(7.70250208f, 23.1517462f, 4f, 18.2694529f, 4f, 12.5f)
                curveTo(4f, 8.63400691f, 7.13400681f, 5.5f, 11f, 5.5f)
                curveTo(12.829814f, 5.5f, 14.6210123f, 6.4144028f, 16f, 7.8282366f)
                curveTo(17.3789877f, 6.4144028f, 19.170186f, 5.5f, 21f, 5.5f)
                close()
            }
            // 内部填充部分（同样使用 #FF4060）
            path(
                fill = SolidColor(Color(0xFFFF4060)), // 与外部相同的红色 #FF4060
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16f, 11.3317089f)
                curveTo(15.0857201f, 9.28334665f, 13.0491506f, 7.5f, 11f, 7.5f)
                curveTo(8.23857625f, 7.5f, 6f, 9.73857647f, 6f, 12.5f)
                curveTo(6f, 17.4386065f, 9.2519779f, 21.7268174f, 15.7559337f, 25.3646328f)
                curveTo(15.9076021f, 25.4494645f, 16.092439f, 25.4494644f, 16.2441073f, 25.3646326f)
                curveTo(22.7480325f, 21.7268037f, 26f, 17.4385986f, 26f, 12.5f)
                curveTo(26f, 9.73857625f, 23.7614237f, 7.5f, 21f, 7.5f)
                curveTo(18.9508494f, 7.5f, 16.9142799f, 9.28334665f, 16f, 11.3317089f)
                close()
            }
            // 锁背景（白色 #fff）
            path(
                fill = SolidColor(Color.White), // 白色 #fff
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.EvenOdd // fill-rule: evenodd
            ) {
                moveTo(29.9796f, 20.5234f)
                curveTo(31.1865f, 21.2121f, 32f, 22.511f, 32f, 24f)
                verticalLineTo(28f)
                curveTo(32f, 30.2091f, 30.2091f, 32f, 28f, 32f)
                horizontalLineTo(21f)
                curveTo(18.7909f, 32f, 17f, 30.2091f, 17f, 28f)
                verticalLineTo(24f)
                curveTo(17f, 22.511f, 17.8135f, 21.2121f, 19.0204f, 20.5234f)
                curveTo(19.2619f, 17.709f, 21.623f, 15.5f, 24.5f, 15.5f)
                curveTo(27.377f, 15.5f, 29.7381f, 17.709f, 29.9796f, 20.5234f)
                close()
            }
            // 锁图标（深色 #1f1f1f）
            path(
                fill = SolidColor(Color(0xFF1F1F1F)), // 深色 #1f1f1f
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.EvenOdd // fill-rule: evenodd
            ) {
                moveTo(28f, 22f)
                curveTo(29.1046f, 22f, 30f, 22.8954f, 30f, 24f)
                verticalLineTo(28f)
                curveTo(30f, 29.1046f, 29.1046f, 30f, 28f, 30f)
                horizontalLineTo(21f)
                curveTo(19.8954f, 30f, 19f, 29.1046f, 19f, 28f)
                verticalLineTo(24f)
                curveTo(19f, 22.8954f, 19.8954f, 22f, 21f, 22f)
                verticalLineTo(21f)
                curveTo(21f, 19.067f, 22.567f, 17.5f, 24.5f, 17.5f)
                curveTo(26.433f, 17.5f, 28f, 19.067f, 28f, 21f)
                verticalLineTo(22f)
                close()
                moveTo(23f, 21f)
                curveTo(23f, 20.1716f, 23.6716f, 19.5f, 24.5f, 19.5f)
                curveTo(25.3284f, 19.5f, 26f, 20.1716f, 26f, 21f)
                verticalLineTo(22f)
                horizontalLineTo(23f)
                verticalLineTo(21f)
                close()
            }
        }.build()
    }
}

