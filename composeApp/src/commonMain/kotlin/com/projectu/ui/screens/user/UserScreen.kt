package com.projectu.ui.screens.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.TagFilterDialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import com.projectu.ui.components.FollowIndicator
import com.projectu.ui.components.HtmlText
import com.projectu.ui.components.MangaSeriesCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.NovelSeriesCard
import com.projectu.ui.components.UserCard
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.blocklist.BlockListScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.userrelations.UserRelationsScreen
import com.projectu.ui.util.AppLogger
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import cafe.adriel.voyager.koin.koinScreenModel
import org.koin.compose.koinInject

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
    private val userId: String
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
        
        // 创建Tag点击处理器
        val scope = rememberCoroutineScope()
        val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
        val tagClickHandler = remember(navigator) {
            TagClickHandler(navigator, searchHistoryStore, scope)
        }
        
        // 获取BlockRuleCache以检查用户是否被屏蔽
        val blockRuleCache: com.projectu.shared.domain.cache.BlockRuleCache = koinInject()
        val enabledRules by blockRuleCache.enabledRules.collectAsState()
        val isAuthorBlocked by remember(enabledRules, userId) {
            derivedStateOf {
                enabledRules.any { rule ->
                    rule.type == com.projectu.shared.domain.model.BlockRuleType.AUTHOR_ID &&
                    rule.value == userId &&
                    rule.enabled
                }
            }
        }
        
        // 获取BlockRuleRepository以添加/删除屏蔽规则
        val blockRuleRepository: com.projectu.shared.domain.repository.BlockRuleRepository = koinInject()
        
        UserScreenContent(
            state = state,
            viewModel = viewModel,
            onTabChange = viewModel::switchTab,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onRetryTab = viewModel::loadTabData,
            scrollIndices = scrollIndices,
            onArtworkClick = { artwork, index ->
                // 获取当前 Tab 的作品列表
                val currentTab = state.currentTab
                
                // 为推荐用户Tab使用特殊的列表源和导航处理
                if (currentTab == UserProfileTab.RECOMMEND_USERS) {
                    val currentArtworkIds = state.tabDataCache[currentTab]?.users?.flatMap { user ->
                        user.illusts.map { it.id }
                    } ?: emptyList()
                    
                    // 创建推荐用户的列表源
                    val listSource = viewModel.createRecommendUsersArtworkListSource()
                    
                    // 创建导航上下文
                    val contextKey = NavigationContextManager.createContext(
                        listSource = listSource,
                        onReturnWithIndex = { returnIndex ->
                            viewModel.setRecommendUsersScrollIndex(returnIndex)
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
                } else {
                    // 其他Tab的作品点击处理
                    val currentArtworkIds = state.tabDataCache[currentTab]?.artworks?.map { it.id } ?: emptyList()
                    
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
                }
            },
            onNovelClick = { novel ->
                // 跳转到小说详情页
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onNovelSeriesClick = { seriesId ->
                // 跳转到小说系列详情页
                navigator.push(NovelSeriesScreen(seriesId))
            },
            onMangaSeriesClick = { seriesId ->
                // 跳转到漫画系列详情页
                navigator.push(MangaSeriesScreen(seriesId))
            },
            onUserClick = { clickedUserId ->
                // 跳转到用户页面
                if (clickedUserId != userId) {
                    navigator.push(UserScreen(clickedUserId))
                }
            },
            onFollowingClick = { clickedUserId, userName ->
                // 跳转到用户关系页面
                navigator.push(UserRelationsScreen(clickedUserId, userName))
            },
            onBackClick = { navigator.pop() },
            onToggleTagFilter = viewModel::toggleTagFilter,
            onSelectTag = viewModel::selectTag,
            onTagClick = { tag -> tagClickHandler.handleTagClick(tag) },
            onBlockAuthor = {
                // 跳转到屏蔽列表页面并预填充作者ID
                navigator.push(BlockListScreen(prefilledAuthorId = userId))
            },
            onUnblockAuthor = {
                // 查找并删除屏蔽规则
                scope.launch {
                    val authorRule = enabledRules.find { rule ->
                        rule.type == com.projectu.shared.domain.model.BlockRuleType.AUTHOR_ID &&
                        rule.value == userId
                    }
                    authorRule?.let { rule ->
                        blockRuleRepository.deleteRule(rule.id)
                    }
                }
            },
            isAuthorBlocked = isAuthorBlocked
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserScreenContent(
    state: UserScreenState,
    viewModel: UserViewModel,
    onTabChange: (UserProfileTab) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetryTab: (UserProfileTab) -> Unit,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelSeriesClick: (String) -> Unit,
    onMangaSeriesClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onFollowingClick: ((String, String) -> Unit)? = null,
    onBackClick: () -> Unit,
    // Tag筛选相关回调
    onToggleTagFilter: (UserProfileTab) -> Unit = {},
    onSelectTag: (UserProfileTab, String?) -> Unit = { _, _ -> },
    // 自定义显示选项
    showBackButton: Boolean = true,
    showFollowIndicator: Boolean = true,
    topBarActions: (@Composable RowScope.() -> Unit)? = null,
    // 是否是独立页面（false 表示嵌入到其他页面如"我的"页面，不显示状态栏 padding）
    isStandalone: Boolean = true,
    // 用于外部控制置顶/刷新
    onRegisterScrollToTopOrRefreshCallback: ((callback: () -> Unit) -> Unit)? = null,
    // Tag点击处理
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    // 屏蔽作者相关
    onBlockAuthor: (() -> Unit)? = null,
    onUnblockAuthor: (() -> Unit)? = null,
    isAuthorBlocked: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 每个Tab的列表滚动状态
    val tabListStates = remember { mutableStateMapOf<UserProfileTab, ListScrollState>() }
    
    // 每个收藏Tab的Tag筛选行滚动状态
    val tagFilterScrollStates = remember { mutableStateMapOf<UserProfileTab, ScrollState>() }
    
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
    
    // 注册外部置顶/刷新回调
    LaunchedEffect(Unit) {
        onRegisterScrollToTopOrRefreshCallback?.invoke {
            val currentTab = state.availableTabs.getOrNull(pagerState.currentPage)
            if (currentTab != null) {
                val scrollState = tabListStates[currentTab]
                if (scrollState?.isAtTop == true) {
                    onRefresh()
                } else {
                    coroutineScope.launch {
                        scrollState?.animateScrollToTop()
                    }
                }
            }
        }
    }
    
    // 内部内容 Composable
    @Composable
    fun UserScreenInnerContent(modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxSize()) {
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
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 用户信息区域
                        UserProfileHeader(
                            profile = state.userProfile,
                            onUserClick = onUserClick,
                            onFollowingClick = onFollowingClick,
                            applyStatusBarPadding = isStandalone
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
                                    userDetailInfo = state.userDetailInfo,
                                    mangaSeries = state.mangaSeries,
                                    novelSeries = state.novelSeries,
                                    scrollIndices = scrollIndices,
                                    recommendUsersScrollIndex = viewModel.getRecommendUsersScrollIndex(),
                                    tabListStates = tabListStates,
                                    tagFilterScrollStates = tagFilterScrollStates,
                                    onArtworkClick = onArtworkClick,
                                    onNovelClick = onNovelClick,
                                    onNovelSeriesClick = onNovelSeriesClick,
                                    onMangaSeriesClick = onMangaSeriesClick,
                                    onUserClick = { userId -> onUserClick(userId.toString()) },
                                    onLoadMore = onLoadMore,
                                    onRefresh = onRefresh,
                                    onRetry = { onRetryTab(tab) },
                                    onToggleTagFilter = { onToggleTagFilter(tab) },
                                    onSelectTag = { selectedTag -> onSelectTag(tab, selectedTag) },
                                    onTagClick = onTagClick,
                                    onBlockAuthor = onBlockAuthor,
                                    onUnblockAuthor = onUnblockAuthor,
                                    isAuthorBlocked = isAuthorBlocked
                                )
                            }
                        } else {
                            // 没有作品
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.user_no_works),
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
    
    // 主要布局
    Box(modifier = Modifier.fillMaxSize()) {
        UserScreenInnerContent()
        
        // 只有在独立页面时才显示悬浮的返回和关注按钮
        if (isStandalone) {
            // 悬浮返回按钮（左上角）
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 4.dp, top = 4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 3.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            
            // 悬浮关注状态指示器（右上角）
            if (showFollowIndicator && state.userProfile.userId.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 自定义操作按钮
                    topBarActions?.invoke(this)
                    
                    // 关注状态
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 3.dp
                    ) {
                        FollowIndicator(
                            user = state.userProfile.toUser(),
                            size = 28.dp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        } else {
            // 嵌入模式时，只显示悬浮的自定义操作按钮（不显示返回和关注按钮）
            if (topBarActions != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 用半透明圆形背景包裹每个按钮
                    topBarActions.invoke(this)
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
    onUserClick: (String) -> Unit,
    onFollowingClick: ((String, String) -> Unit)? = null,
    applyStatusBarPadding: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
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
        
        // 关注数 - 可点击
        Text(
            text = stringResource(Res.string.user_following_count, profile.following),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(
                enabled = onFollowingClick != null
            ) {
                onFollowingClick?.invoke(profile.userId, profile.name)
            }
        )
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
                text = { Text(stringResource(tab.displayNameRes)) },
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
    userDetailInfo: UserDetailInfo?,
    mangaSeries: List<MangaSeries>,
    novelSeries: List<NovelSeries>,
    scrollIndices: MutableMap<UserProfileTab, Int>,
    recommendUsersScrollIndex: Int = 0,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    tagFilterScrollStates: MutableMap<UserProfileTab, ScrollState> = mutableMapOf(),
    onArtworkClick: (Artwork, Int) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelSeriesClick: (String) -> Unit,
    onMangaSeriesClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onToggleTagFilter: () -> Unit = {},
    onSelectTag: (String?) -> Unit = {},
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onBlockAuthor: (() -> Unit)? = null,
    onUnblockAuthor: (() -> Unit)? = null,
    isAuthorBlocked: Boolean = false
) {
    // 获取或创建当前Tab的Tag筛选行滚动状态
    val tagScrollState = tagFilterScrollStates.getOrPut(tab) { ScrollState(0) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (tab) {
            UserProfileTab.USER_INFO -> {
                // 用户详情信息
                if (userDetailInfo != null) {
                    UserInfoContent(
                        userDetailInfo = userDetailInfo,
                        tab = tab,
                        tabListStates = tabListStates,
                        onBlockAuthor = onBlockAuthor,
                        onUnblockAuthor = onUnblockAuthor,
                        isAuthorBlocked = isAuthorBlocked
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            else -> {
                when {
                    tabData.isLoading && tabData.artworks.isEmpty() && tabData.novels.isEmpty() && tabData.users.isEmpty() -> {
                        CircularProgressIndicator()
                    }
                    tabData.error != null && tabData.artworks.isEmpty() && tabData.novels.isEmpty() && tabData.users.isEmpty() -> {
                        ErrorDisplay(
                            message = tabData.error,
                            onRetry = onRetry
                        )
                    }
                    else -> {
                        when (tab) {
                            UserProfileTab.RECOMMEND_USERS -> {
                                // 推荐用户列表（使用UserCard组件）
                                RecommendUsersList(
                                    users = tabData.users,
                                    tab = tab,
                                    tabListStates = tabListStates,
                                    scrollIndex = recommendUsersScrollIndex,
                                    onUserClick = onUserClick,
                                    onArtworkClick = onArtworkClick,
                                    isLoading = tabData.isLoading,
                                    isRefreshing = tabData.isRefreshing,
                                    hasMore = tabData.hasMore,
                                    onRefresh = onRefresh
                                )
                            }
                            UserProfileTab.ILLUSTS, UserProfileTab.MANGA -> {
                                // 瀑布流展示插画/漫画（用户自己的作品，不显示作者信息）
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tag筛选行
                                    UserWorkTagFilterRow(
                                        tabData = tabData,
                                        scrollState = tagScrollState,
                                        onToggleExpand = onToggleTagFilter,
                                        onSelectTag = onSelectTag
                                    )
                                    
                                    ArtworkStaggeredGrid(
                                        artworks = tabData.artworks,
                                        tab = tab,
                                        scrollIndices = scrollIndices,
                                        tabListStates = tabListStates,
                                        onArtworkClick = onArtworkClick,
                                        onLoadMore = onLoadMore,
                                        isLoading = tabData.isLoading,
                                        isRefreshing = tabData.isRefreshing,
                                        onRefresh = onRefresh,
                                        showUserInfo = false
                                    )
                                }
                            }
                            UserProfileTab.NOVELS -> {
                                // 列表展示小说（用户自己的作品，不显示作者信息）
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tag筛选行
                                    UserWorkTagFilterRow(
                                        tabData = tabData,
                                        scrollState = tagScrollState,
                                        onToggleExpand = onToggleTagFilter,
                                        onSelectTag = onSelectTag
                                    )
                                    
                                    NovelList(
                                        novels = tabData.novels,
                                        tab = tab,
                                        tabListStates = tabListStates,
                                        onNovelClick = onNovelClick,
                                        onSeriesClick = onNovelSeriesClick,
                                        onTagClick = onTagClick,
                                        onLoadMore = onLoadMore,
                                        isLoading = tabData.isLoading,
                                        isRefreshing = tabData.isRefreshing,
                                        onRefresh = onRefresh,
                                        showUserInfo = false
                                    )
                                }
                            }
                            UserProfileTab.MANGA_SERIES -> {
                                // 漫画系列列表
                                MangaSeriesList(
                                    series = mangaSeries,
                                    tab = tab,
                                    tabListStates = tabListStates,
                                    onClick = { item ->
                                        onMangaSeriesClick(item.id)
                                    }
                                )
                            }
                            UserProfileTab.NOVEL_SERIES -> {
                                // 小说系列列表
                                NovelSeriesList(
                                    series = novelSeries,
                                    tab = tab,
                                    tabListStates = tabListStates,
                                    onClick = { item -> 
                                        onNovelSeriesClick(item.id)
                                    },
                                    onTagClick = onTagClick?.let { handler ->
                                        { tagName -> handler(com.projectu.shared.domain.model.Tag(tagName)) }
                                    }
                                )
                            }
                            UserProfileTab.BOOKMARK_ILLUSTS_PUBLIC,
                            UserProfileTab.BOOKMARK_ILLUSTS_PRIVATE -> {
                                // 收藏的插画·漫画（瀑布流展示，显示作者信息）
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tag筛选行
                                    BookmarkTagFilterRow(
                                        tabData = tabData,
                                        scrollState = tagScrollState,
                                        onToggleExpand = onToggleTagFilter,
                                        onSelectTag = onSelectTag
                                    )
                                    
                                    ArtworkStaggeredGrid(
                                        artworks = tabData.artworks,
                                        tab = tab,
                                        scrollIndices = scrollIndices,
                                        tabListStates = tabListStates,
                                        onArtworkClick = onArtworkClick,
                                        onUserClick = onUserClick,
                                        onLoadMore = onLoadMore,
                                        isLoading = tabData.isLoading,
                                        isRefreshing = tabData.isRefreshing,
                                        onRefresh = onRefresh,
                                        showUserInfo = true
                                    )
                                }
                            }
                            UserProfileTab.BOOKMARK_NOVELS_PUBLIC,
                            UserProfileTab.BOOKMARK_NOVELS_PRIVATE -> {
                                // 收藏的小说（列表展示，显示作者信息）
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tag筛选行
                                    BookmarkTagFilterRow(
                                        tabData = tabData,
                                        scrollState = tagScrollState,
                                        onToggleExpand = onToggleTagFilter,
                                        onSelectTag = onSelectTag
                                    )
                                    
                                    NovelList(
                                        novels = tabData.novels,
                                        tab = tab,
                                        tabListStates = tabListStates,
                                        onNovelClick = onNovelClick,
                                        onSeriesClick = onNovelSeriesClick,
                                        onUserClick = onUserClick,
                                        onTagClick = onTagClick,
                                        onLoadMore = onLoadMore,
                                        isLoading = tabData.isLoading,
                                        isRefreshing = tabData.isRefreshing,
                                        onRefresh = onRefresh,
                                        showUserInfo = true
                                    )
                                }
                            }
                            else -> {
                                // USER_INFO 已在上面处理
                            }
                        }
                    }
                }
            }
        }
        
        // 标签筛选弹窗（收藏Tab和用户作品Tab都支持）
        if ((tab.isBookmarkTab() || tab.isUserWorkTab()) && tabData.isTagDialogOpen) {
            val tags = if (tab.isBookmarkTab()) tabData.bookmarkTags else tabData.userWorkTags
            TagFilterDialog(
                tags = tags,
                selectedTag = tabData.selectedTag,
                onDismiss = onToggleTagFilter,
                onSelectTag = onSelectTag,
                isLoading = tabData.isLoadingTags
            )
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
    onUserClick: ((String) -> Unit)? = null,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    showUserInfo: Boolean = false
) {
    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()
    val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
    val columns by settingsCache.staggeredGridColumns.collectAsState()
    
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
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
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
            if (isLoading && !isRefreshing) {
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
 * 小说列表
 */
@Composable
fun NovelList(
    novels: List<Novel>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
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
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            items(novels, key = { it.id }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onSeriesClick = onSeriesClick,
                    onUserClick = onUserClick,
                    onTagClick = onTagClick,
                    showUserInfo = showUserInfo
                )
            }
            
            // 加载更多指示器
            if (isLoading && !isRefreshing) {
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
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
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
 * 推荐用户列表
 */
@Composable
fun RecommendUsersList(
    users: List<User>,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    scrollIndex: Int = 0,
    onUserClick: (String) -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    hasMore: Boolean = true,
    onRefresh: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    
    // 注册滚动状态
    LaunchedEffect(listState) {
        tabListStates[tab] = ListScrollState.LazyList(listState)
    }
    
    // 返回后恢复滚动位置
    LaunchedEffect(scrollIndex, users) {
        if (scrollIndex > 0 && users.isNotEmpty()) {
            // 计算需要滚动到的用户索引
            var accumulatedArtworks = 0
            var targetUserIndex = 0
            for ((index, user) in users.withIndex()) {
                val userArtworkCount = user.illusts.size
                if (accumulatedArtworks + userArtworkCount > scrollIndex) {
                    targetUserIndex = index
                    break
                }
                accumulatedArtworks += userArtworkCount
                targetUserIndex = index + 1
            }
            listState.animateScrollToItem(targetUserIndex.coerceAtMost(users.size - 1).coerceAtLeast(0))
        }
    }
    
    // 计算每个用户作品的起始索引
    val userArtworkStartIndices = remember(users) {
        val indices = mutableMapOf<Int, Int>()
        var currentIndex = 0
        users.forEachIndexed { userIndex, user ->
            indices[userIndex] = currentIndex
            currentIndex += user.illusts.size
        }
        indices
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        if (users.isEmpty() && !isLoading && !isRefreshing && !hasMore) {
            // 空状态提示（只有在加载完成后才显示）
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.user_recommend_users_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.user_recommend_users_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
                    val artworkStartIndex = userArtworkStartIndices.getOrElse(index) { 0 }
                    UserCard(
                        user = user,
                        onUserClick = { onUserClick(user.id) },
                        onArtworkClick = { artwork, localIndex ->
                            val globalIndex = artworkStartIndex + localIndex
                            onArtworkClick(artwork, globalIndex)
                        },
                        artworkStartIndex = 0  // 传入 0，因为我们在外部计算全局索引
                    )
                }
            }
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
    onClick: (NovelSeries) -> Unit,
    onTagClick: ((String) -> Unit)? = null
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
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        items(series, key = { it.id }) { item ->
            NovelSeriesCard(
                series = item,
                onClick = { onClick(item) },
                onTagClick = onTagClick
            )
        }
    }
}

/**
 * 用户详情信息内容
 */
@Composable
fun UserInfoContent(
    userDetailInfo: UserDetailInfo,
    tab: UserProfileTab,
    tabListStates: MutableMap<UserProfileTab, ListScrollState>,
    onBlockAuthor: (() -> Unit)? = null,
    onUnblockAuthor: (() -> Unit)? = null,
    isAuthorBlocked: Boolean = false
) {
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    
    // 注册滚动状态
    LaunchedEffect(listState) {
        tabListStates[tab] = ListScrollState.LazyList(listState)
    }
    
    // 使用 SelectionContainer 支持文本选择复制
    SelectionContainer {
        LazyColumn(
            state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // 用户ID
        item {
            UserInfoCard(title = stringResource(Res.string.user_info_basic)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserInfoRow(
                        label = stringResource(Res.string.user_info_user_id),
                        value = userDetailInfo.userId
                    )
                    
                    // 屏蔽/解除屏蔽按钮
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            if (isAuthorBlocked) {
                                onUnblockAuthor?.invoke()
                            } else {
                                onBlockAuthor?.invoke()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAuthorBlocked) {
                                stringResource(Res.string.action_unblock_author)
                            } else {
                                stringResource(Res.string.action_block_author)
                            }
                        )
                    }
                }
            }
        }
        
        // 个人简介（使用HtmlText支持链接）
        userDetailInfo.commentHtml?.takeIf { it.isNotBlank() }?.let { commentHtml ->
            item {
                UserInfoBioSection(
                    title = stringResource(Res.string.user_info_bio),
                    htmlContent = commentHtml,
                    onLinkClick = { url ->
                        // 处理 Pixiv 的 jump.php 链接
                        val actualUrl = if (url.contains("/jump.php?")) {
                            // 从 jump.php 链接中提取实际 URL
                            url.substringAfter("/jump.php?")
                                .let { java.net.URLDecoder.decode(it, "UTF-8") }
                        } else {
                            url
                        }
                        uriHandler.openUri(actualUrl)
                    }
                )
            }
        } ?: userDetailInfo.comment?.takeIf { it.isNotBlank() }?.let { comment ->
            // 如果没有 HTML 版本，使用纯文本
            item {
                UserInfoSection(
                    title = stringResource(Res.string.user_info_bio),
                    content = comment
                )
            }
        }
        
        // 社交关系
        item {
            UserInfoCard(title = stringResource(Res.string.user_info_social)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserInfoRow(
                        label = stringResource(Res.string.user_info_following),
                        value = userDetailInfo.following.toString()
                    )
                    UserInfoRow(
                        label = stringResource(Res.string.user_info_mypixiv),
                        value = userDetailInfo.mypixivCount.toString()
                    )
                    if (userDetailInfo.followedBack) {
                        UserInfoRow(
                            label = stringResource(Res.string.user_info_followed_back),
                            value = "✓"
                        )
                    }
                    if (userDetailInfo.isMypixiv) {
                        UserInfoRow(
                            label = stringResource(Res.string.user_info_is_mypixiv),
                            value = "✓"
                        )
                    }
                }
            }
        }
        
        // 社交媒体链接
        val hasSocialLinks = listOfNotNull(
            userDetailInfo.twitterUrl,
            userDetailInfo.facebookUrl,
            userDetailInfo.instagramUrl,
            userDetailInfo.tumblrUrl,
            userDetailInfo.pawooUrl,
            userDetailInfo.circlemsUrl,
            userDetailInfo.webpage
        ).isNotEmpty()
        
        if (hasSocialLinks) {
            item {
                UserInfoCard(title = stringResource(Res.string.user_info_links)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userDetailInfo.webpage?.let { url ->
                            UserInfoLinkRow(
                                label = stringResource(Res.string.user_info_webpage),
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.twitterUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Twitter/X",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.instagramUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Instagram",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.facebookUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Facebook",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.tumblrUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Tumblr",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.pawooUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Pawoo",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                        userDetailInfo.circlemsUrl?.let { url ->
                            UserInfoLinkRow(
                                label = "Circle.ms",
                                url = url,
                                onClick = { uriHandler.openUri(url) }
                            )
                        }
                    }
                }
            }
        }
        
        // 个人属性
        val hasPersonalInfo = listOfNotNull(
            userDetailInfo.region,
            userDetailInfo.age,
            userDetailInfo.birthDay,
            userDetailInfo.gender,
            userDetailInfo.job
        ).isNotEmpty()
        
        if (hasPersonalInfo) {
            item {
                UserInfoCard(title = stringResource(Res.string.user_info_personal)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userDetailInfo.region?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_region),
                                value = it
                            )
                        }
                        userDetailInfo.gender?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_gender),
                                value = it
                            )
                        }
                        userDetailInfo.age?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_age),
                                value = it
                            )
                        }
                        userDetailInfo.birthDay?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_birthday),
                                value = it
                            )
                        }
                        userDetailInfo.job?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_job),
                                value = it
                            )
                        }
                    }
                }
            }
        }
        
        // 工作环境
        val hasWorkspace = listOfNotNull(
            userDetailInfo.workspacePc,
            userDetailInfo.workspaceMonitor,
            userDetailInfo.workspaceTool,
            userDetailInfo.workspaceScanner,
            userDetailInfo.workspaceTablet,
            userDetailInfo.workspaceMouse,
            userDetailInfo.workspacePrinter,
            userDetailInfo.workspaceDesktop,
            userDetailInfo.workspaceMusic,
            userDetailInfo.workspaceDesk,
            userDetailInfo.workspaceChair,
            userDetailInfo.workspaceComment,
            userDetailInfo.workspaceImageUrl
        ).isNotEmpty()
        
        if (hasWorkspace) {
            item {
                UserInfoCard(title = stringResource(Res.string.user_info_workspace)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 工作环境图片
                        userDetailInfo.workspaceImageUrl?.let { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = stringResource(Res.string.user_info_workspace),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        userDetailInfo.workspacePc?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_pc),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceMonitor?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_monitor),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceTool?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_tool),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceTablet?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_tablet),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceMouse?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_mouse),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceScanner?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_scanner),
                                value = it
                            )
                        }
                        userDetailInfo.workspacePrinter?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_printer),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceDesktop?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_desktop),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceDesk?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_desk),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceChair?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_chair),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceMusic?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_music),
                                value = it
                            )
                        }
                        userDetailInfo.workspaceComment?.let {
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_workspace_comment),
                                value = it
                            )
                        }
                    }
                }
            }
        }
        
        // 接稿状态
        val hasCommission = userDetailInfo.commissionRequestStatus != null || 
                           userDetailInfo.commissionFanRequestStatus != null
        
        if (hasCommission) {
            item {
                UserInfoCard(title = stringResource(Res.string.user_info_commission)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userDetailInfo.commissionRequestStatus?.let { status ->
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_commission_request),
                                value = status
                            )
                        }
                        userDetailInfo.commissionFanRequestStatus?.let { status ->
                            UserInfoRow(
                                label = stringResource(Res.string.user_info_commission_fan_request),
                                value = status
                            )
                        }
                    }
                }
            }
        }
        
        // 群组
        if (userDetailInfo.groups.isNotEmpty()) {
            item {
                UserInfoCard(title = stringResource(Res.string.user_info_groups)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userDetailInfo.groups.forEach { group ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.iconUrl?.let { iconUrl ->
                                    AsyncImage(
                                        model = iconUrl,
                                        contentDescription = group.title,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 其他状态
        item {
            UserInfoCard(title = stringResource(Res.string.user_info_status)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userDetailInfo.premium) {
                        UserInfoRow(
                            label = stringResource(Res.string.user_info_premium),
                            value = "✓"
                        )
                    }
                    if (userDetailInfo.official) {
                        UserInfoRow(
                            label = stringResource(Res.string.user_info_official),
                            value = "✓"
                        )
                    }
                    if (userDetailInfo.canSendMessage) {
                        UserInfoRow(
                            label = stringResource(Res.string.user_info_can_message),
                            value = "✓"
                        )
                    }
                }
            }
        }
    }
    } // SelectionContainer
}

/**
 * 用户信息简介区域
 */
@Composable
private fun UserInfoSection(
    title: String,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 用户信息简介区域（支持HTML链接）
 */
@Composable
private fun UserInfoBioSection(
    title: String,
    htmlContent: String,
    onLinkClick: (url: String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HtmlText(
                html = htmlContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                onLinkClick = onLinkClick
            )
        }
    }
}

/**
 * 用户信息卡片
 */
@Composable
private fun UserInfoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

/**
 * 用户信息行
 */
@Composable
private fun UserInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * 用户信息链接行
 */
@Composable
private fun UserInfoLinkRow(
    label: String,
    url: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.7f)
        )
    }
}

/**
 * 收藏标签筛选行
 * 
 * 点击按钮弹出标签筛选弹窗，支持搜索和选择标签
 * 选中标签后，在按钮旁边显示标签芯片，点击可取消筛选
 * 
 * @param scrollState 保留参数用于兼容，但不再使用
 */
@Composable
fun BookmarkTagFilterRow(
    tabData: TabData,
    scrollState: ScrollState,
    onToggleExpand: () -> Unit,
    onSelectTag: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 按标签筛选按钮
            OutlinedButton(
                onClick = onToggleExpand,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.bookmark_tag_filter_button),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            // 显示当前选中的标签（如果有）
            if (tabData.selectedTag != null) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectTag(null) },
                    label = { 
                        Text(
                            text = tabData.selectedTag,
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.bookmark_tag_filter_clear),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

/**
 * 用户作品标签筛选行
 * 
 * 用于插画和小说Tab，点击按钮弹出标签筛选弹窗
 * 选中标签后，在按钮旁边显示标签芯片，点击可取消筛选
 * 
 * @param scrollState 保留参数用于兼容，但不再使用
 */
@Composable
fun UserWorkTagFilterRow(
    tabData: TabData,
    scrollState: ScrollState,
    onToggleExpand: () -> Unit,
    onSelectTag: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 按标签筛选按钮
            OutlinedButton(
                onClick = onToggleExpand,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.bookmark_tag_filter_button),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            // 显示当前选中的标签（如果有）
            if (tabData.selectedTag != null) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectTag(null) },
                    label = { 
                        Text(
                            text = tabData.selectedTag,
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.bookmark_tag_filter_clear),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

/**
 * 判断Tab是否为用户作品类型（支持标签筛选）
 */
fun UserProfileTab.isUserWorkTab(): Boolean = when (this) {
    UserProfileTab.ILLUSTS,
    UserProfileTab.MANGA,
    UserProfileTab.NOVELS -> true
    else -> false
}
