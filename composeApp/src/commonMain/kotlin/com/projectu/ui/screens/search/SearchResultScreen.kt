package com.projectu.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.UserCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.util.rememberTagClickHandler
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.util.UUID

/**
 * 搜索结果页面
 * @param initialKeyword 初始搜索关键词
 * @param uniqueId 页面唯一标识，默认生成UUID确保每次都是新实例
 */
data class SearchResultScreen(
    val initialKeyword: String,
    private val uniqueId: String = UUID.randomUUID().toString()
) : Screen {
    
    // 覆盖 key 属性，确保每个实例都是唯一的
    override val key: ScreenKey = uniqueId
    
    companion object {
        // 滚动位置记录，用于从详情页返回时恢复位置
        // 使用 companion object 避免在页面重组时丢失状态
        private val scrollIndicesMap = mutableMapOf<String, SnapshotStateMap<String, Int>>()
    }
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SearchResultViewModel> {
            parametersOf(initialKeyword)
        }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 创建Tag点击处理器
        val tagClickHandler = rememberTagClickHandler(navigator)
        
        // 为当前搜索关键词创建独立的 scrollIndices
        val scrollIndices = remember(initialKeyword) {
            scrollIndicesMap.getOrPut(initialKeyword) { mutableStateMapOf() }
        }
        
        SearchResultContent(
            state = state,
            onSearchKeywordChange = viewModel::onSearchKeywordChange,
            onSearch = viewModel::performSearch,
            onAutocompleteSuggestionClick = viewModel::onAutocompleteSuggestionClick,
            onCategoryChange = viewModel::onCategoryChange,
            onToggleFilterDrawer = viewModel::toggleFilterDrawer,
            onLoadMore = viewModel::loadMore,
            onArtworkClick = { artwork, index, category ->
                // 根据当前分类创建对应的列表数据源
                val (listSource, artworkIds, scrollKey) = when (category) {
                    SearchCategory.ILLUST -> Triple(
                        viewModel.createArtworkListSource(),
                        state.illustResults.map { it.id },
                        "illust"
                    )
                    SearchCategory.USER -> Triple(
                        viewModel.createUserArtworkListSource(),
                        state.userResults.flatMap { user -> user.illusts.map { it.id } },
                        "user"
                    )
                    else -> Triple(
                        viewModel.createArtworkListSource(),
                        emptyList(),
                        "other"
                    )
                }
                
                // 创建导航上下文，返回时恢复滚动位置
                val contextKey = NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { returnedIndex ->
                        scrollIndices[scrollKey] = returnedIndex
                    }
                )
                
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = artworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onNovelClick = { novel ->
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onSeriesClick = { seriesId ->
                navigator.push(NovelSeriesScreen(seriesId = seriesId))
            },
            onUserClick = { user ->
                navigator.push(UserScreen(userId = user.id))
            },
            onTagClick = tagClickHandler::handleTagClick,
            onUpdateIllustParams = viewModel::updateIllustParams,
            onUpdateNovelParams = viewModel::updateNovelParams,
            onUpdateUserParams = viewModel::updateUserParams,
            onRefresh = viewModel::refresh,
            onBack = { navigator.pop() },
            scrollIndices = scrollIndices
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchResultContent(
    state: SearchResultState,
    onSearchKeywordChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onAutocompleteSuggestionClick: (Tag) -> Unit,
    onCategoryChange: (SearchCategory) -> Unit,
    onToggleFilterDrawer: () -> Unit,
    onLoadMore: () -> Unit,
    onArtworkClick: (Artwork, Int, SearchCategory) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (User) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onUpdateIllustParams: (IllustSearchParams) -> Unit,
    onUpdateNovelParams: (NovelSearchParams) -> Unit,
    onUpdateUserParams: (UserSearchParams) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    scrollIndices: SnapshotStateMap<String, Int> = mutableStateMapOf(),
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentCategory.ordinal,
        pageCount = { SearchCategory.entries.size }
    )
    val scope = rememberCoroutineScope()
    
    // 为每个分类预先创建独立的列表状态
    val illustListState = rememberLazyStaggeredGridState()
    val novelListState = rememberLazyListState()
    val userListState = rememberLazyListState()
    
    // 监听筛选条件变更，重置滚动位置
    LaunchedEffect(state.illustResults.isEmpty(), state.novelResults.isEmpty(), state.userResults.isEmpty()) {
        // 当所有列表都为空时（筛选条件变更后清空数据），重置所有滚动位置
        if (state.illustResults.isEmpty() && state.novelResults.isEmpty() && state.userResults.isEmpty()) {
            illustListState.scrollToItem(0)
            novelListState.scrollToItem(0)
            userListState.scrollToItem(0)
        }
    }
    
    // 从详情页返回时恢复插画列表滚动位置
    val scrollToIndexIllust by remember {
        derivedStateOf { scrollIndices["illust"] }
    }
    
    LaunchedEffect(scrollToIndexIllust) {
        scrollToIndexIllust?.let { index ->
            if (index >= 0 && index < state.illustResults.size) {
                illustListState.animateScrollToItem(index)
                scrollIndices.remove("illust") // 滚动完成后清除索引
            }
        }
    }
    
    // 从详情页返回时恢复用户tab滚动位置
    val scrollToIndexUser by remember {
        derivedStateOf { scrollIndices["user"] }
    }
    
    LaunchedEffect(scrollToIndexUser) {
        scrollToIndexUser?.let { targetArtworkIndex ->
            // 计算目标作品所在的用户索引
            var accumulatedIndex = 0
            var targetUserIndex = 0
            
            for ((userIndex, user) in state.userResults.withIndex()) {
                val userArtworkCount = user.illusts.size
                if (targetArtworkIndex < accumulatedIndex + userArtworkCount) {
                    targetUserIndex = userIndex
                    break
                }
                accumulatedIndex += userArtworkCount
            }
            
            if (targetUserIndex >= 0 && targetUserIndex < state.userResults.size) {
                userListState.animateScrollToItem(targetUserIndex)
                scrollIndices.remove("user")
            }
        }
    }
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = remember(state.currentCategory) {
        {
            val currentCategory = state.currentCategory
            when (currentCategory) {
                SearchCategory.ILLUST -> {
                    val isAtTop = illustListState.firstVisibleItemIndex == 0 && 
                                  illustListState.firstVisibleItemScrollOffset == 0
                    if (isAtTop) {
                        onRefresh()
                    } else {
                        scope.launch {
                            illustListState.animateScrollToItem(0)
                        }
                    }
                }
                SearchCategory.NOVEL -> {
                    val isAtTop = novelListState.firstVisibleItemIndex == 0 && 
                                  novelListState.firstVisibleItemScrollOffset == 0
                    if (isAtTop) {
                        onRefresh()
                    } else {
                        scope.launch {
                            novelListState.animateScrollToItem(0)
                        }
                    }
                }
                SearchCategory.USER -> {
                    val isAtTop = userListState.firstVisibleItemIndex == 0 && 
                                  userListState.firstVisibleItemScrollOffset == 0
                    if (isAtTop) {
                        onRefresh()
                    } else {
                        scope.launch {
                            userListState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
    }
    
    // 同步 Pager 和 Category
    // 只在滑动结束后同步，避免在动画过程中触发
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val category = SearchCategory.entries[pagerState.currentPage]
            if (category != state.currentCategory) {
                onCategoryChange(category)
            }
        }
    }
    
    // 当category变化时，同步pager位置
    LaunchedEffect(state.currentCategory) {
        val targetPage = state.currentCategory.ordinal
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // 搜索输入栏
                SearchInputBar(
                    searchKeyword = state.searchKeyword,
                    onSearchKeywordChange = onSearchKeywordChange,
                    onSearch = onSearch,
                    autocompleteSuggestions = state.autocompleteSuggestions,
                    onAutocompleteSuggestionClick = onAutocompleteSuggestionClick,
                    onBack = onBack
                )
                
                // 分类 Tab 和筛选按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = state.currentCategory.ordinal,
                        modifier = Modifier.weight(1f),
                        edgePadding = 0.dp
                    ) {
                        SearchCategory.entries.forEach { category ->
                            Tab(
                                selected = state.currentCategory == category,
                                onClick = {
                                    if (state.currentCategory == category) {
                                        // 点击当前选中的tab：刷新或置顶
                                        scrollToTopOrRefresh()
                                    } else {
                                        // 切换到其他tab：只需更新category，LaunchedEffect会自动同步pager
                                        onCategoryChange(category)
                                    }
                                },
                                text = { Text(category.getDisplayName(), maxLines = 1) }
                            )
                        }
                    }
                    
                    // 筛选按钮（固定在右侧）
                    IconButton(onClick = onToggleFilterDrawer) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(Res.string.download_filter))
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // HorizontalPager for category content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (SearchCategory.entries[page]) {
                    SearchCategory.ILLUST -> {
                        IllustResultGrid(
                            artworks = state.illustResults,
                            isLoadingMore = state.isLoadingMore,
                            hasMore = state.hasMoreIllust,
                            onLoadMore = onLoadMore,
                            onArtworkClick = { artwork, index -> onArtworkClick(artwork, index, SearchCategory.ILLUST) },
                            listState = illustListState,
                            error = state.error,
                            onRetry = onRefresh
                        )
                    }
                    SearchCategory.NOVEL -> {
                        NovelResultList(
                            novels = state.novelResults,
                            isLoadingMore = state.isLoadingMore,
                            hasMore = state.hasMoreNovel,
                            onLoadMore = onLoadMore,
                            onNovelClick = onNovelClick,
                            onSeriesClick = onSeriesClick,
                            onTagClick = onTagClick,
                            listState = novelListState,
                            error = state.error,
                            onRetry = onRefresh
                        )
                    }
                    SearchCategory.USER -> {
                        UserResultList(
                            users = state.userResults,
                            isLoadingMore = state.isLoadingMore,
                            hasMore = state.hasMoreUser,
                            onLoadMore = onLoadMore,
                            onUserClick = onUserClick,
                            onArtworkClick = { artwork, index -> onArtworkClick(artwork, index, SearchCategory.USER) },
                            listState = userListState,
                            error = state.error,
                            onRetry = onRefresh
                        )
                    }
                }
            }
            
            // 筛选抽屉
            if (state.isFilterDrawerOpen) {
                FilterDrawer(
                    currentCategory = state.currentCategory,
                    illustParams = state.illustParams,
                    novelParams = state.novelParams,
                    userParams = state.userParams,
                    isPremiumUser = state.isPremiumUser,
                    onClose = onToggleFilterDrawer,
                    onUpdateIllustParams = onUpdateIllustParams,
                    onUpdateNovelParams = onUpdateNovelParams,
                    onUpdateUserParams = onUpdateUserParams
                )
            }
        }
    }
}

/**
 * 搜索输入栏（带自动补全）
 */
@Composable
fun SearchInputBar(
    searchKeyword: androidx.compose.ui.text.input.TextFieldValue,
    onSearchKeywordChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    autocompleteSuggestions: List<Tag>,
    onAutocompleteSuggestionClick: (Tag) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.nav_back))
            }
            
            // 搜索输入框（与搜索准备页面保持一致）
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                trailingIcon = {
                    Row {
                        if (searchKeyword.text.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchKeywordChange(
                                    androidx.compose.ui.text.input.TextFieldValue("")
                                )
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.search_clear))
                            }
                        }
                        IconButton(
                            onClick = onSearch,
                            enabled = searchKeyword.text.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(Res.string.search_button)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }
        
        // 浮动自动补全建议
        if (autocompleteSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 72.dp)
                    .heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn {
                    items(autocompleteSuggestions) { tag ->
                        ListItem(
                            headlineContent = {
                                Text(tag.translatedName ?: tag.name)
                            },
                            supportingContent = if (tag.translatedName != null) {
                                { Text(tag.name) }
                            } else null,
                            leadingContent = {
                                Icon(Icons.Default.Tag, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                onAutocompleteSuggestionClick(tag)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 插画搜索结果网格
 */
@Composable
fun IllustResultGrid(
    artworks: List<Artwork>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    listState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // 错误状态
            error != null && artworks.isEmpty() -> {
                ErrorDisplay(
                    message = error,
                    onRetry = onRetry ?: {},
                    modifier = Modifier.align(Alignment.Center),
                    isFullScreen = true
                )
            }
            // 初始加载中
            isLoadingMore && artworks.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 空状态
            artworks.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 有数据，显示列表
            else -> {
                // 监听滚动，触发加载更多
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10 && hasMore && !isLoadingMore) {
                                onLoadMore()
                            }
                        }
                }
                
                val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
                val columns by settingsCache.staggeredGridColumns.collectAsState()
                
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(artworks, key = { it.id }) { artwork ->
                        val index = artworks.indexOf(artwork)
                        ArtworkCard(
                            artwork = artwork,
                            onClick = { onArtworkClick(artwork, index) }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 小说搜索结果列表
 */
@Composable
fun NovelResultList(
    novels: List<Novel>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    listState: androidx.compose.foundation.lazy.LazyListState,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // 错误状态
            error != null && novels.isEmpty() -> {
                ErrorDisplay(
                    message = error,
                    onRetry = onRetry ?: {},
                    modifier = Modifier.align(Alignment.Center),
                    isFullScreen = true
                )
            }
            // 初始加载中
            isLoadingMore && novels.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 空状态
            novels.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 有数据，显示列表
            else -> {
                // 监听滚动，触发加载更多
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null && lastVisibleIndex >= novels.size - 3 && hasMore && !isLoadingMore) {
                                onLoadMore()
                            }
                        }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(novels, key = { it.id }) { novel ->
                        NovelCard(
                            novel = novel,
                            onClick = { onNovelClick(novel) },
                            onSeriesClick = onSeriesClick,
                            onTagClick = onTagClick
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 用户搜索结果列表
 */
@Composable
fun UserResultList(
    users: List<User>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            // 错误状态
            error != null && users.isEmpty() -> {
                ErrorDisplay(
                    message = error,
                    onRetry = onRetry ?: {},
                    modifier = Modifier.align(Alignment.Center),
                    isFullScreen = true
                )
            }
            // 初始加载中
            isLoadingMore && users.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 空状态
            users.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // 有数据，显示列表
            else -> {
                // 预计算每个用户的作品起始索引
                val userArtworkStartIndices = remember(users) {
                    var index = 0
                    users.map { user ->
                        val startIndex = index
                        index += user.illusts.size
                        startIndex
                    }
                }
                
                // 监听滚动，触发加载更多
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { lastVisibleIndex ->
                            if (lastVisibleIndex != null && lastVisibleIndex >= users.size - 3 && hasMore && !isLoadingMore) {
                                onLoadMore()
                            }
                        }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
                        val artworkStartIndex = userArtworkStartIndices.getOrElse(index) { 0 }
                        UserCard(
                            user = user,
                            onUserClick = { onUserClick(user) },
                            onArtworkClick = { artwork, localIndex ->
                                val globalIndex = artworkStartIndex + localIndex
                                onArtworkClick(artwork, globalIndex)
                            },
                            artworkStartIndex = 0  // 传入 0，因为我们在外部计算全局索引
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 筛选抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDrawer(
    currentCategory: SearchCategory,
    illustParams: IllustSearchParams,
    novelParams: NovelSearchParams,
    userParams: UserSearchParams,
    isPremiumUser: Boolean,
    onClose: () -> Unit,
    onUpdateIllustParams: (IllustSearchParams) -> Unit,
    onUpdateNovelParams: (NovelSearchParams) -> Unit,
    onUpdateUserParams: (UserSearchParams) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选条件",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.search_close))
                }
            }
            
            HorizontalDivider()
            
            // 根据分类显示不同的筛选器
            when (currentCategory) {
                SearchCategory.ILLUST -> {
                    IllustFilters(
                        params = illustParams,
                        onParamsChange = onUpdateIllustParams,
                        isPremiumUser = isPremiumUser
                    )
                }
                SearchCategory.NOVEL -> {
                    NovelFilters(
                        params = novelParams,
                        onParamsChange = onUpdateNovelParams,
                        isPremiumUser = isPremiumUser
                    )
                }
                SearchCategory.USER -> {
                    UserFilters(
                        params = userParams,
                        onParamsChange = onUpdateUserParams
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 插画筛选器
 */
@Composable
fun IllustFilters(
    params: IllustSearchParams,
    onParamsChange: (IllustSearchParams) -> Unit,
    isPremiumUser: Boolean = false
) {
    // 获取所有模式的本地化名称
    val modes = com.projectu.shared.data.remote.model.IllustSearchMode.entries
    val modeNames = modes.map { it.getDisplayName() }
    val currentModeName = params.searchMode.getDisplayName()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 搜索模式
        SearchModeDropdown(
            label = stringResource(Res.string.search_mode),
            selectedMode = currentModeName,
            modes = modeNames,
            onModeSelect = { displayName ->
                val index = modeNames.indexOf(displayName)
                if (index >= 0) {
                    onParamsChange(params.copy(searchMode = modes[index]))
                }
            }
        )
        
        // 排序方式 - 使用下拉列表
        SortOrderDropdown(
            selectedOrder = params.order,
            onOrderSelect = { onParamsChange(params.copy(order = it)) },
            isPremiumUser = isPremiumUser
        )
        
        // 收藏人数筛选 - 新增
        BookmarkCountDropdown(
            selectedCount = params.bookmarkCount,
            onCountSelect = { onParamsChange(params.copy(bookmarkCount = it)) }
        )
        
        // 内容分级 - 使用胶囊导航
        ContentModeChips(
            selectedMode = params.contentMode,
            onModeSelect = { onParamsChange(params.copy(contentMode = it)) }
        )
        
        // 日期范围筛选
        DateRangePicker(
            dateRange = params.dateRange,
            onDateRangeChange = { onParamsChange(params.copy(dateRange = it)) }
        )
        
        // AI 过滤 - 移到最下方
        AiFilterSwitch(
            checked = params.hideAi,
            onCheckedChange = { onParamsChange(params.copy(hideAi = it)) }
        )
    }
}

/**
 * 小说筛选器
 */
@Composable
fun NovelFilters(
    params: NovelSearchParams,
    onParamsChange: (NovelSearchParams) -> Unit,
    isPremiumUser: Boolean = false
) {
    // 获取所有模式的本地化名称
    val modes = com.projectu.shared.data.remote.model.NovelSearchMode.entries
    val modeNames = modes.map { it.getDisplayName() }
    val currentModeName = params.searchMode.getDisplayName()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 搜索模式
        SearchModeDropdown(
            label = stringResource(Res.string.search_mode),
            selectedMode = currentModeName,
            modes = modeNames,
            onModeSelect = { displayName ->
                val index = modeNames.indexOf(displayName)
                if (index >= 0) {
                    onParamsChange(params.copy(searchMode = modes[index]))
                }
            }
        )
        
        // 排序方式 - 使用下拉列表
        SortOrderDropdown(
            selectedOrder = params.order,
            onOrderSelect = { onParamsChange(params.copy(order = it)) },
            isPremiumUser = isPremiumUser
        )
        
        // 收藏人数筛选 - 新增
        BookmarkCountDropdown(
            selectedCount = params.bookmarkCount,
            onCountSelect = { onParamsChange(params.copy(bookmarkCount = it)) }
        )
        
        // 内容分级 - 使用胶囊导航
        ContentModeChips(
            selectedMode = params.contentMode,
            onModeSelect = { onParamsChange(params.copy(contentMode = it)) }
        )
        
        // 日期范围筛选
        DateRangePicker(
            dateRange = params.dateRange,
            onDateRangeChange = { onParamsChange(params.copy(dateRange = it)) }
        )
        
        // 作品语言筛选
        WorkLanguageDropdown(
            selectedLanguage = params.workLang,
            onLanguageSelect = { onParamsChange(params.copy(workLang = it)) }
        )
        
        // AI 过滤 - 移到最下方
        AiFilterSwitch(
            checked = params.hideAi,
            onCheckedChange = { onParamsChange(params.copy(hideAi = it)) }
        )
    }
}

/**
 * 用户筛选器
 */
@Composable
fun UserFilters(
    params: UserSearchParams,
    onParamsChange: (UserSearchParams) -> Unit
) {
    // 获取所有模式的本地化名称
    val modes = com.projectu.shared.data.remote.model.UserSearchMode.entries
    val modeNames = modes.map { it.getDisplayName() }
    val currentModeName = params.searchMode.getDisplayName()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 搜索模式
        SearchModeDropdown(
            label = stringResource(Res.string.search_mode),
            selectedMode = currentModeName,
            modes = modeNames,
            onModeSelect = { displayName ->
                val index = modeNames.indexOf(displayName)
                if (index >= 0) {
                    onParamsChange(params.copy(searchMode = modes[index]))
                }
            }
        )
        
        // 仅显示有作品的用户
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(Res.string.search_only_users_with_works))
                Text(
                    text = stringResource(Res.string.search_filter_no_works),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = params.onlyWithWork,
                onCheckedChange = { onParamsChange(params.copy(onlyWithWork = it)) }
            )
        }
    }
}

/**
 * 搜索模式下拉选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchModeDropdown(
    label: String,
    selectedMode: String,
    modes: List<String>,
    onModeSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedMode,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode) },
                    onClick = {
                        onModeSelect(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 排序方式下拉选择器（新版）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOrderDropdown(
    selectedOrder: SortOrder,
    onOrderSelect: (SortOrder) -> Unit,
    isPremiumUser: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 过滤可用的排序选项：
    // - 非会员：显示日期排序 + 伪热度排序
    // - 会员：显示日期排序 + 真实热度排序
    val availableOrders = SortOrder.entries.filter { order ->
        when {
            // 伪热度排序只给非会员
            order.isPreviewMode -> !isPremiumUser
            // 真实热度排序只给会员
            order.requiresPremium -> isPremiumUser
            // 日期排序所有人都可用
            else -> true
        }
    }
    val orderNames = availableOrders.map { it.getDisplayName() }
    val selectedOrderName = selectedOrder.getDisplayName()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOrderName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.search_sort_order)) },
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableOrders.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.getDisplayName()) },
                    onClick = {
                        onOrderSelect(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 收藏人数下拉选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkCountDropdown(
    selectedCount: BookmarkCount,
    onCountSelect: (BookmarkCount) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val counts = BookmarkCount.entries
    val countNames = counts.map { stringResource(getStringResourceByKey(it.displayNameKey)) }
    val selectedCountName = stringResource(getStringResourceByKey(selectedCount.displayNameKey))
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCountName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.search_bookmark_count)) },
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            counts.forEach { count ->
                DropdownMenuItem(
                    text = { Text(stringResource(getStringResourceByKey(count.displayNameKey))) },
                    onClick = {
                        onCountSelect(count)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 排序方式选择器（旧版，保留作为备用）
 */
@Composable
fun SortOrderSelector(
    selectedOrder: SortOrder,
    onOrderSelect: (SortOrder) -> Unit,
    isPremiumUser: Boolean = false
) {
    Column {
        Text(
            text = stringResource(Res.string.search_sort_order),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        SortOrder.entries.forEach { order ->
            // 根据用户会员状态决定是否显示高级会员专属排序选项
            val isEnabled = !order.requiresPremium || isPremiumUser
            val alpha = if (isEnabled) 1f else 0.38f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { onOrderSelect(order) }
                    .padding(vertical = 8.dp)
                    .alpha(alpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOrder == order,
                    onClick = null,
                    enabled = isEnabled
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(order.getDisplayName())
                    // 如果是高级会员专属选项且用户不是会员，显示提示
                    if (order.requiresPremium && !isPremiumUser) {
                        Text(
                            text = stringResource(Res.string.search_sort_premium_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 内容分级胶囊导航选择器（新版）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentModeChips(
    selectedMode: ContentMode,
    onModeSelect: (ContentMode) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.search_content_rating),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContentMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelect(mode) },
                    label = { 
                        Text(
                            text = mode.getDisplayName(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 内容分级选择器（旧版，保留作为备用）
 */
@Composable
fun ContentModeSelector(
    selectedMode: ContentMode,
    onModeSelect: (ContentMode) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.search_content_rating),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        ContentMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelect(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = null
                )
                Spacer(Modifier.width(8.dp))
                Text(mode.getDisplayName())
            }
        }
    }
}

/**
 * AI 过滤开关
 */
@Composable
fun AiFilterSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(stringResource(Res.string.search_hide_ai_works))
            Text(
                text = stringResource(Res.string.search_filter_ai_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 日期范围选择器（使用Material DatePicker）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    dateRange: DateRange?,
    onDateRangeChange: (DateRange?) -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = stringResource(Res.string.search_date_range),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 起始日期
            OutlinedButton(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.date_start),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = dateRange?.startDate ?: stringResource(Res.string.date_unlimited),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 结束日期
            OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.date_end),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = dateRange?.endDate ?: stringResource(Res.string.date_unlimited),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // 快速清除按钮
        if (dateRange != null && (dateRange.startDate != null || dateRange.endDate != null)) {
            TextButton(
                onClick = { onDateRangeChange(null) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.date_clear), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    
    // 起始日期选择器
    if (showStartDatePicker) {
        DatePickerModal(
            initialDate = dateRange?.startDate,
            onDateSelected = { selectedDate ->
                onDateRangeChange(
                    DateRange(
                        startDate = selectedDate,
                        endDate = dateRange?.endDate
                    )
                )
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        DatePickerModal(
            initialDate = dateRange?.endDate,
            onDateSelected = { selectedDate ->
                onDateRangeChange(
                    DateRange(
                        startDate = dateRange?.startDate,
                        endDate = selectedDate
                    )
                )
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * 日期选择弹窗（复用排行榜的日期转换逻辑）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: String?,
    onDateSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    // 将 yyyy-MM-dd 转换为 UTC 毫秒时间戳
    fun dateStringToMillis(dateString: String?): Long? {
        if (dateString == null) return null
        return try {
            val parts = dateString.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            // 计算自 1970-01-01 以来的天数
            var days = 0L
            for (y in 1970 until year) {
                days += if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 366 else 365
            }
            
            val isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
            val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            for (m in 1 until month) {
                days += daysInMonth[m - 1]
            }
            days += (day - 1)
            
            days * 24 * 60 * 60 * 1000L
        } catch (e: Exception) {
            null
        }
    }
    
    // 将 UTC 毫秒时间戳转换为 yyyy-MM-dd
    fun millisToDateString(millis: Long): String {
        val days = millis / (24 * 60 * 60 * 1000)
        var remainingDays = days
        var year = 1970
        
        while (true) {
            val daysInYear = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }
        
        val isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 1
        for (m in daysInMonth) {
            if (remainingDays < m) break
            remainingDays -= m
            month++
        }
        
        val day = remainingDays + 1
        return "%04d-%02d-%02d".format(year, month, day.toInt())
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateStringToMillis(initialDate)
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // "清除" 按钮
                TextButton(
                    onClick = {
                        onDateSelected(null)
                    }
                ) {
                    Text(stringResource(Res.string.date_clear))
                }
                // "确定" 按钮
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millisToDateString(millis))
                        }
                    }
                ) {
                    Text(stringResource(Res.string.common_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(Res.string.date_select_title),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
    }
}

/**
 * 日期范围选择器（旧版，已替换）
 */
@Composable
fun DateRangeSelector(
    dateRange: DateRange?,
    onDateRangeChange: (DateRange?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var startDateText by remember { mutableStateOf(dateRange?.startDate ?: "") }
    var endDateText by remember { mutableStateOf(dateRange?.endDate ?: "") }
    
    Column {
        Text(
            text = stringResource(Res.string.search_date_range),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        
        // 显示当前选择的日期范围
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (dateRange != null && (dateRange.startDate != null || dateRange.endDate != null)) {
                    val start = dateRange.startDate ?: stringResource(Res.string.date_unlimited)
                    val end = dateRange.endDate ?: stringResource(Res.string.date_unlimited)
                    "$start ~ $end"
                } else {
                    stringResource(Res.string.date_select)
                }
            )
        }
        
        // 快速清除按钮
        if (dateRange != null && (dateRange.startDate != null || dateRange.endDate != null)) {
            TextButton(
                onClick = { 
                    onDateRangeChange(null)
                    startDateText = ""
                    endDateText = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.date_clear), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    
    // 日期选择对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.date_select_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 起始日期
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { startDateText = it },
                        label = { Text(stringResource(Res.string.date_start)) },
                        placeholder = { Text(stringResource(Res.string.date_format_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 结束日期
                    OutlinedTextField(
                        value = endDateText,
                        onValueChange = { endDateText = it },
                        label = { Text(stringResource(Res.string.date_end)) },
                        placeholder = { Text(stringResource(Res.string.date_format_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "${stringResource(Res.string.date_format_hint)} (${stringResource(Res.string.date_format_example)})\n${stringResource(Res.string.date_empty_means_unlimited)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newRange = if (startDateText.isBlank() && endDateText.isBlank()) {
                            null
                        } else {
                            DateRange(
                                startDate = startDateText.takeIf { it.isNotBlank() },
                                endDate = endDateText.takeIf { it.isNotBlank() }
                            )
                        }
                        onDateRangeChange(newRange)
                        showDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(Res.string.date_cancel))
                }
            }
        )
    }
}

/**
 * 获取SearchCategory的本地化显示名称
 */
@Composable
fun SearchCategory.getDisplayName(): String {
    return stringResource(
        when (this) {
            SearchCategory.ILLUST -> Res.string.search_tab_illust
            SearchCategory.NOVEL -> Res.string.search_tab_novel
            SearchCategory.USER -> Res.string.search_tab_user
        }
    )
}

/**
 * 获取SortOrder的本地化显示名称
 */
@Composable
fun SortOrder.getDisplayName(): String {
    return stringResource(
        when (this) {
            SortOrder.DATE_DESC -> Res.string.search_sort_date_desc
            SortOrder.DATE_ASC -> Res.string.search_sort_date_asc
            SortOrder.POPULAR_PREVIEW -> Res.string.search_sort_popular_preview
            SortOrder.POPULAR_DESC -> Res.string.search_sort_popular_desc
            SortOrder.POPULAR_MALE_DESC -> Res.string.search_sort_popular_male_desc
            SortOrder.POPULAR_FEMALE_DESC -> Res.string.search_sort_popular_female_desc
        }
    )
}

/**
 * 获取ContentMode的本地化显示名称
 */
@Composable
fun ContentMode.getDisplayName(): String {
    return when (this) {
        ContentMode.R18 -> "R18"
        ContentMode.ALL -> stringResource(Res.string.search_rating_all)
        ContentMode.SAFE -> stringResource(Res.string.search_rating_safe)
    }
}

/**
 * 获取IllustSearchMode的本地化显示名称
 */
@Composable
fun com.projectu.shared.data.remote.model.IllustSearchMode.getDisplayName(): String {
    return stringResource(
        when (this) {
            com.projectu.shared.data.remote.model.IllustSearchMode.TAG -> Res.string.search_mode_tag_partial
            com.projectu.shared.data.remote.model.IllustSearchMode.TAG_FULL -> Res.string.search_mode_tag_full
            com.projectu.shared.data.remote.model.IllustSearchMode.TITLE_CAPTION -> Res.string.search_mode_title_caption
        }
    )
}

/**
 * 获取NovelSearchMode的本地化显示名称
 */
@Composable
fun com.projectu.shared.data.remote.model.NovelSearchMode.getDisplayName(): String {
    return stringResource(
        when (this) {
            com.projectu.shared.data.remote.model.NovelSearchMode.TAG_ONLY -> Res.string.search_mode_tag_partial
            com.projectu.shared.data.remote.model.NovelSearchMode.TAG_FULL -> Res.string.search_mode_tag_full
            com.projectu.shared.data.remote.model.NovelSearchMode.TEXT_CONTENT -> Res.string.search_mode_text
            com.projectu.shared.data.remote.model.NovelSearchMode.TAG_TITLE_CAPTION -> Res.string.search_mode_keyword
        }
    )
}

/**
 * 获取UserSearchMode的本地化显示名称
 */
@Composable
fun com.projectu.shared.data.remote.model.UserSearchMode.getDisplayName(): String {
    return stringResource(
        when (this) {
            com.projectu.shared.data.remote.model.UserSearchMode.PARTIAL -> Res.string.search_mode_user_partial
            com.projectu.shared.data.remote.model.UserSearchMode.EXACT -> Res.string.search_mode_user_exact
        }
    )
}

/**
 * 作品语言下拉选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkLanguageDropdown(
    selectedLanguage: WorkLanguage,
    onLanguageSelect: (WorkLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allLanguages = WorkLanguage.entries
    
    // 获取显示文本的辅助函数
    @Composable
    fun getDisplayText(language: WorkLanguage): String {
        return when (language) {
            WorkLanguage.ALL -> stringResource(Res.string.work_lang_all)
            WorkLanguage.OTHER -> stringResource(Res.string.work_lang_other)
            else -> language.displayName
        }
    }
    
    Column {
        Text(
            text = stringResource(Res.string.work_lang_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = getDisplayText(selectedLanguage),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                allLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(getDisplayText(language)) },
                        onClick = {
                            onLanguageSelect(language)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * 根据字符串键获取StringResource
 */
@Composable
fun getStringResourceByKey(key: String): org.jetbrains.compose.resources.StringResource {
    return when (key) {
        "bookmark_count_none" -> Res.string.bookmark_count_none
        "bookmark_count_500" -> Res.string.bookmark_count_500
        "bookmark_count_1000" -> Res.string.bookmark_count_1000
        "bookmark_count_2000" -> Res.string.bookmark_count_2000
        "bookmark_count_5000" -> Res.string.bookmark_count_5000
        "bookmark_count_7500" -> Res.string.bookmark_count_7500
        "bookmark_count_10000" -> Res.string.bookmark_count_10000
        "bookmark_count_20000" -> Res.string.bookmark_count_20000
        "bookmark_count_50000" -> Res.string.bookmark_count_50000
        "bookmark_count_100000" -> Res.string.bookmark_count_100000
        else -> Res.string.bookmark_count_none
    }
}
