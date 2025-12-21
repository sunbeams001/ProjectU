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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import cafe.adriel.voyager.core.screen.Screen
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
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.util.rememberTagClickHandler
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

/**
 * 搜索结果页面
 */
data class SearchResultScreen(val initialKeyword: String) : Screen {
    
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
                        state.userResults.flatMap { user -> user.illusts?.map { it.id } ?: emptyList() },
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
                val userArtworkCount = user.illusts?.size ?: 0
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
    LaunchedEffect(pagerState.currentPage) {
        val category = SearchCategory.entries[pagerState.currentPage]
        if (category != state.currentCategory) {
            onCategoryChange(category)
        }
    }
    
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
                    ScrollableTabRow(
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
                                        // 切换到其他tab
                                        scope.launch {
                                            pagerState.animateScrollToPage(category.ordinal)
                                        }
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
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.nav_back))
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
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(3),
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
                            LaunchedEffect(Unit) {
                                if (hasMore) {
                                    onLoadMore()
                                }
                            }
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
                            onTagClick = onTagClick
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            LaunchedEffect(Unit) {
                                if (hasMore) {
                                    onLoadMore()
                                }
                            }
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
                        index += (user.illusts?.size ?: 0)
                        startIndex
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
                            LaunchedEffect(Unit) {
                                if (hasMore) {
                                    onLoadMore()
                                }
                            }
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
                        onParamsChange = onUpdateIllustParams
                    )
                }
                SearchCategory.NOVEL -> {
                    NovelFilters(
                        params = novelParams,
                        onParamsChange = onUpdateNovelParams
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
    onParamsChange: (IllustSearchParams) -> Unit
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
        
        // 排序方式
        SortOrderSelector(
            selectedOrder = params.order,
            onOrderSelect = { onParamsChange(params.copy(order = it)) }
        )
        
        // 内容分级
        ContentModeSelector(
            selectedMode = params.contentMode,
            onModeSelect = { onParamsChange(params.copy(contentMode = it)) }
        )
        
        // AI 过滤
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
    onParamsChange: (NovelSearchParams) -> Unit
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
        
        // 排序方式
        SortOrderSelector(
            selectedOrder = params.order,
            onOrderSelect = { onParamsChange(params.copy(order = it)) }
        )
        
        // 内容分级
        ContentModeSelector(
            selectedMode = params.contentMode,
            onModeSelect = { onParamsChange(params.copy(contentMode = it)) }
        )
        
        // AI 过滤
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
                .menuAnchor()
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
 * 排序方式选择器
 */
@Composable
fun SortOrderSelector(
    selectedOrder: SortOrder,
    onOrderSelect: (SortOrder) -> Unit
) {
    Column {
        Text(
            text = stringResource(Res.string.search_sort_order),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        SortOrder.entries.forEach { order ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOrderSelect(order) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedOrder == order,
                    onClick = null
                )
                Spacer(Modifier.width(8.dp))
                Text(order.getDisplayName())
            }
        }
    }
}

/**
 * 内容分级选择器
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
