package com.projectu.ui.screens.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.koin.getScreenModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.UserCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 发现页面统一内容区域
 * 参照排行榜设计，支持横向滑动切换内容类型
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoveryContent(
    onRegisterScrollToTopOrRefreshCallback: ((() -> Unit) -> Unit)? = null
) {
    val contentTypes = remember { DiscoveryContentType.getAll() }
    
    // 预先创建所有 ViewModel，避免切换时重新创建
    val usersViewModel: DiscoveryUsersViewModel = koinInject()
    val illustsViewModel: DiscoveryIllustsViewModel = koinInject()
    val novelsViewModel: DiscoveryNovelsViewModel = koinInject()
    
    // 收集所有状态
    val usersState by usersViewModel.state.collectAsState()
    val illustsState by illustsViewModel.state.collectAsState()
    val novelsState by novelsViewModel.state.collectAsState()
    
    // 为每个内容类型创建独立的列表状态缓存
    val listStates = remember {
        mutableStateMapOf<DiscoveryContentType, Any>()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 创建 Pager 状态，默认进入第二个页面（推荐插画·漫画）
    val pagerState = rememberPagerState(
        initialPage = 1, // 默认进入 ILLUSTS
        pageCount = { contentTypes.size }
    )
    
    val currentContentType = contentTypes[pagerState.currentPage]
    
    // 创建刷新回调映射
    val refreshCallbacks = remember {
        mapOf(
            DiscoveryContentType.USERS to usersViewModel::refresh,
            DiscoveryContentType.ILLUSTS to illustsViewModel::refresh,
            DiscoveryContentType.NOVELS to novelsViewModel::refresh
        )
    }
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = {
        val listState = listStates[currentContentType]
        val isAtTop = when (listState) {
            is LazyStaggeredGridState -> 
                listState.firstVisibleItemIndex == 0 && 
                listState.firstVisibleItemScrollOffset == 0
            is LazyListState -> 
                listState.firstVisibleItemIndex == 0 && 
                listState.firstVisibleItemScrollOffset == 0
            else -> true
        }
        
        if (isAtTop) {
            // 刷新当前页面
            refreshCallbacks[currentContentType]?.invoke()
        } else {
            coroutineScope.launch {
                when (listState) {
                    is LazyStaggeredGridState -> listState.animateScrollToItem(0)
                    is LazyListState -> listState.animateScrollToItem(0)
                }
            }
        }
    }
    
    // 注册回调
    LaunchedEffect(scrollToTopOrRefresh) {
        onRegisterScrollToTopOrRefreshCallback?.invoke(scrollToTopOrRefresh)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 内容类型选择器
        DiscoveryContentTypeSelector(
            contentTypes = contentTypes,
            currentTypeIndex = pagerState.currentPage,
            onContentTypeChange = { type ->
                val targetPage = contentTypes.indexOf(type)
                if (targetPage >= 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            },
            onRefreshOrScrollToTop = scrollToTopOrRefresh,
            modifier = Modifier.fillMaxWidth()
        )
        
        // HorizontalPager：支持左右滑动切换内容类型
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { contentTypes[it] }
        ) { page ->
            val contentType = contentTypes[page]
            
            when (contentType) {
                DiscoveryContentType.USERS -> {
                    val listState = rememberLazyListState()
                    remember(contentType) {
                        listStates.getOrPut(contentType) { listState }
                    }
                    
                    DiscoveryUsersPage(
                        state = usersState,
                        onLoadMore = usersViewModel::loadMore,
                        onRefresh = usersViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onUserClick = { user ->
                            // TODO: 跳转到用户详情页
                            println("点击用户: ${user.name}")
                        },
                        onArtworkClick = { artwork ->
                            // TODO: 跳转到作品详情页
                            println("点击作品: ${artwork.title}")
                        },
                        listState = listState as LazyListState
                    )
                }
                
                DiscoveryContentType.ILLUSTS -> {
                    val listState = rememberLazyStaggeredGridState()
                    remember(contentType) {
                        listStates.getOrPut(contentType) { listState }
                    }
                    
                    DiscoveryIllustsPage(
                        state = illustsState,
                        onModeChange = illustsViewModel::switchMode,
                        onLoadMore = illustsViewModel::loadMore,
                        onRefresh = illustsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onArtworkClick = { artwork ->
                            // TODO: 跳转到作品详情页
                            println("点击作品: ${artwork.title}")
                        },
                        listState = listState as LazyStaggeredGridState
                    )
                }
                
                DiscoveryContentType.NOVELS -> {
                    val listState = rememberLazyListState()
                    remember(contentType) {
                        listStates.getOrPut(contentType) { listState }
                    }
                    
                    DiscoveryNovelsPage(
                        state = novelsState,
                        onModeChange = novelsViewModel::switchMode,
                        onLoadMore = novelsViewModel::loadMore,
                        onRefresh = novelsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onNovelClick = { novel ->
                            // TODO: 跳转到小说详情页
                            println("点击小说: ${novel.title}")
                        },
                        listState = listState as LazyListState
                    )
                }
            }
        }
    }
}

/**
 * 内容类型选择器
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoveryContentTypeSelector(
    contentTypes: List<DiscoveryContentType>,
    currentTypeIndex: Int,
    onContentTypeChange: (DiscoveryContentType) -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // 当选中的类型变化时，自动滚动到可见位置
    LaunchedEffect(currentTypeIndex, contentTypes) {
        if (currentTypeIndex >= 0 && currentTypeIndex < contentTypes.size) {
            val chipWidth = 150
            val scrollPosition = (currentTypeIndex * chipWidth).coerceAtLeast(0)
            scrollState.animateScrollTo(scrollPosition)
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contentTypes.forEachIndexed { index, type ->
                FilterChip(
                    selected = index == currentTypeIndex,
                    onClick = {
                        if (index == currentTypeIndex) {
                            // 点击已选中的类型，触发刷新或滚动到顶部
                            onRefreshOrScrollToTop()
                        } else {
                            // 切换到新的类型
                            onContentTypeChange(type)
                        }
                    },
                    label = { Text(type.displayName) }
                )
            }
        }
    }
}

/**
 * 推荐用户页面内容
 */
@Composable
fun DiscoveryUsersPage(
    state: DiscoveryUsersState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    listState: LazyListState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.users.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null && state.users.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRefresh) {
                        Text("重试")
                    }
                }
            }
            state.users.isNotEmpty() -> {
                UserListLayout(
                    users = state.users,
                    onUserClick = onUserClick,
                    onArtworkClick = onArtworkClick,
                    onLoadMore = onLoadMore,
                    isLoadingMore = state.isLoadingMore,
                    listState = listState,
                    isRefreshing = state.isLoading && state.users.isNotEmpty(),
                    onRefresh = onRefresh
                )
            }
        }
    }
}

/**
 * 推荐插画·漫画页面内容
 */
@Composable
fun DiscoveryIllustsPage(
    state: DiscoveryIllustsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    listState: LazyStaggeredGridState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mode 选择器
        DiscoveryModeSelector(
            currentMode = state.currentMode,
            onModeChange = onModeChange,
            onRefreshOrScrollToTop = onRefreshOrScrollToTop,
            modifier = Modifier.fillMaxWidth()
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.artworks.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.artworks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
                state.artworks.isNotEmpty() -> {
                    ArtworkStaggeredGridLayout(
                        artworks = state.artworks,
                        onArtworkClick = onArtworkClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        listState = listState,
                        isRefreshing = state.isLoading && state.artworks.isNotEmpty(),
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * 推荐小说页面内容
 */
@Composable
fun DiscoveryNovelsPage(
    state: DiscoveryNovelsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    listState: LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mode 选择器
        DiscoveryModeSelector(
            currentMode = state.currentMode,
            onModeChange = onModeChange,
            onRefreshOrScrollToTop = onRefreshOrScrollToTop,
            modifier = Modifier.fillMaxWidth()
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.novels.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.novels.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
                state.novels.isNotEmpty() -> {
                    NovelListLayout(
                        novels = state.novels,
                        onNovelClick = onNovelClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        listState = listState,
                        isRefreshing = state.isLoading && state.novels.isNotEmpty(),
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * Discovery Mode 选择器（ALL/SAFE/R18）
 */
@Composable
fun DiscoveryModeSelector(
    currentMode: DiscoveryMode,
    onModeChange: (DiscoveryMode) -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember { DiscoveryMode.entries }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = {
                        if (currentMode == mode) {
                            // 点击已选中的 mode，触发刷新或滚动到顶部
                            onRefreshOrScrollToTop()
                        } else {
                            // 切换到新的 mode
                            onModeChange(mode)
                        }
                    },
                    label = {
                        Text(
                            text = when (mode) {
                                DiscoveryMode.ALL -> stringResource(Res.string.discovery_mode_all)
                                DiscoveryMode.SAFE -> stringResource(Res.string.discovery_mode_safe)
                                DiscoveryMode.R18 -> stringResource(Res.string.discovery_mode_r18)
                            }
                        )
                    }
                )
            }
        }
    }
}

/**
 * 用户列表布局（带下拉刷新）
 */
@Composable
fun UserListLayout(
    users: List<User>,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyListState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index?.let { it >= totalItems - 5 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    onUserClick = { onUserClick(user) },
                    onArtworkClick = onArtworkClick
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

/**
 * 作品瀑布流布局（带下拉刷新）
 */
@Composable
fun ArtworkStaggeredGridLayout(
    artworks: List<Artwork>,
    onArtworkClick: (Artwork) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyStaggeredGridState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10) {
                    onLoadMore()
                }
            }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            items(artworks, key = { it.id }) { artwork ->
                ArtworkCard(
                    artwork = artwork,
                    onClick = { onArtworkClick(artwork) }
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

/**
 * 小说列表布局（带下拉刷新）
 */
@Composable
fun NovelListLayout(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyListState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index?.let { it >= totalItems - 5 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(novels, key = { it.id }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) }
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
