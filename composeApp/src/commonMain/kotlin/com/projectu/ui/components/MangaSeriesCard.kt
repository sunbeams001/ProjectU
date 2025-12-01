package com.projectu.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.MangaSeries

/**
 * 漫画系列卡片组件 - Material Design 3 风格
 * 
 * 显示内容：
 * - 封面（左侧，固定尺寸）
 * - 标题
 * - 简介（支持展开/收起，解析HTML）
 * - 统计信息（篇数）
 * - 追更状态
 * 
 * @param series 漫画系列信息
 * @param onClick 点击卡片回调
 * @param modifier 修饰符
 */
@Composable
fun MangaSeriesCard(
    series: MangaSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左侧：封面
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (series.coverUrl != null) {
                    AsyncImage(
                        model = series.coverUrl,
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 默认占位背景
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🖼️",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
                
                // 追更状态标记
                if (series.isWatched) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "追更中",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 右侧：信息区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 60.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上部：标题和简介
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 标题（最多2行）
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 简介（支持展开/收起，解析HTML）
                    val captionText = series.caption ?: series.description
                    captionText?.takeIf { it.isNotBlank() }?.let { caption ->
                        var isExpanded by remember { mutableStateOf(false) }
                        val plainText = remember(caption) {
                            htmlToPlainText(caption)
                        }
                        val needsExpansion = plainText.length > 50 || plainText.count { it == '\n' } >= 2
                        
                        HtmlText(
                            html = caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = if (isExpanded) null else 2,
                            overflow = TextOverflow.Ellipsis,
                            onClick = if (needsExpansion) {
                                { isExpanded = !isExpanded }
                            } else null
                        )
                    }
                }
                
                // 下部：统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 篇数
                    Text(
                        text = "共 ${series.total} 篇",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 更新日期（如果有）
                    series.updateDate?.let { date ->
                        // 只显示日期部分（去掉时间和时区）
                        val displayDate = date.substringBefore("T")
                        Text(
                            text = "更新: $displayDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
