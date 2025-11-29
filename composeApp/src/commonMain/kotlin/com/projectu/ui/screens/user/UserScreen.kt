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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.FollowIndicator
import com.projectu.ui.components.MangaSeriesCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.NovelSeriesCard
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
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
 * @param userId 用户ID
 */
class UserScreen(
    private val userId: Long
) : Screen {
    
    // 将 scrollIndices 提升到类级别，避免在导航时丢失
    private val scrollIndices = mutableStateMapOf<UserProfileTab, Int>()
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<UserViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 加载用户数据
        LaunchedEffect(userId) {
            viewModel.loadUser(userId)
        }
        
        // 当前Tab的作品ID列表（State类型，用于ArtworkDetailScreen）
        val currentTabArtworkIdsState: State<List<String>> = remember {
            derivedStateOf {
                val tabData = state.tabDataCache[state.currentTab]
                tabData?.artworks?.map { it.id } ?: emptyList()
            }
        }
        
        UserScreenContent(
            state = state,
            onTabChange = viewModel::switchTab,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onRetryTab = viewModel::loadTabData,
            scrollIndices = scrollIndices,
            onArtworkClick = { artwork, index ->
                // 跳转到作品详情页
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = currentTabArtworkIdsState,
                        initialIndex = index,
                        onLoadMore = { viewModel.loadMore() },
                        onReturnWithIndex = { returnIndex ->
                            // 记忆返回时的索引，用于列表定位
                            scrollIndices[state.currentTab] = returnIndex
                        }
                    )
                )
            },
            onNovelClick = { novel ->
                // TODO: 跳转到小说详情页
                println("点击小说: ${novel.title}")
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
    onUserClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 每个Tab的列表滚动状态
    val tabListStates = remember { mutableStateMapOf<UserProfileTab, ListScrollState>() }
    
    // Pager状态
    val pagerState = rememberPagerState(
        initialPage = state.availableTabs.indexOf(state.currentTab).coerceAtLeast(0),
        pageCount = { state.availableTabs.size }
    )
    
    // 同步Pager和Tab
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newTab = state.availableTabs.getOrNull(pagerState.currentPage)
            if (newTab != null && newTab != state.currentTab) {
                onTabChange(newTab)
            }
        }
    }
    
    // 当外部切换Tab时，同步到Pager
    LaunchedEffect(state.currentTab) {
        val targetPage = state.availableTabs.indexOf(state.currentTab)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
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
                                currentTabIndex = pagerState.currentPage,
                                onTabClick = handleTabClick
                            )
                            
                            // 内容区域 - HorizontalPager
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                key = { state.availableTabs[it].name }
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
 */
@Composable
fun UserProfileTabRow(
    tabs: List<UserProfileTab>,
    currentTabIndex: Int,
    onTabClick: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = currentTabIndex,
        modifier = Modifier.fillMaxWidth(),
        divider = {}
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == currentTabIndex,
                onClick = { onTabClick(index) },
                text = { Text(tab.displayName) }
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
    mangaSeries: List<MangaSeriesItem>,
    novelSeries: List<NovelSeriesItem>,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
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
                        // 瀑布流展示插画/漫画
                        ArtworkStaggeredGrid(
                            artworks = tabData.artworks,
                            tab = tab,
                            scrollIndices = scrollIndices,
                            tabListStates = tabListStates,
                            onArtworkClick = onArtworkClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading
                        )
                    }
                    UserProfileTab.NOVELS -> {
                        // 列表展示小说
                        NovelList(
                            novels = tabData.novels,
                            tab = tab,
                            tabListStates = tabListStates,
                            onNovelClick = onNovelClick,
                            onLoadMore = onLoadMore,
                            isLoading = tabData.isLoading
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
                            onClick = { /* TODO: 跳转到系列详情 */ }
                        )
                    }
                    UserProfileTab.BOOKMARKS -> {
                        // 收藏列表（暂未实现）
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("收藏功能开发中...")
                        }
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
    onLoadMore: () -> Unit,
    isLoading: Boolean
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
                showUserInfo = false  // 用户主页不显示作者信息（都是同一用户的作品）
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
    onLoadMore: () -> Unit,
    isLoading: Boolean
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
                onClick = { onNovelClick(novel) }
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
    series: List<MangaSeriesItem>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onClick: (MangaSeriesItem) -> Unit
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
    series: List<NovelSeriesItem>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onClick: (NovelSeriesItem) -> Unit
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
