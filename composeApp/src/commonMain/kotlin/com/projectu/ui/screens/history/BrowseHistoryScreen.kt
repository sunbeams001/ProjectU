package com.projectu.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.BrowseHistoryItem
import com.projectu.shared.domain.model.HistoryContentType
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*
import projectu.composeapp.generated.resources.Res
import java.text.SimpleDateFormat
import java.util.*

/**
 * 浏览历史页面
 */
class BrowseHistoryScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: BrowseHistoryViewModel = koinInject()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        BrowseHistoryContent(
            state = state,
            onIntent = viewModel::handleIntent,
            onHistoryItemClick = { item ->
                // 根据类型跳转到对应的详情页
                when (item.contentType) {
                    HistoryContentType.ILLUST,
                    HistoryContentType.MANGA,
                    HistoryContentType.UGOIRA -> {
                        navigator.push(ArtworkDetailScreen(artworkId = item.contentId))
                    }
                    HistoryContentType.NOVEL -> {
                        navigator.push(NovelDetailScreen(novelId = item.contentId))
                    }
                    HistoryContentType.NOVEL_SERIES -> {
                        navigator.push(NovelSeriesScreen(seriesId = item.contentId))
                    }
                    HistoryContentType.MANGA_SERIES -> {
                        navigator.push(MangaSeriesScreen(seriesId = item.contentId))
                    }
                }
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseHistoryContent(
    state: BrowseHistoryScreenState,
    onIntent: (BrowseHistoryIntent) -> Unit,
    onHistoryItemClick: (BrowseHistoryItem) -> Unit,
    onBackClick: () -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 筛选菜单按钮
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            HistoryFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(filter.getDisplayName())
                                            if (filter == state.selectedFilter) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onIntent(BrowseHistoryIntent.FilterByType(filter))
                                        showFilterMenu = false
                                    }
                                )
                            }
                            
                            HorizontalDivider()
                            
                            // 清空历史记录选项
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.history_clear), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showFilterMenu = false
                                    showClearDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            state.filteredHistoryItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.history_no_history),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = state.filteredHistoryItems,
                        key = { it.id }
                    ) { item ->
                        HistoryItemCard(
                            item = item,
                            onClick = { onHistoryItemClick(item) },
                            onDelete = { onIntent(BrowseHistoryIntent.DeleteHistoryItem(item.id)) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    
    // 清空历史记录确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(Res.string.history_clear)) },
            text = {
                Text(
                    if (state.selectedFilter == HistoryFilter.ALL) {
                        stringResource(Res.string.history_clear_all_confirm)
                    } else {
                        stringResource(Res.string.history_clear_type_confirm, state.selectedFilter.getDisplayName())
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(BrowseHistoryIntent.ClearHistoryByType(state.selectedFilter))
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.history_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(Res.string.history_cancel))
                }
            }
        )
    }
}

/**
 * 历史记录条目卡片
 */
@Composable
private fun HistoryItemCard(
    item: BrowseHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 缩略图
            if (item.thumbnailUrl != null) {
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                // 无缩略图时显示占位符
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.contentType) {
                                HistoryContentType.NOVEL, HistoryContentType.NOVEL_SERIES -> Icons.Default.MenuBook
                                else -> Icons.Default.Image
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 信息部分 - 使用Column配合weight确保时间和作者不重叠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 标题
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 内容类型标签
                        AssistChip(
                            onClick = {},
                            label = { Text(item.contentType.getDisplayName(), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                        
                        // R18 标签
                        if (item.isR18) {
                            AssistChip(
                                onClick = {},
                                label = { Text("R18", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        // AI 标签
                        if (item.isAi) {
                            AssistChip(
                                onClick = {},
                                label = { Text("AI", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    // 作者信息 - 增大与tag的间距
                    if (item.authorName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "by ${item.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 弹性空间，确保时间在底部且不与作者名重叠
                    Spacer(modifier = Modifier.weight(1f).heightIn(min = 4.dp))
                    
                    // 浏览时间 - 为右下角删除按钮留出空间
                    Text(
                        text = formatViewedTime(item.viewedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 40.dp)
                    )
                }
                
                // 删除按钮 - 固定在右下角
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.history_delete_item_title)) },
            text = { Text(stringResource(Res.string.history_delete_item_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text(stringResource(Res.string.history_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.history_cancel))
                }
            }
        )
    }
}

/**
 * 获取筛选类型的显示名称
 */
@Composable
private fun HistoryFilter.getDisplayName(): String {
    return when (this) {
        HistoryFilter.ALL -> stringResource(Res.string.history_filter_all)
        HistoryFilter.ILLUST -> stringResource(Res.string.history_filter_illustrations)
        HistoryFilter.MANGA -> stringResource(Res.string.history_filter_manga)
        HistoryFilter.UGOIRA -> stringResource(Res.string.history_filter_ugoira)
        HistoryFilter.NOVEL -> stringResource(Res.string.history_filter_novels)
        HistoryFilter.NOVEL_SERIES -> stringResource(Res.string.history_filter_novel_series)
        HistoryFilter.MANGA_SERIES -> stringResource(Res.string.history_filter_manga_series)
    }
}

/**
 * 获取内容类型的显示名称
 */
@Composable
private fun HistoryContentType.getDisplayName(): String {
    return when (this) {
        HistoryContentType.ILLUST -> stringResource(Res.string.history_type_illust)
        HistoryContentType.MANGA -> stringResource(Res.string.history_type_manga)
        HistoryContentType.UGOIRA -> stringResource(Res.string.history_type_ugoira)
        HistoryContentType.NOVEL -> stringResource(Res.string.history_type_novel)
        HistoryContentType.NOVEL_SERIES -> stringResource(Res.string.history_type_series)
        HistoryContentType.MANGA_SERIES -> stringResource(Res.string.history_type_series)
    }
}

/**
 * 格式化浏览时间
 */
@Composable
private fun formatViewedTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> stringResource(Res.string.history_time_just_now)
        diff < 3600_000 -> stringResource(Res.string.history_time_minutes_ago, (diff / 60_000).toString())
        diff < 86400_000 -> stringResource(Res.string.history_time_hours_ago, (diff / 3600_000).toString())
        diff < 604800_000 -> stringResource(Res.string.history_time_days_ago, (diff / 86400_000).toString())
        else -> {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
