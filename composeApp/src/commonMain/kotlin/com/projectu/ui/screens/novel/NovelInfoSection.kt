package com.projectu.ui.screens.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.User
import com.projectu.shared.util.DateTimeFormatter
import com.projectu.ui.components.FollowIndicator
import com.projectu.ui.components.HtmlText
import com.projectu.ui.components.NovelBookmarkIndicator

/**
 * 小说信息展示区域
 * 
 * 支持收缩和展开两种状态
 * - 收缩状态：仅显示一个可点击的按钮或提示条
 * - 展开状态：显示完整的小说信息
 * 
 * @param novel 小说对象
 * @param authorFollowStatus 作者关注状态
 * @param isExpanded 是否展开
 * @param isMarkerLoading 书签操作是否正在加载
 * @param onToggle 切换展开/收缩回调
 * @param onCollapse 收起回调（用于下拉手势触发）
 * @param onMarkerClick 点击书签按钮回调
 * @param onUserClick 点击用户区域回调
 * @param onSeriesClick 点击系列回调
 * @param modifier 修饰符
 */
@Composable
fun NovelInfoSection(
    novel: Novel,
    authorFollowStatus: FollowStatus,
    isExpanded: Boolean,
    isMarkerLoading: Boolean = false,
    onToggle: () -> Unit,
    onCollapse: () -> Unit = onToggle,
    onMarkerClick: () -> Unit = {},
    onUserClick: ((userId: Long) -> Unit)? = null,
    onSeriesClick: ((seriesId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val r18Color = Color(0xFFFF4060)
    val hasMarker = novel.marker != null
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 收缩状态的提示条 - 始终显示
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 仅在收缩状态时添加底部导航栏 padding（展开时由详细信息区域处理）
                    .then(if (!isExpanded) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：标题
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 右侧：书签按钮 + 收藏按钮 + 展开图标
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 阅读书签按钮（稍后再读）
                    IconButton(
                        onClick = onMarkerClick,
                        enabled = !isMarkerLoading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isMarkerLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (hasMarker) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                contentDescription = if (hasMarker) "移除书签" else "添加书签",
                                tint = if (hasMarker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // 收藏按钮
                    NovelBookmarkIndicator(
                        novel = novel,
                        size = 24.dp
                    )
                    
                    // 展开/收缩图标
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 展开状态的详细信息
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 第一行：封面 + 基本信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 封面
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            if (novel.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = novel.imageUrl,
                                    contentDescription = novel.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            
                            // AI标记
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
                        
                        // 右侧信息
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 标题
                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // 系列信息
                            if (novel.seriesTitle != null && novel.seriesId != null) {
                                Text(
                                    text = "📚 ${novel.seriesTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable {
                                        novel.seriesId?.toLongOrNull()?.let { onSeriesClick?.invoke(it) }
                                    }
                                )
                            }
                            
                            // 字数和阅读时长
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📝 ${formatNumber(novel.textCount)}字",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "⏱️ ${formatReadingTime(novel.readingTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 页数
                            if (novel.pageCount > 1) {
                                Text(
                                    text = "📄 共${novel.pageCount}页",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 统计信息行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            icon = Icons.Default.Visibility,
                            label = "浏览",
                            value = formatNumber(novel.viewCount)
                        )
                        StatItem(
                            icon = Icons.Default.SentimentSatisfied,
                            label = "点赞",
                            value = formatNumber(novel.likeCount)
                        )
                        StatItem(
                            icon = Icons.Default.Favorite,
                            label = "收藏",
                            value = formatNumber(novel.bookmarkCount)
                        )
                        StatItem(
                            icon = Icons.AutoMirrored.Filled.Comment,
                            label = "评论",
                            value = formatNumber(novel.commentCount)
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 简介
                    if (novel.description.isNotBlank()) {
                        val uriHandler = LocalUriHandler.current
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "简介",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HtmlText(
                                html = novel.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                onLinkClick = { url ->
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        // 忽略无法打开的链接
                                    }
                                }
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // 标签
                    if (novel.tags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "标签",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 年龄限制标签
                                if (novel.ageLimit == AgeLimit.R18) {
                                    TagChip(text = "R-18", isR18 = true)
                                } else if (novel.ageLimit == AgeLimit.R18G) {
                                    TagChip(text = "R-18G", isR18 = true)
                                }
                                
                                // 其他标签（不省略）
                                novel.tags.forEach { tag ->
                                    val isR18Tag = tag.name.equals("R-18", ignoreCase = true) || 
                                                   tag.name.equals("R-18G", ignoreCase = true)
                                    if (!isR18Tag) { // 避免重复显示
                                        TagChip(
                                            text = tag.translatedName ?: tag.name,
                                            isR18 = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // 作者信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 作者头像
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .then(
                                    if (onUserClick != null) {
                                        Modifier.clickable {
                                            novel.userId.toLongOrNull()?.let { onUserClick(it) }
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (novel.userProfileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = novel.userProfileImageUrl,
                                    contentDescription = novel.userName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = novel.userName,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 作者名
                        Text(
                            text = novel.userName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (onUserClick != null) {
                                        Modifier.clickable {
                                            novel.userId.toLongOrNull()?.let { onUserClick(it) }
                                        }
                                    } else Modifier
                                )
                        )
                        
                        // 关注按钮
                        FollowIndicator(
                            user = User(
                                id = novel.userId,
                                name = novel.userName,
                                profileImageUrl = novel.userProfileImageUrl,
                                followStatus = authorFollowStatus
                            ),
                            size = 28.dp
                        )
                    }
                    
                    // 发布时间
                    Text(
                        text = "发布时间: ${DateTimeFormatter.formatToLocalDateTime(novel.createdTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 统计项组件
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 标签芯片
 */
@Composable
private fun TagChip(
    text: String,
    isR18: Boolean,
    modifier: Modifier = Modifier
) {
    val r18Color = Color(0xFFFF4060)
    val backgroundColor = if (isR18) 
        r18Color.copy(alpha = 0.15f) 
    else 
        MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isR18) 
        r18Color 
    else 
        MaterialTheme.colorScheme.onSecondaryContainer
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 格式化数字
 */
private fun formatNumber(number: Int): String {
    return when {
        number >= 10000 -> String.format("%.1fw", number / 10000.0)
        number >= 1000 -> String.format("%.1fk", number / 1000.0)
        else -> number.toString()
    }
}

/**
 * 格式化阅读时间
 */
private fun formatReadingTime(minutes: Int): String {
    return when {
        minutes >= 60 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}小时${mins}分钟" else "${hours}小时"
        }
        minutes > 0 -> "${minutes}分钟"
        else -> "不到1分钟"
    }
}

/**
 * FlowRow 布局（如果项目中没有，使用此简化实现）
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
