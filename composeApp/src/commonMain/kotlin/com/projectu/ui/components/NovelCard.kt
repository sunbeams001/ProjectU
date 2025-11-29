package com.projectu.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.getUrlByQuality
import org.koin.compose.koinInject

/**
 * 小说卡片组件 - Material Design 3 风格
 * 用于单行列表展示
 * 
 * 显示内容：
 * - 封面（左侧，固定尺寸）
 * - 标题
 * - 标签（最多3个）
 * - 作者信息（头像+名字）
 * - 收藏状态
 * - 字数
 * - 收藏数
 * - 系列标题（如果有）
 * 
 * @param novel 小说对象
 * @param onClick 点击小说回调
 * @param onUserClick 点击用户区域回调（头像或用户名），为null时不响应用户点击
 * @param modifier 修饰符
 * @param settingsCache 设置缓存
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelCard(
    novel: Novel,
    onClick: () -> Unit,
    onUserClick: ((userId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    settingsCache: SettingsCache = koinInject()
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
                .combinedClickable(
                    onClick = {
                        println("NovelCard: 点击卡片主体 - ID: ${novel.id}, 标题: ${novel.title}")
                        onClick()
                    },
                    onLongClick = {
                        println("NovelCard: 长按卡片主体 - ID: ${novel.id}, 标题: ${novel.title}")
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左侧：封面
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = {
                            println("NovelCard: 点击封面 - ID: ${novel.id}")
                            onClick()
                        },
                        onLongClick = {
                            println("NovelCard: 长按封面 - ID: ${novel.id}")
                        }
                    )
            ) {
                // 封面图片或默认图标
                if (novel.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = novel.imageUrl,
                        contentDescription = novel.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 默认图标背景
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // AI标记（如果是AI生成）
                if (novel.isAiGenerated) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            // 右侧：信息区域（自适应高度）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 120.dp), // 最小高度120dp，但可以根据内容增长
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上部：标题 + 收藏状态
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // 标题（最多3行）
                        Text(
                            text = novel.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 收藏状态指示器
                        NovelBookmarkIndicator(
                            novel = novel,
                            size = 20.dp
                        )
                    }
                    
                    // 标签（显示所有标签，支持换行）
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 检查是否需要显示R-18或R18G标签
                        val needsR18Tag = novel.ageLimit == AgeLimit.R18 && 
                            !novel.tags.any { it.name.equals("R-18", ignoreCase = true) }
                        val needsR18GTag = novel.ageLimit == AgeLimit.R18G && 
                            !novel.tags.any { it.name.equals("R-18G", ignoreCase = true) }
                        
                        // 如果需要，先显示R-18或R18G标签
                        if (needsR18Tag) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = r18Color.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .height(20.dp)
                                    .combinedClickable(
                                        onClick = { println("NovelCard: 点击标签 - R-18") },
                                        onLongClick = { println("NovelCard: 长按标签 - R-18") }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R-18",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = r18Color,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        if (needsR18GTag) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = r18Color.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .height(20.dp)
                                    .combinedClickable(
                                        onClick = { println("NovelCard: 点击标签 - R-18G") },
                                        onLongClick = { println("NovelCard: 长按标签 - R-18G") }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "R-18G",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = r18Color,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        // 显示所有原有标签
                        novel.tags.forEach { tag ->
                            val isR18 = tag.name.equals("R-18", ignoreCase = true)
                            val isR18G = tag.name.equals("R-18G", ignoreCase = true)
                            val tagColor = if (isR18 || isR18G) r18Color else MaterialTheme.colorScheme.onSecondaryContainer
                            val tagBgColor = if (isR18 || isR18G) 
                                r18Color.copy(alpha = 0.15f) 
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                            
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tagBgColor,
                                modifier = Modifier
                                    .height(20.dp)
                                    .combinedClickable(
                                        onClick = { 
                                            println("NovelCard: 点击标签 - ${tag.translatedName ?: tag.name}") 
                                        },
                                        onLongClick = { 
                                            println("NovelCard: 长按标签 - ${tag.translatedName ?: tag.name}") 
                                        }
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag.translatedName ?: tag.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tagColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    // 描述（如果有） - 支持点击展开/收起
                    if (novel.description.isNotBlank()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        val plainText = remember(novel.description) {
                            htmlToPlainText(novel.description)
                        }
                        // 判断是否需要展开功能（文本超过3行约60个字符时）
                        val needsExpansion = plainText.length > 60 || plainText.count { it == '\n' } >= 2
                        
                        HtmlText(
                            html = novel.description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = if (isExpanded) null else 3,
                            overflow = TextOverflow.Ellipsis,
                            onClick = if (needsExpansion) {
                                { isExpanded = !isExpanded }
                            } else null
                        )
                    }
                    
                    // 系列标题（如果有）
                    if (novel.seriesTitle != null) {
                        Text(
                            text = "系列: ${novel.seriesTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { 
                                        println("NovelCard: 点击系列 - ${novel.seriesTitle} (ID: ${novel.seriesId})") 
                                    },
                                    onLongClick = { 
                                        println("NovelCard: 长按系列 - ${novel.seriesTitle} (ID: ${novel.seriesId})") 
                                    }
                                )
                        )
                    }
                    
                    // 增加与下方作者信息的间距
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 下部：作者信息 + 统计信息（在右下角）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：作者信息（头像 + 名称）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (onUserClick != null) {
                                    Modifier.combinedClickable(
                                        onClick = {
                                            novel.userId.toLongOrNull()?.let { onUserClick(it) }
                                        }
                                    )
                                } else Modifier
                            )
                    ) {
                        // 作者头像
                        AsyncImage(
                            model = novel.userProfileImageUrl,
                            contentDescription = novel.userName,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 作者名
                        Text(
                            text = novel.userName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 右侧：统计信息（字数 + 收藏数）
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 字数
                        Text(
                            text = "${formatNumber(novel.textCount)}字",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 收藏数
                        Text(
                            text = "❤ ${formatNumber(novel.bookmarkCount)}",
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
 * 格式化数字（千位分隔）
 */
private fun formatNumber(number: Int): String {
    return when {
        number >= 10000 -> String.format("%.1fw", number / 10000.0)
        number >= 1000 -> String.format("%.1fk", number / 1000.0)
        else -> number.toString()
    }
}


