package com.projectu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.Artwork

/**
 * 作品标记组件 - 统一管理作品的额外信息标记
 * 包括：页数标记、AI标记等
 * 
 * @param artwork 作品对象
 * @param modifier 修饰符
 * @param alignment 标记在容器中的对齐方式
 */
@Composable
fun ArtworkBadges(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd
) {
    Box(modifier = modifier) {
        // AI 标记 - 左上角
        if (artwork.isAiGenerated) {
            AiBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            )
        }
        
        // 多页标记 - 右上角
        if (artwork.pageCount > 1) {
            PageCountBadge(
                pageCount = artwork.pageCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}

/**
 * AI 生成标记
 */
@Composable
private fun AiBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * 页数标记
 */
@Composable
private fun PageCountBadge(pageCount: Int, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "${pageCount}P",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
