package com.projectu.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.ui.util.formatReadingTime

/**
 * 小说系列卡片组件 - Material Design 3 风格
 * 
 * 显示内容：
 * - 封面（左侧，固定尺寸）
 * - 标题
 * - 标签（支持R-18标记和普通标签）
 * - 简介（支持展开/收起，解析HTML）
 * - 统计信息（篇数、字数）
 * - 完结/连载状态
 * 
 * @param series 小说系列信息
 * @param onClick 点击卡片回调
 * @param modifier 修饰符
 */
@Composable
fun NovelSeriesCard(
    series: NovelSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // R-18和R18G标签颜色
    val r18Color = Color(0xFFFF4060)
    
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
                    .width(80.dp)
                    .height(120.dp)
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
                                text = "📚",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
                
                // 完结/连载状态标记
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = if (series.isConcluded) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (series.isConcluded) stringResource(Res.string.series_concluded) else stringResource(Res.string.series_ongoing),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (series.isConcluded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 右侧：信息区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上部：标题和标签
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
                    
                    // 标签（使用FlowRow，类似小说卡片）
                    if (series.tags.isNotEmpty() || series.isR18 || series.isR18G || series.isOriginal || series.isAiGenerated) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // R-18/R-18G 标签
                            if (series.isR18) {
                                TagChip(
                                    text = "R-18",
                                    textColor = r18Color,
                                    backgroundColor = r18Color.copy(alpha = 0.15f)
                                )
                            } else if (series.isR18G) {
                                TagChip(
                                    text = "R-18G",
                                    textColor = r18Color,
                                    backgroundColor = r18Color.copy(alpha = 0.15f)
                                )
                            }
                            
                            // 原创标签
                            if (series.isOriginal) {
                                TagChip(
                                    text = stringResource(Res.string.series_original),
                                    textColor = MaterialTheme.colorScheme.primary,
                                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                            
                            // AI 生成标签
                            if (series.isAiGenerated) {
                                TagChip(
                                    text = "AI",
                                    textColor = MaterialTheme.colorScheme.tertiary,
                                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            }
                            
                            // 普通标签（展示所有标签）
                            series.tags.forEach { tag ->
                                // 跳过已经显示的R-18标签
                                if (!tag.equals("R-18", ignoreCase = true) && 
                                    !tag.equals("R-18G", ignoreCase = true)) {
                                    TagChip(
                                        text = tag,
                                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // 简介（支持展开/收起，解析HTML）
                    if (series.caption.isNotBlank()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        val plainText = remember(series.caption) {
                            htmlToPlainText(series.caption)
                        }
                        val needsExpansion = plainText.length > 60 || plainText.count { it == '\n' } >= 2
                        
                        HtmlText(
                            html = series.caption,
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
                    
                    // 间距
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 下部：统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 篇数
                    Text(
                        text = stringResource(Res.string.series_total_episodes, series.contentCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 字数
                    series.totalCharacterCount?.takeIf { it > 0 }?.let { count ->
                        Text(
                            text = stringResource(Res.string.series_word_count, formatWordCount(count)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 预计阅读时间
                    series.readingTimeSeconds?.takeIf { it > 0 }?.let { seconds ->
                        Text(
                            text = formatReadingTime(seconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 标签芯片
 */
@Composable
private fun TagChip(
    text: String,
    textColor: Color,
    backgroundColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        modifier = Modifier.height(20.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 格式化字数
 */
private fun formatWordCount(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1fw", count / 10000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}
