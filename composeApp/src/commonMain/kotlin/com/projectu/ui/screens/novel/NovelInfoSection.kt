package com.projectu.ui.screens.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
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
import com.projectu.ui.util.formatNumber
import com.projectu.ui.util.formatReadingTime
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

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
 * @param markerStatus 书签状态
 * @param isMarkerLoading 书签操作是否正在加载
 * @param onToggle 切换展开/收缩回调
 * @param onCollapse 收起回调（用于下拉手势触发）
 * @param onMarkerClick 点击书签按钮回调
 * @param onUserClick 点击用户区域回调
 * @param onSeriesClick 点击系列回调
 * @param onCommentClick 点击评论回调
 * @param modifier 修饰符
 */
@Composable
fun NovelInfoSection(
    novel: Novel,
    authorFollowStatus: FollowStatus,
    isExpanded: Boolean,
    markerStatus: MarkerStatus = MarkerStatus.NO_MARKER,
    isMarkerLoading: Boolean = false,
    translatedDescription: String? = null,
    isTranslating: Boolean = false,
    onToggle: () -> Unit,
    onCollapse: () -> Unit = onToggle,
    onMarkerClick: () -> Unit = {},
    onUserClick: ((userId: String) -> Unit)? = null,
    onSeriesClick: ((seriesId: String) -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onRecommendClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onBlockTag: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onTranslateClick: (() -> Unit)? = null,
    onClearTranslation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val r18Color = Color(0xFFFF4060)
    val snackbarHostState = remember { SnackbarHostState() }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    // 三种状态：未添加、已添加当前页、已添加其他页
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
                            val (icon, contentDesc, tint) = when (markerStatus) {
                                MarkerStatus.NO_MARKER -> Triple(
                                    Icons.Default.BookmarkAdd,
                                    stringResource(Res.string.novel_add_marker),
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MarkerStatus.MARKER_CURRENT_PAGE -> Triple(
                                    Icons.Default.BookmarkAdded,
                                    stringResource(Res.string.novel_remove_marker),
                                    MaterialTheme.colorScheme.primary
                                )
                                MarkerStatus.MARKER_OTHER_PAGE -> Triple(
                                    Icons.Default.BookmarkBorder,
                                    stringResource(Res.string.novel_update_marker),
                                    MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = contentDesc,
                                tint = tint,
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
                        contentDescription = if (isExpanded) stringResource(Res.string.novel_collapse) else stringResource(Res.string.novel_expand),
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
            val scrollState = rememberScrollState()
            
            // 添加鼠标拖动支持（用于桌面平台）
            val dragModifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDrag = 0f
                    
                    drag(down.id) { change ->
                        // 只有在滚动到顶部时才处理下拉手势
                        if (scrollState.value == 0) {
                            val dragAmount = change.positionChange().y
                            val horizontalDrag = abs(change.positionChange().x)
                            val verticalDrag = abs(dragAmount)
                            
                            // 向下拖动且主要是垂直方向
                            if (dragAmount > 0 && verticalDrag > horizontalDrag * 0.5f) {
                                change.consume()
                                totalDrag += dragAmount
                            }
                        }
                    }
                    
                    // 拖动结束，判断是否超过阈值
                    if (totalDrag > 150f) { // 150px 阈值，与 NestedScrollConnection 一致
                        onCollapse()
                    }
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 可滚动区域（统计数据、标签、作品信息、简介）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(dragModifier)
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    // 1. 封面 + 基本信息
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
                                        novel.seriesId?.let { onSeriesClick?.invoke(it) }
                                    }
                                )
                            }
                            
                            // 字数和阅读时长
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "📝 " + stringResource(Res.string.novel_text_count, formatNumber(novel.textCount)),
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
                                    text = "📄 " + stringResource(Res.string.novel_page_count, novel.pageCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // 2. 作者信息 + 发布时间（整合在一起，参考作品详情页）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 作者头像
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (onUserClick != null) {
                                        Modifier.clickable {
                                            onUserClick(novel.userId)
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
                        
                        // 作者名和发布时间（占据剩余空间）
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (onUserClick != null) {
                                        Modifier.clickable {
                                            onUserClick(novel.userId)
                                        }
                                    } else Modifier
                                ),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 作者名
                            Text(
                                text = novel.userName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // 发布时间
                            Text(
                                text = DateTimeFormatter.formatToLocalDateTime(novel.createdTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        
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
                    
                    HorizontalDivider()
                    
                    // 3. 统计信息行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            icon = Icons.Default.Visibility,
                            label = stringResource(Res.string.novel_stat_views),
                            value = formatNumber(novel.viewCount)
                        )
                        StatItem(
                            icon = Icons.Default.SentimentSatisfied,
                            label = stringResource(Res.string.novel_stat_likes),
                            value = formatNumber(novel.likeCount)
                        )
                        StatItem(
                            icon = Icons.Default.Favorite,
                            label = stringResource(Res.string.novel_stat_bookmarks),
                            value = formatNumber(novel.bookmarkCount)
                        )
                        StatItem(
                            icon = Icons.AutoMirrored.Filled.Comment,
                            label = stringResource(Res.string.novel_stat_comments),
                            value = formatNumber(novel.commentCount),
                            onClick = onCommentClick
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 4. 标签
                    if (novel.tags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(Res.string.novel_tags),
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
                                val clipboardManager = LocalClipboardManager.current
                                val scope = rememberCoroutineScope()
                                
                                novel.tags.forEach { tag ->
                                    val isR18Tag = tag.name.equals("R-18", ignoreCase = true) || 
                                                   tag.name.equals("R-18G", ignoreCase = true)
                                    if (!isR18Tag) { // 避免重复显示
                                        var showTagMenu by remember { mutableStateOf(false) }
                                        val copiedMessage = stringResource(Res.string.common_copied, tag.name)
                                        
                                        Box {
                                            TagChip(
                                                text = tag.translatedName ?: tag.name,
                                                isR18 = false,
                                                onClick = { onTagClick?.invoke(tag) },
                                                onLongClick = { showTagMenu = true }
                                            )
                                            
                                            DropdownMenu(
                                                expanded = showTagMenu,
                                                onDismissRequest = { showTagMenu = false }
                                            ) {
                                                // 复制标签
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(Res.string.action_copy)) },
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(tag.name))
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = copiedMessage
                                                            )
                                                        }
                                                        showTagMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                                
                                                // 屏蔽标签
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(Res.string.action_block_tag)) },
                                                    onClick = {
                                                        onBlockTag?.invoke(tag)
                                                        showTagMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Block,
                                                            contentDescription = null
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // 5. 作品信息（ID等）
                    val clipboardManager = LocalClipboardManager.current
                    val coroutineScope = rememberCoroutineScope()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val novelIdCopiedMessage = stringResource(Res.string.id_copied, novel.id)
                        val userIdCopiedMessage = stringResource(Res.string.id_copied, novel.userId)
                        
                        Text(
                            text = stringResource(Res.string.novel_info),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // 小说ID（可点击复制）
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(novel.id))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = novelIdCopiedMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.novel_id),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = novel.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.action_copy),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // 用户ID（可点击复制）
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(novel.userId))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = userIdCopiedMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.novel_user_id),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = novel.userId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.action_copy),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // 6. 简介
                    if (novel.description.isNotBlank()) {
                        val uriHandler = LocalUriHandler.current
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.novel_description),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // 翻译按钮
                                if (onTranslateClick != null && onClearTranslation != null) {
                                    IconButton(
                                        onClick = { 
                                            if (translatedDescription == null) {
                                                onTranslateClick()
                                            } else {
                                                onClearTranslation()
                                            }
                                        },
                                        enabled = !isTranslating
                                    ) {
                                        if (isTranslating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (translatedDescription == null) 
                                                    Icons.Default.Translate 
                                                else 
                                                    Icons.Default.Close,
                                                contentDescription = if (translatedDescription == null) "Translate" else "Close",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // 原文
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
                            
                            // 翻译结果
                            if (translatedDescription != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                
                                Text(
                                    text = stringResource(Res.string.common_translation_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                
                                HtmlText(
                                    html = translatedDescription,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    }
                    
                    // 固定在底部的操作按钮区域（不在滚动区域内）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // 操作按钮行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 推荐作品按钮
                            NovelActionButton(
                                icon = Icons.Default.Recommend,
                                label = stringResource(Res.string.novel_recommend_title),
                                onClick = { onRecommendClick?.invoke() },
                                enabled = onRecommendClick != null
                            )
                            
                            // 下载按钮
                            NovelActionButton(
                                icon = Icons.Default.Download,
                                label = stringResource(Res.string.download_novel),
                                onClick = { onDownloadClick?.invoke() },
                                enabled = onDownloadClick != null
                            )
                        }
                    }
            }
        }
    }
        }
        
        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
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
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
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
        modifier = modifier.then(
            if (onClick != null || onLongClick != null) {
                Modifier.combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() }
                )
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
 * 操作按钮组件（小说专用）
 */
@Composable
private fun NovelActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
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
