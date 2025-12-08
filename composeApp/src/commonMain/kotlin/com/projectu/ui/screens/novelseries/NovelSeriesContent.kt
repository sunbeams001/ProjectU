package com.projectu.ui.screens.novelseries

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.util.DateTimeFormatter
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.util.formatNumber
import com.projectu.ui.util.formatReadingTime
import com.projectu.ui.components.HtmlText
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.htmlToPlainText
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 小说系列详情页内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelSeriesContent(
    state: NovelSeriesDetailState,
    onBackClick: () -> Unit,
    onToggleWatch: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryDetails: () -> Unit,
    onRetryContents: () -> Unit,
    onRefresh: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onUserClick: (String) -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState, state.novels.size, state.isLoadingContents, state.hasMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !state.isLoadingContents && state.hasMore && state.novels.isNotEmpty()) {
                onLoadMore()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.series?.title ?: stringResource(Res.string.novel_series),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                },
                actions = {
                    // 刷新按钮
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.nav_refresh)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 初次加载系列详情
                state.isLoadingSeries && state.series == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                // 系列详情加载失败
                state.seriesError != null && state.series == null -> {
                    ErrorDisplay(
                        message = state.seriesError,
                        onRetry = onRetryDetails,
                        modifier = Modifier.fillMaxSize(),
                        isFullScreen = true
                    )
                }
                
                // 正常显示
                state.series != null -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 系列详情卡片
                        item(key = "series_header") {
                            NovelSeriesHeaderCard(
                                series = state.series,
                                isWatchLoading = state.isWatchLoading,
                                onToggleWatch = onToggleWatch,
                                onUserClick = onUserClick,
                                onDownloadClick = onDownloadClick
                            )
                        }
                        
                        // 分隔线和标题
                        item(key = "content_divider") {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(Res.string.series_works_list, state.series.contentCount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // 内容加载错误
                        if (state.contentsError != null && state.novels.isEmpty()) {
                            item(key = "contents_error") {
                                ErrorDisplay(
                                    message = state.contentsError,
                                    onRetry = onRetryContents,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                        
                        // 小说列表 - 使用 NovelCard 组件
                        items(
                            items = state.novels,
                            key = { it.id }
                        ) { novel ->
                            NovelCard(
                                novel = novel,
                                onClick = { onNovelClick(novel) },
                                onUserClick = onUserClick,
                                onSeriesClick = null,
                                showSeriesInfo = false, // 已经在系列页面中，不需要显示系列信息
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        
                        // 加载更多指示器
                        if (state.isLoadingContents && state.novels.isNotEmpty()) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        
                        // 底部空间
                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 系列详情头部卡片
 */
@Composable
private fun NovelSeriesHeaderCard(
    series: NovelSeries,
    isWatchLoading: Boolean,
    onToggleWatch: () -> Unit,
    onUserClick: (String) -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // R-18颜色
    val r18Color = Color(0xFFFF4060)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上部：封面 + 基本信息
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
                    if (series.coverUrl != null) {
                        AsyncImage(
                            model = series.coverUrl,
                            contentDescription = series.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 默认占位
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
                    
                    // 完结/连载状态
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // 右侧信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 统计信息
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 篇数
                        Text(
                            text = stringResource(Res.string.series_total_episodes, series.contentCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 字数
                        series.totalCharacterCount?.let { count ->
                            if (count > 0) {
                                Text(
                                    text = stringResource(Res.string.series_word_count, formatWordCount(count)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 阅读时间 - 从秒转换
                    series.readingTimeSeconds?.let { seconds ->
                        if (seconds > 0) {
                            Text(
                                text = formatReadingTime(seconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 作者信息
                    Row(
                        modifier = Modifier
                            .clickable { 
                                onUserClick(series.userId)
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 作者头像
                        if (series.profileImageUrl != null) {
                            AsyncImage(
                                model = series.profileImageUrl,
                                contentDescription = series.userName,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // 作者名
                        Text(
                            text = series.userName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 标签行 - 显示全部标签
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // R-18 标签
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
                
                // AI 标签
                if (series.isAiGenerated) {
                    TagChip(
                        text = "AI",
                        textColor = MaterialTheme.colorScheme.tertiary,
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
                
                // 所有普通标签（不限制数量）
                series.tags.forEach { tag ->
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
            
            // 简介 - 支持展开/收起
            if (series.caption.isNotBlank()) {
                var isExpanded by remember { mutableStateOf(false) }
                val plainText = remember(series.caption) {
                    htmlToPlainText(series.caption)
                }
                // 判断是否需要展开功能（文本超过3行约60个字符时）
                val needsExpansion = plainText.length > 60 || plainText.count { it == '\n' } >= 2
                
                HtmlText(
                    html = series.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = if (isExpanded) null else 3,
                    overflow = TextOverflow.Ellipsis,
                    onClick = if (needsExpansion) {
                        { isExpanded = !isExpanded }
                    } else null
                )
            }
            
            // 更新时间 - 使用本地时区时间，显示到秒
            Text(
                text = stringResource(Res.string.series_last_update, DateTimeFormatter.formatToDetailedLocalDateTime(series.updateDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 追更按钮
            Button(
                onClick = onToggleWatch,
                enabled = !isWatchLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = if (series.isWatched) {
                    ButtonDefaults.outlinedButtonColors()
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isWatchLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (series.isWatched) 
                            Icons.Default.NotificationsOff 
                        else 
                            Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (series.isWatched) stringResource(Res.string.series_remove_watch) else stringResource(Res.string.series_add_watch)
                    )
                }
            }
            
            // 下载按钮
            if (onDownloadClick != null) {
                OutlinedButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "下载系列为 EPUB")
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
