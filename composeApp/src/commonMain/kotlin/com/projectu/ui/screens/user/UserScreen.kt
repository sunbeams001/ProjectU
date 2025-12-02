package com.projectu.ui.screens.user

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.FollowIndicator
import com.projectu.ui.components.MangaSeriesCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.NovelSeriesCard
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import cafe.adriel.voyager.koin.koinScreenModel

/**
 * 列表滚动状态的封装，用于统一管理不同类型列表的滚动
 */
sealed class ListScrollState {
    data class StaggeredGrid(val state: LazyStaggeredGridState) : ListScrollState()
    data class LazyList(val state: LazyListState) : ListScrollState()
    
    val isAtTop: Boolean
        get() = when (this) {
            is StaggeredGrid -> state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            is LazyList -> state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
        }
    
    suspend fun animateScrollToTop() {
        when (this) {
            is StaggeredGrid -> state.animateScrollToItem(0)
            is LazyList -> state.animateScrollToItem(0)
        }
    }
}

/**
 * 用户页面
 * 
 * 注意：所有类级别的属性必须是可序列化的，以支持 Activity 状态恢复
 * scrollIndices 通过 NavigationContextManager 管理，确保跨导航保持滚动位置
 * 
 * @param userId 用户ID
 */
data class UserScreen(
    private val userId: Long
) : Screen {
    
    // 每个用户页面需要独立的 key，确保 ScreenModel 不会被错误复用
    override val key: ScreenKey = "UserScreen_$userId"
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<UserViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 使用 NavigationContextManager 管理滚动位置，确保从详情页返回时恢复
        val scrollIndices = NavigationContextManager.getOrCreateUserScrollIndices(userId)
        
        // 加载用户数据
        LaunchedEffect(userId) {
            viewModel.loadUser(userId)
        }
        
        UserScreenContent(
            state = state,
            onTabChange = viewModel::switchTab,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onRetryTab = viewModel::loadTabData,
            scrollIndices = scrollIndices,
            onArtworkClick = { artwork, index ->
                // 获取当前 Tab 的作品列表
                val currentArtworkIds = state.tabDataCache[state.currentTab]?.artworks?.map { it.id } ?: emptyList()
                val currentTab = state.currentTab
                
                // 创建绑定到当前 Tab 的列表源
                val listSource = viewModel.createArtworkListSource(currentTab)
                
                // 创建导航上下文
                val contextKey = NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { returnIndex ->
                        scrollIndices[currentTab] = returnIndex
                    }
                )
                
                // 跳转到作品详情页
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = currentArtworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onNovelClick = { novel ->
                // 跳转到小说详情页
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onNovelSeriesClick = { seriesId ->
                // 跳转到小说系列详情页
                navigator.push(NovelSeriesScreen(seriesId))
            },
            onUserClick = { clickedUserId ->
                // 跳转到用户页面
                if (clickedUserId.toLongOrNull() != userId) {
                    navigator.push(UserScreen(clickedUserId.toLong()))
                }
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserScreenContent(
    state: UserScreenState,
    onTabChange: (UserProfileTab) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetryTab: (UserProfileTab) -> Unit,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelSeriesClick: (Long) -> Unit,
    onUserClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 每个Tab的列表滚动状态
    val tabListStates = remember { mutableStateMapOf<UserProfileTab, ListScrollState>() }
    
    // Pager状态 - 使用 availableTabs 的 key 来确保在 tabs 变化时重建 pager
    val pagerState = key(state.availableTabs) {
        rememberPagerState(
            initialPage = state.availableTabs.indexOf(state.currentTab).coerceAtLeast(0),
            pageCount = { state.availableTabs.size }
        )
    }
    
    // 安全的当前页索引，确保不超出范围
    val safeCurrentPage = pagerState.currentPage.coerceIn(0, (state.availableTabs.size - 1).coerceAtLeast(0))
    
    // 同步Pager和Tab
    LaunchedEffect(safeCurrentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newTab = state.availableTabs.getOrNull(safeCurrentPage)
            if (newTab != null && newTab != state.currentTab) {
                onTabChange(newTab)
            }
        }
    }
    
    // 当外部切换Tab时，同步到Pager
    LaunchedEffect(state.currentTab, state.availableTabs) {
        val targetPage = state.availableTabs.indexOf(state.currentTab)
        if (targetPage >= 0 && targetPage != pagerState.currentPage && state.availableTabs.isNotEmpty()) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    // Tab点击处理：如果点击已选中的Tab，滚动到顶部或刷新
    val handleTabClick: (Int) -> Unit = { index ->
        val clickedTab = state.availableTabs.getOrNull(index)
        if (index == pagerState.currentPage && clickedTab != null) {
            // 点击了当前Tab，检查是否在顶部
            val scrollState = tabListStates[clickedTab]
            if (scrollState?.isAtTop == true) {
                // 在顶部，刷新
                onRefresh()
            } else {
                // 不在顶部，滚动到顶部
                coroutineScope.launch {
                    scrollState?.animateScrollToTop()
                }
            }
        } else {
            // 点击了其他Tab，切换
            coroutineScope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 关注状态指示器
                    if (state.userProfile.userId.isNotEmpty()) {
                        FollowIndicator(
                            user = state.userProfile.toUser(),
                            size = 28.dp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoadingProfile && state.userProfile.userId.isEmpty() -> {
                    // 初次加载
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.profileError != null && state.userProfile.userId.isEmpty() -> {
                    // 错误状态
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.profileError,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 用户信息区域
                        UserProfileHeader(
                            profile = state.userProfile,
                            onUserClick = onUserClick
                        )
                        
                        // Tab导航栏
                        if (state.availableTabs.isNotEmpty()) {
                            UserProfileTabRow(
                                tabs = state.availableTabs,
                                currentTabIndex = safeCurrentPage,
                                onTabClick = handleTabClick
                            )
                            
                            // 内容区域 - HorizontalPager
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                key = { state.availableTabs.getOrNull(it)?.name ?: it }
                            ) { page ->
                                val tab = state.availableTabs[page]
                                val tabData = state.tabDataCache[tab] ?: TabData()
                                
                                UserTabContent(
                                    tab = tab,
                                    tabData = tabData,
                                    mangaSeries = state.mangaSeries,
                                    novelSeries = state.novelSeries,
                                    scrollIndices = scrollIndices,
                                    tabListStates = tabListStates,
                                    onArtworkClick = onArtworkClick,
                                    onNovelClick = onNovelClick,
                                    onNovelSeriesClick = onNovelSeriesClick,
                                    onUserClick = { userId -> onUserClick(userId.toString()) },
                                    onLoadMore = onLoadMore,
                                    onRetry = { onRetryTab(tab) }
                                )
                            }
                        } else {
                            // 没有作品
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "该用户暂无作品",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 用户信息头部
 */
@Composable
fun UserProfileHeader(
    profile: UserProfile,
    onUserClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 头像
        AsyncImage(
            model = profile.imageBig.ifEmpty { profile.image },
            contentDescription = profile.name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable { onUserClick(profile.userId) },
            contentScale = ContentScale.Crop
        )
        
        // 用户名和会员标识
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (profile.premium) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Premium",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFFD700)
                )
            }
        }
        
        // 关注数
        Text(
            text = "关注 ${profile.following}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 简介
        profile.comment?.takeIf { it.isNotBlank() }?.let { comment ->
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    
    HorizontalDivider()
}

/**
 * 将 UserProfile 转换为 User（用于 FollowIndicator）
 */
fun UserProfile.toUser(): User = User(
    id = userId,
    name = name,
    profileImageUrl = image,
    profileImageUrlBig = imageBig,
    comment = comment,
    followStatus = if (isFollowed) FollowStatus.PUBLIC else FollowStatus.NOT_FOLLOWING,
    isPremium = premium,
    backgroundUrl = backgroundUrl,
    followingCount = following
)

/**
 * Tab导航栏
 * 使用 SecondaryScrollableTabRow 支持滑动
 * 当Tab数量较少时，通过计算edgePadding实现居中效果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileTabRow(
    tabs: List<UserProfileTab>,
    currentTabIndex: Int,
    onTabClick: (Int) -> Unit
) {
    // 防止空列表或索引越界
    if (tabs.isEmpty()) return
    
    // 安全的索引，确保在有效范围内
    val safeTabIndex = currentTabIndex.coerceIn(0, tabs.size - 1)
    
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val tabWidths = remember(tabs) { mutableStateMapOf<Int, Int>() }
    
    // 计算所有Tab的总宽度
    val totalTabsWidthPx = remember(tabWidths.size, tabs.size) {
        if (tabWidths.size == tabs.size && tabs.isNotEmpty()) {
            tabWidths.values.sum()
        } else {
            0
        }
    }
    
    // 计算居中所需的边距
    val edgePadding = remember(containerWidthPx, totalTabsWidthPx) {
        if (totalTabsWidthPx > 0 && containerWidthPx > totalTabsWidthPx) {
            with(density) { ((containerWidthPx - totalTabsWidthPx) / 2).toDp() }
        } else {
            // 使用默认的 52.dp，这是 Material 3 ScrollableTabRow 的默认值
            52.dp
        }
    }
    
    SecondaryScrollableTabRow(
        selectedTabIndex = safeTabIndex,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                containerWidthPx = size.width
            },
        edgePadding = edgePadding,
        divider = {}
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == safeTabIndex,
                onClick = { onTabClick(index) },
                text = { Text(tab.displayName) },
                modifier = Modifier.onSizeChanged { size ->
                    tabWidths[index] = size.width
                }
            )
        }
    }
}

/**
 * Tab内容区域
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserTabContent(
    tab: UserProfileTab,
    tabData: TabData,
    mangaSeries: List<MangaSeries>,
    novelSeries: List<NovelSeries>,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelSeriesClick: (Long) -> Unit,
    onUserClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            tabData.isLoading && tabData.artworks.isEmpty() && tabData.novels.isEmpty() -> {
                CircularProgressIndicator()
            }
            tabData.error != null && tabData.artworks.isEmpty() && tabData.novels.isEmpty() -> {
                ErrorDisplay(
                    message = tabData.error,
                    onRetry = onRetry
                )
            }
            else -> {
                when (tab) {
                    UserProfileTab.ILLUSTS, UserProfileTab.MANGA -> {
                        // 瀑布流展示插画/漫画（用户自己的作品，不显示作者信息）
                        ArtworkStaggeredGrid(
                            artworks = tabData.artworks,
                            tab = tab,
                            scrollIndices = scrollIndices,
                            tabListStates = tabListStates,
                            onArtworkClick = onArtworkClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading,
                            showUserInfo = false
                        )
                    }
                    UserProfileTab.NOVELS -> {
                        // 列表展示小说（用户自己的作品，不显示作者信息）
                        NovelList(
                            novels = tabData.novels,
                            tab = tab,
                            tabListStates = tabListStates,
                            onNovelClick = onNovelClick,
                            onSeriesClick = onNovelSeriesClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading,
                            showUserInfo = false
                        )
                    }
                    UserProfileTab.MANGA_SERIES -> {
                        // 漫画系列列表
                        MangaSeriesList(
                            series = mangaSeries,
                            tab = tab,
                            tabListStates = tabListStates,
                            onClick = { /* TODO: 跳转到系列详情 */ }
                        )
                    }
                    UserProfileTab.NOVEL_SERIES -> {
                        // 小说系列列表
                        NovelSeriesList(
                            series = novelSeries,
                            tab = tab,
                            tabListStates = tabListStates,
                            onClick = { item -> 
                                item.id.toLongOrNull()?.let { id -> onNovelSeriesClick(id) }
                            }
                        )
                    }
                    UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                    UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                        // 收藏的插画·漫画（瀑布流展示，显示作者信息）
                        ArtworkStaggeredGrid(
                            artworks = tabData.artworks,
                            tab = tab,
                            scrollIndices = scrollIndices,
                            tabListStates = tabListStates,
                            onArtworkClick = onArtworkClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading,
                            showUserInfo = true
                        )
                    }
                    UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                    UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                        // 收藏的小说（列表展示，显示作者信息）
                        NovelList(
                            novels = tabData.novels,
                            tab = tab,
                            tabListStates = tabListStates,
                            onNovelClick = onNovelClick,
                            onSeriesClick = onNovelSeriesClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading,
                            showUserInfo = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * 插画/漫画瀑布流
 */
@Composable
fun ArtworkStaggeredGrid(
    artworks: List<Artwork>,
    tab: UserProfileTab,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    showUserInfo: Boolean = false
) {
    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()
    
    // 注册滚动状态
    LaunchedEffect(gridState) {
        tabListStates[tab] = ListScrollState.StaggeredGrid(gridState)
    }
    
    // 每次重组时检查是否需要滚动
    val pendingScrollIndex = scrollIndices[tab]
    
    // 使用 LaunchedEffect 并以 pendingScrollIndex 作为 key
    // 当 scrollIndices 更新后，下次重组时会触发
    LaunchedEffect(pendingScrollIndex, artworks.size) {
        if (pendingScrollIndex != null && pendingScrollIndex >= 0 && artworks.isNotEmpty()) {
            // 稍微延迟以确保列表已渲染
            kotlinx.coroutines.delay(150)
            
            val targetIndex = pendingScrollIndex.coerceAtMost(artworks.size - 1)
            gridState.animateScrollToItem(targetIndex)
            
            // 清除标记，避免重复滚动
            scrollIndices.remove(tab)
        }
    }
    
    // 监听滚动，触发加载更多
    LaunchedEffect(gridState, artworks.size, isLoading) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 6 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoading && artworks.isNotEmpty()) {
                onLoadMore()
            }
        }
    }
    
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(artworks, key = { it.id }) { artwork ->
            val index = artworks.indexOf(artwork)
            ArtworkCard(
                artwork = artwork,
                onClick = { onArtworkClick(artwork, index) },
                onUserClick = onUserClick,
                showUserInfo = showUserInfo
            )
        }
        
        // 加载更多指示器
        if (isLoading) {
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

/**
 * 小说列表
 */
@Composable
fun NovelList(
    novels: List<Novel>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onUserClick: ((Long) -> Unit)? = null,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    showUserInfo: Boolean = true
) {
    val listState = rememberLazyListState()
    
    // 注册滚动状态
    LaunchedEffect(listState) {
        tabListStates[tab] = ListScrollState.LazyList(listState)
    }
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState, novels.size, isLoading) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoading && novels.isNotEmpty()) {
                onLoadMore()
            }
        }
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(novels, key = { it.id }) { novel ->
            NovelCard(
                novel = novel,
                onClick = { onNovelClick(novel) },
                onSeriesClick = onSeriesClick,
                onUserClick = onUserClick,
                showUserInfo = showUserInfo
            )
        }
        
        // 加载更多指示器
        if (isLoading) {
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

/**
 * 漫画系列列表
 */
@Composable
fun MangaSeriesList(
    series: List<MangaSeries>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onClick: (MangaSeries) -> Unit
) {
    val listState = rememberLazyListState()
    
    // 注册滚动状态
    LaunchedEffect(listState) {
        tabListStates[tab] = ListScrollState.LazyList(listState)
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(series, key = { it.id }) { item ->
            MangaSeriesCard(
                series = item,
                onClick = { onClick(item) }
            )
        }
    }
}

/**
 * 小说系列列表
 */
@Composable
fun NovelSeriesList(
    series: List<NovelSeries>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onClick: (NovelSeries) -> Unit
) {
    val listState = rememberLazyListState()
    
    // 注册滚动状态
    LaunchedEffect(listState) {
        tabListStates[tab] = ListScrollState.LazyList(listState)
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(series, key = { it.id }) { item ->
            NovelSeriesCard(
                series = item,
                onClick = { onClick(item) }
            )
        }
    }
}
