package com.projectu.ui.screens.artwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import com.projectu.shared.util.DateTimeFormatter
import com.projectu.ui.components.BookmarkIndicator
import com.projectu.ui.components.FollowIndicator
import com.projectu.ui.components.HtmlText
import com.projectu.ui.util.formatNumber
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 作品基础信息区域
 * 标题、作者、收藏等 - 始终可见，支持上滑和下滑手势
 * 
 * @param artwork 作品对象
 * @param authorFollowStatus 作者关注状态
 * @param onUserClick 点击用户区域回调
 * @param onSeriesClick 点击系列回调
 * @param onDragDelta 拖动增量回调（支持上滑和下滑）
 * @param onDragEnd 拖动结束回调
 * @param modifier 修饰符
 */
@Composable
fun ArtworkBasicInfoSection(
    artwork: Artwork,
    authorFollowStatus: FollowStatus,
    onUserClick: ((userId: String) -> Unit)? = null,
    onSeriesClick: ((seriesId: String) -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        onDragEnd?.invoke()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        // dragAmount: 下滑为正，上滑为负
                        onDragDelta?.invoke(dragAmount)
                    }
                )
            },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 第一行：标题 + 收藏按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 标题区域（占据剩余空间）
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 标题
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 系列信息（如果有）
                    val seriesId = artwork.seriesId
                    val seriesTitle = artwork.seriesTitle
                    if (seriesId != null && seriesTitle != null) {
                        Text(
                            text = seriesTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.then(
                                if (onSeriesClick != null) {
                                    Modifier.clickable {
                                        onSeriesClick(seriesId)
                                    }
                                } else Modifier
                            )
                        )
                    }
                }

                // 收藏状态指示器（内置收藏逻辑）
                BookmarkIndicator(
                    artwork = artwork,
                    size = 28.dp
                )
            }

            // 第二行：作者信息 + 关注按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 作者头像 - 如果URL为空，显示默认图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (onUserClick != null) {
                                Modifier.clickable {
                                    onUserClick(artwork.userId)
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork.userProfileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = artwork.userProfileImageUrl,
                            contentDescription = artwork.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 显示默认头像图标
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
                                    contentDescription = artwork.userName,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 作者名和投稿时间（占据剩余空间）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onUserClick != null) {
                                Modifier.clickable {
                                    onUserClick(artwork.userId)
                                }
                            } else Modifier
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 作者名
                    Text(
                        text = artwork.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 投稿时间
                    Text(
                        text = DateTimeFormatter.formatToLocalDateTime(artwork.createdTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // 关注状态指示器（内置关注逻辑）
                FollowIndicator(
                    user = User(
                        id = artwork.userId,
                        name = artwork.userName,
                        profileImageUrl = artwork.userProfileImageUrl,
                        followStatus = authorFollowStatus
                    ),
                    size = 28.dp
                )
            }
        }
    }
}

/**
 * 作品详情信息区域
 * 标签、简介、统计等 - 固定在屏幕底部，高度为屏幕一半
 * 
 * @param artwork 作品对象
 * @param onCommentClick 点击评论回调
 * @param onSimilarClick 点击推荐相似作品回调
 * @param onDownloadClick 点击下载回调
 * @param onScrollAtTop 滚动到顶部时的下滑回调
 * @param modifier 修饰符
 */
@Composable
fun ArtworkDetailInfoSection(
    artwork: Artwork,
    onCommentClick: (() -> Unit)? = null,
    onSimilarClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDownloadLongClick: (() -> Unit)? = null,
    onScrollAtTop: ((Float) -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // 嵌套滚动连接，用于处理滚动到顶部时的下滑手势
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 检查是否滚动到顶部且尝试向下滚动
                if (scrollState.value == 0 && available.y > 0) {
                    // 滚动到顶且下滑，触发收起基础信息区域
                    onScrollAtTop?.invoke(available.y)
                    // 消费这个滚动事件
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 可滚动区域（统计数据、标签、作品信息、简介）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 统计信息行（浏览+点赞+收藏+评论）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(
                        icon = Icons.Default.Visibility,
                        label = stringResource(Res.string.artwork_stat_views),
                        value = formatNumber(artwork.viewCount)
                    )
                    StatItem(
                        icon = Icons.Default.SentimentSatisfied,
                        label = stringResource(Res.string.artwork_stat_likes),
                        value = formatNumber(artwork.likeCount)
                    )
                    StatItem(
                        icon = Icons.Default.Favorite,
                        label = stringResource(Res.string.artwork_stat_bookmarks),
                        value = formatNumber(artwork.bookmarkCount)
                    )
                    StatItem(
                        icon = Icons.AutoMirrored.Filled.Comment,
                        label = stringResource(Res.string.artwork_stat_comments),
                        value = formatNumber(artwork.commentCount),
                        onClick = onCommentClick
                    )
                }
                
                HorizontalDivider()
                
                // 2. 标签
                if (artwork.tags.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.artwork_tags),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 年龄限制标签
                            if (artwork.ageLimit == AgeLimit.R18) {
                                TagChip(text = "R-18", isR18 = true)
                            } else if (artwork.ageLimit == AgeLimit.R18G) {
                                TagChip(text = "R-18G", isR18 = true)
                            }
                            
                            // 其他标签
                            artwork.tags.forEach { tag ->
                                val isR18Tag = tag.name.equals("R-18", ignoreCase = true) ||
                                        tag.name.equals("R-18G", ignoreCase = true)
                                if (!isR18Tag) { // 避免重复显示
                                    TagChip(
                                        text = tag.translatedName ?: tag.name,
                                        isR18 = false,
                                        onClick = onTagClick?.let { { it(tag) } }
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // 3. 作品信息（分辨率、ID等）
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.artwork_info),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // 分辨率
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.artwork_resolution),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${artwork.width} × ${artwork.height}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 页数（多页作品显示）
                    if (artwork.pageCount > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.artwork_page_count),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = artwork.pageCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // 作品ID
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.artwork_id),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = artwork.id,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 用户ID
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.artwork_user_id),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = artwork.userId,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                HorizontalDivider()
                
                // 4. 简介
                if (artwork.description.isNotBlank()) {
                    val uriHandler = LocalUriHandler.current
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.artwork_description),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HtmlText(
                            html = artwork.description,
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
            }
            
            // 固定在底部的操作按钮区域
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 5. 操作按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 推荐相似作品按钮
                        ActionButton(
                            icon = Icons.Default.Recommend,
                            label = stringResource(Res.string.artwork_similar),
                            onClick = { onSimilarClick?.invoke() },
                            enabled = onSimilarClick != null
                        )
                        
                        // 下载按钮（点击下载GIF，长按下载MP4）
                        ActionButton(
                            icon = Icons.Default.Download,
                            label = stringResource(Res.string.artwork_download),
                            onClick = { onDownloadClick?.invoke() },
                            onLongClick = { onDownloadLongClick?.invoke() },
                            enabled = onDownloadClick != null
                        )
                    }
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
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
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
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
 * 操作按钮组件
 */
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (enabled) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

/**
 * FlowRow 布局
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
