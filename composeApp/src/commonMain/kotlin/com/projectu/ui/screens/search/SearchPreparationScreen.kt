@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.projectu.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import coil3.compose.AsyncImage
import com.projectu.shared.data.remote.dto.tag.PopularTag
import com.projectu.shared.data.remote.dto.tag.SearchSuggestionBody
import com.projectu.shared.data.remote.dto.tag.ThumbnailInfo
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.util.rememberCopyToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 搜索准备页面
 * 包含搜索框、搜索历史、搜索建议和标签自动补全
 */
class SearchPreparationScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SearchPreparationViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        
        val thumbnailClickHandler = remember(navigator) {
            val handler: (ThumbnailInfo) -> Unit = { thumbnail ->
                // 根据 illustType 判断导航到插画还是小说详情页
                when (thumbnail.illustType) {
                    3 -> navigator.push(NovelDetailScreen(novelId = thumbnail.id))
                    else -> navigator.push(ArtworkDetailScreen(artworkId = thumbnail.id))
                }
            }
            handler
        }
        
        SearchPreparationContent(
            state = state,
            onSearchKeywordChange = viewModel::onSearchKeywordChange,
            onSearch = {
                scope.launch {
                    val keyword = viewModel.performSearch()
                    if (keyword != null) {
                        navigator.push(SearchResultScreen(keyword))
                    }
                }
            },
            onAutocompleteSuggestionClick = viewModel::onAutocompleteSuggestionClick,
            onHistoryClick = viewModel::onHistoryClick,
            onRecommendationTagClick = viewModel::onRecommendationTagClick,
            onClearHistory = viewModel::clearHistory,
            onRefreshRecommendations = viewModel::refreshRecommendations,
            onThumbnailClick = thumbnailClickHandler,
            onRemoveHistory = viewModel::removeHistory,
            onTogglePin = viewModel::togglePinHistory
        )
    }
}

@Composable
fun SearchPreparationContent(
    state: SearchPreparationState,
    onSearchKeywordChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onAutocompleteSuggestionClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    onHistoryClick: (String) -> Unit,
    onRecommendationTagClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    onClearHistory: () -> Unit,
    onRefreshRecommendations: () -> Unit,
    onThumbnailClick: (ThumbnailInfo) -> Unit,
    onRemoveHistory: (String) -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stableOnThumbnailClick = remember(onThumbnailClick) { onThumbnailClick }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 搜索输入栏
        Box {
            SearchInputBar(
                searchText = state.searchKeyword,
                onSearchTextChange = onSearchKeywordChange,
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 浮动的标签自动补全
            FloatingAutocompleteSuggestions(
                searchText = state.searchKeyword.text,
                suggestions = state.autocompleteSuggestions,
                onSuggestionClick = onAutocompleteSuggestionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 72.dp)
            )
        }
        
        HorizontalDivider()
        
        // 搜索历史和建议区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 搜索历史区域 (1/3 高度)
            SearchHistorySection(
                history = state.searchHistory,
                onHistoryClick = onHistoryClick,
                onClearHistory = onClearHistory,
                onRemoveHistory = onRemoveHistory,
                onTogglePin = onTogglePin,
                modifier = Modifier.fillMaxWidth()
            )
            
            HorizontalDivider()
            
            // 搜索建议区域 (2/3 高度，可滚动)
            SearchRecommendationsSection(
                myFavoriteTags = state.myFavoriteTags,
                popularIllustTags = state.popularIllustTags,
                popularNovelTags = state.popularNovelTags,
                recommendedTags = state.recommendedTags,
                bookmarkRecommendedTags = state.bookmarkRecommendedTags,
                thumbnails = state.searchRecommendations?.thumbnails ?: emptyList(),
                popularIllustRaw = state.searchRecommendations?.popularTags?.illust ?: emptyList(),
                popularNovelRaw = state.searchRecommendations?.popularTags?.novel ?: emptyList(),
                recommendedRaw = state.searchRecommendations?.recommendTags?.illust ?: emptyList(),
                bookmarkRecommendedRaw = state.searchRecommendations?.recommendByTags?.illust ?: emptyList(),
                isLoading = state.isLoadingRecommendations,
                error = state.recommendationsError,
                onTagClick = onRecommendationTagClick,
                onThumbnailClick = stableOnThumbnailClick,
                onRefresh = onRefreshRecommendations,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 搜索输入栏
 */
@Composable
fun SearchInputBar(
    searchText: androidx.compose.ui.text.input.TextFieldValue,
    onSearchTextChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(Res.string.search_input_hint)) },
        trailingIcon = {
            Row {
                if (searchText.text.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange(androidx.compose.ui.text.input.TextFieldValue("")) }) {
                        Icon(Icons.Default.Clear, stringResource(Res.string.search_clear))
                    }
                }
                IconButton(
                    onClick = onSearch,
                    enabled = searchText.text.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(Res.string.search_button)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}

/**
 * 浮动的标签自动补全列表
 */
@Composable
fun FloatingAutocompleteSuggestions(
    searchText: String,
    suggestions: List<com.projectu.shared.domain.model.Tag>,
    onSuggestionClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldShow = remember(searchText, suggestions) {
        searchText.isNotBlank() && 
        !searchText.endsWith(" ") && 
        suggestions.isNotEmpty()
    }
    
    if (shouldShow) {
        Card(
            modifier = modifier.heightIn(max = 200.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn {
                items(suggestions) { tag ->
                    ListItem(
                        headlineContent = { 
                            // 优先显示翻译，如果没有翻译则显示原始名
                            Text(tag.translatedName ?: tag.name) 
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.clickable { onSuggestionClick(tag) }
                    )
                }
            }
        }
    }
}

/**
 * 搜索历史区域
 */
@Composable
fun SearchHistorySection(
    history: List<com.projectu.shared.data.local.SearchHistoryItem>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveHistory: (String) -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.search_history_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text(stringResource(Res.string.search_history_clear))
                }
            }
        }
        
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.search_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 使用 FlowRow 展示历史标签
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { item ->
                    EnhancedHistoryChip(
                        item = item,
                        onClick = { onHistoryClick(item.keyword) },
                        onRemove = { onRemoveHistory(item.keyword) },
                        onTogglePin = { onTogglePin(item.keyword) }
                    )
                }
            }
        }
    }
}

/**
 * 增强版搜索历史标签芯片
 * 支持固定状态显示和长按操作
 */
@Composable
fun EnhancedHistoryChip(
    item: com.projectu.shared.data.local.SearchHistoryItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val copyToClipboard = rememberCopyToClipboard()
    
    // 使用 Surface 代替 SuggestionChip，完全控制点击行为
    // 注意：不能同时使用 Surface.onClick 和 combinedClickable，会冲突
    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { 
                showMenu = true
            }
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), // SuggestionChip 的圆角
        color = MaterialTheme.colorScheme.secondaryContainer, // SuggestionChip 的背景色
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f) // 淡边框
        )
    ) {
        Row(
            // 根据是否有图标调整内边距
            modifier = Modifier.padding(
                start = if (item.isPinned) 8.dp else 12.dp,  // 有左侧图标时减少左内边距
                end = if (!item.isPinned) 8.dp else 12.dp,   // 有右侧图标时减少右内边距
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧图钉图标（固定时显示）
            if (item.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            
            // 关键词文本
            Text(
                text = item.keyword,
                style = MaterialTheme.typography.labelLarge
            )
            
            // 右侧删除图标（非固定时显示）
            if (!item.isPinned) {
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = { onRemove() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text(stringResource(Res.string.search_history_copy))
                    }
                },
                onClick = {
                    copyToClipboard(item.keyword)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                        Text(if (item.isPinned) stringResource(Res.string.search_history_unpin) else stringResource(Res.string.search_history_pin))
                    }
                },
                onClick = {
                    onTogglePin()
                    showMenu = false
                }
            )
        }
    }
}

/**
 * 搜索建议区域
 */
@Composable
fun SearchRecommendationsSection(
    myFavoriteTags: List<com.projectu.shared.domain.model.Tag>,
    popularIllustTags: List<com.projectu.shared.domain.model.Tag>,
    popularNovelTags: List<com.projectu.shared.domain.model.Tag>,
    recommendedTags: List<com.projectu.shared.domain.model.Tag>,
    bookmarkRecommendedTags: List<com.projectu.shared.domain.model.Tag>,
    thumbnails: List<com.projectu.shared.data.remote.dto.tag.ThumbnailInfo>,
    popularIllustRaw: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    popularNovelRaw: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    recommendedRaw: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    bookmarkRecommendedRaw: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    isLoading: Boolean,
    error: String?,
    onTagClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    onThumbnailClick: (ThumbnailInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.search_recommendations_title),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, stringResource(Res.string.search_recommendations_refresh))
            }
        }
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                ErrorDisplay(
                    message = error,
                    onRetry = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    isFullScreen = false
                )
            }
            else -> {
                // 我的收藏标签（纯标签芯片）
                if (myFavoriteTags.isNotEmpty()) {
                    SimpleTagSection(
                        title = stringResource(Res.string.search_recommendations_my_favorite),
                        tags = myFavoriteTags,
                        onTagClick = onTagClick
                    )
                }
                
                // 热门标签 - 插画（标签+作品预览）
                if (popularIllustTags.isNotEmpty() && popularIllustRaw.isNotEmpty()) {
                    TagWithArtworkSection(
                        title = stringResource(Res.string.search_recommendations_popular_illust),
                        tags = popularIllustTags,
                        tagRawData = popularIllustRaw,
                        thumbnails = thumbnails,
                        onTagClick = onTagClick,
                        onArtworkClick = onThumbnailClick
                    )
                }
                
                // 热门标签 - 小说（标签+作品预览）
                if (popularNovelTags.isNotEmpty() && popularNovelRaw.isNotEmpty()) {
                    TagWithArtworkSection(
                        title = stringResource(Res.string.search_recommendations_popular_novel),
                        tags = popularNovelTags,
                        tagRawData = popularNovelRaw,
                        thumbnails = thumbnails,
                        onTagClick = onTagClick,
                        onArtworkClick = onThumbnailClick
                    )
                }
                
                // 推荐标签（标签+作品预览）
                if (recommendedTags.isNotEmpty() && recommendedRaw.isNotEmpty()) {
                    TagWithArtworkSection(
                        title = stringResource(Res.string.search_recommendations_recommended),
                        tags = recommendedTags,
                        tagRawData = recommendedRaw,
                        thumbnails = thumbnails,
                        onTagClick = onTagClick,
                        onArtworkClick = onThumbnailClick
                    )
                }
                
                // 基于收藏的推荐标签（标签+作品列表）
                if (bookmarkRecommendedTags.isNotEmpty() && bookmarkRecommendedRaw.isNotEmpty()) {
                    TagWithArtworkGridSection(
                        title = stringResource(Res.string.search_recommendations_bookmark),
                        tags = bookmarkRecommendedTags,
                        tagRawData = bookmarkRecommendedRaw,
                        thumbnails = thumbnails,
                        onTagClick = onTagClick,
                        onArtworkClick = onThumbnailClick
                    )
                }
            }
        }
    }
}

/**
 * 简单标签区域（纯标签芯片）
 */
@Composable
private fun SimpleTagSection(
    title: String,
    tags: List<com.projectu.shared.domain.model.Tag>,
    onTagClick: (com.projectu.shared.domain.model.Tag) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = { onTagClick(tag) },
                    label = { 
                        // 优先显示翻译，如果没有翻译则显示原始名
                        Text(tag.translatedName ?: tag.name) 
                    }
                )
            }
        }
    }
}

/**
 * 缩略图预览区域
 */
@Composable
private fun ThumbnailPreviewSection(
    thumbnails: List<ThumbnailInfo>,
    onThumbnailClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.search_recommendations_thumbnails),
            style = MaterialTheme.typography.titleSmall
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(thumbnails.size) { index ->
                val thumbnail = thumbnails[index]
                ThumbnailCard(
                    thumbnail = thumbnail,
                    onClick = { onThumbnailClick(thumbnail.id) }
                )
            }
        }
    }
}

/**
 * 缩略图卡片
 */
@Composable
private fun ThumbnailCard(
    thumbnail: ThumbnailInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = thumbnail.url,
                contentDescription = thumbnail.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = thumbnail.title,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 标签+单个作品预览区域（用于热门标签和推荐标签）
 * 3列网格布局，每个格子显示作品方形缩略图+标签
 */
@Composable
private fun TagWithArtworkSection(
    title: String,
    tags: List<com.projectu.shared.domain.model.Tag>,
    tagRawData: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    thumbnails: List<com.projectu.shared.data.remote.dto.tag.ThumbnailInfo>,
    onTagClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    onArtworkClick: (com.projectu.shared.data.remote.dto.tag.ThumbnailInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // 为每个标签创建索引映射
        val tagMap = tagRawData.associateBy { it.tag }
        val thumbnailMap = thumbnails.associateBy { it.id }
        
        // 过滤出有效数据
        val validItems = tags.mapNotNull { tag ->
            val rawTag = tagMap[tag.name]
            val firstArtworkId = rawTag?.ids?.firstOrNull()
            val thumbnail = firstArtworkId?.let { thumbnailMap[it] }
            if (thumbnail != null) {
                Pair(tag, thumbnail)
            } else null
        }
        
        // 使用设置中的列数配置
        val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
        val columns by settingsCache.staggeredGridColumns.collectAsState()
        
        // 网格布局 - 完全贴边
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 5000.dp), // 使用 heightIn 而不是固定 height，允许内容自适应
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(0.dp), // 完全贴边
            userScrollEnabled = false // 禁用独立滚动，跟随外层滚动
        ) {
            items(validItems.size) { index ->
                val (tag, thumbnail) = validItems[index]
                SquareArtworkWithTag(
                    tag = tag,
                    thumbnail = thumbnail,
                    onTagClick = { onTagClick(tag) },
                    onArtworkClick = onArtworkClick
                )
            }
        }
    }
}

/**
 * 方形作品+底部标签
 */
@Composable
private fun SquareArtworkWithTag(
    tag: com.projectu.shared.domain.model.Tag,
    thumbnail: com.projectu.shared.data.remote.dto.tag.ThumbnailInfo,
    onTagClick: () -> Unit,
    onArtworkClick: (com.projectu.shared.data.remote.dto.tag.ThumbnailInfo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = { onArtworkClick(thumbnail) })
    ) {
        // 作品缩略图 - 填充整个方形区域
        com.projectu.ui.components.RetryableAsyncImage(
            model = thumbnail.url,
            contentDescription = thumbnail.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // 标签文字（底部）
        Text(
            text = tag.translatedName ?: tag.name,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clickable(onClick = onTagClick),
            style = MaterialTheme.typography.labelMedium,
            color = androidx.compose.ui.graphics.Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 标签+作品网格区域（用于基于收藏的推荐）
 * 每个标签下方显示3列瀑布流作品列表
 */
@Composable
private fun TagWithArtworkGridSection(
    title: String,
    tags: List<com.projectu.shared.domain.model.Tag>,
    tagRawData: List<com.projectu.shared.data.remote.dto.tag.PopularTag>,
    thumbnails: List<com.projectu.shared.data.remote.dto.tag.ThumbnailInfo>,
    onTagClick: (com.projectu.shared.domain.model.Tag) -> Unit,
    onArtworkClick: (com.projectu.shared.data.remote.dto.tag.ThumbnailInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        val tagMap = tagRawData.associateBy { it.tag }
        val thumbnailMap = thumbnails.associateBy { it.id }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            tags.forEach { tag ->
                val rawTag = tagMap[tag.name]
                val artworkIds = rawTag?.ids ?: emptyList()
                val artworkThumbnails = artworkIds.mapNotNull { thumbnailMap[it] }
                
                if (artworkThumbnails.isNotEmpty()) {
                    TagWithArtworkWaterfallGrid(
                        tag = tag,
                        thumbnails = artworkThumbnails,
                        onTagClick = { onTagClick(tag) },
                        onArtworkClick = onArtworkClick
                    )
                }
            }
        }
    }
}

/**
 * 标签+作品瀑布流网格（3列）
 */
@Composable
private fun TagWithArtworkWaterfallGrid(
    tag: com.projectu.shared.domain.model.Tag,
    thumbnails: List<com.projectu.shared.data.remote.dto.tag.ThumbnailInfo>,
    onTagClick: () -> Unit,
    onArtworkClick: (ThumbnailInfo) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标签芯片
        SuggestionChip(
            onClick = onTagClick,
            label = { 
                Text(
                    text = tag.translatedName ?: tag.name,
                    style = MaterialTheme.typography.labelLarge
                ) 
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // 转换ThumbnailInfo为Artwork，并创建映射
        val artworkToThumbnailMap = thumbnails.associateBy { it.id }
        val artworks = thumbnails.map { it.toArtwork() }
        
        // 使用设置中的列数配置
        val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
        val columns by settingsCache.staggeredGridColumns.collectAsState()
        
        val itemsPerColumn = artworks.chunked((artworks.size + columns - 1) / columns)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 为每列创建一个Column
            repeat(columns) { columnIndex ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsPerColumn.getOrNull(columnIndex)?.forEach { artwork ->
                        com.projectu.ui.components.ArtworkCard(
                            artwork = artwork,
                            onClick = { 
                                val thumbnail = artworkToThumbnailMap[artwork.id]
                                if (thumbnail != null) {
                                    onArtworkClick(thumbnail)
                                }
                            },
                            showUserInfo = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * 将ThumbnailInfo转换为Artwork（简化版本，仅用于展示）
 */
private fun com.projectu.shared.data.remote.dto.tag.ThumbnailInfo.toArtwork(): com.projectu.shared.domain.model.Artwork {
    return com.projectu.shared.domain.model.Artwork(
        id = this.id,
        title = this.title,
        description = this.description,
        type = com.projectu.shared.domain.model.ArtworkType.fromIllustType(this.illustType),
        imageUrls = com.projectu.shared.domain.model.ArtworkImageUrls(
            pages = listOf(
                com.projectu.shared.domain.model.PageImageUrls(
                    page = 0,
                    urls = com.projectu.shared.domain.model.ImageUrls(
                        squareMedium = this.url,
                        large = this.url,
                        master1200 = this.url,
                        original = this.url
                    ),
                    width = this.width,
                    height = this.height
                )
            )
        ),
        width = this.width,
        height = this.height,
        pageCount = this.pageCount,
        userId = this.userId,
        userName = this.userName,
        userProfileImageUrl = this.profileImageUrl,
        tags = emptyList(),
        viewCount = 0,
        likeCount = 0,
        bookmarkCount = 0,
        commentCount = 0,
        createdTime = this.createDate,
        totalView = 0,
        totalBookmarks = 0,
        ageLimit = when (this.xRestrict) {
            1 -> com.projectu.shared.domain.model.AgeLimit.R18
            2 -> com.projectu.shared.domain.model.AgeLimit.R18G
            else -> com.projectu.shared.domain.model.AgeLimit.ALL_AGE
        }
    )
}
