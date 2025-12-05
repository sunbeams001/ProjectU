package com.projectu.ui.screens.userrelations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.components.UserCard
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 用户关系页面
 * 
 * 显示用户的关注列表、好P友列表和粉丝列表
 * 
 * @param userId 用户ID
 * @param userName 用户名
 * @param initialPageKey 初始页面的 key（用于序列化）
 */
data class UserRelationsScreen(
    private val userId: String,
    private val userName: String,
    private val initialPageKey: String = RelationPage.FollowingPublic.key
) : Screen {
    
    override val key: ScreenKey = "UserRelationsScreen_$userId"
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<UserRelationsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 初始化（将 key 转换为 RelationPage）
        val initialPage = remember(initialPageKey) { RelationPage.fromKey(initialPageKey) }
        LaunchedEffect(userId) {
            viewModel.initialize(userId, userName, initialPage)
        }
        
        UserRelationsContent(
            state = state,
            viewModel = viewModel,
            onPageChange = viewModel::switchPage,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onUserClick = { user ->
                navigator.push(UserScreen(user.id))
            },
            onArtworkClick = { artwork, index, allArtworkIds, pageKey ->
                // 创建列表源
                val listSource = viewModel.createArtworkListSource(pageKey)
                
                // 创建导航上下文（使用 ViewModel 管理滚动位置）
                val contextKey = NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { returnIndex ->
                        viewModel.setScrollPosition(pageKey, returnIndex)
                    }
                )
                
                // 跳转到作品详情页
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = allArtworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserRelationsContent(
    state: UserRelationsScreenState,
    viewModel: UserRelationsViewModel,
    onPageChange: (RelationPage) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int, List<String>, String) -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 为每个页面维护独立的列表状态
    val listStates = remember { mutableStateMapOf<String, LazyListState>() }
    
    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = state.availablePages.indexOf(state.currentPage).coerceAtLeast(0),
        pageCount = { state.availablePages.size }
    )
    
    // 同步 Pager 页面切换到 ViewModel
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newPage = state.availablePages.getOrNull(pagerState.currentPage)
            if (newPage != null && newPage != state.currentPage) {
                onPageChange(newPage)
            }
        }
    }
    
    // 当外部切换页面时，同步到 Pager
    LaunchedEffect(state.currentPage) {
        val targetPage = state.availablePages.indexOf(state.currentPage)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    // 处理导航栏点击
    val handlePrimaryNavClick: (Int) -> Unit = { primaryIndex ->
        val targetPage = when (primaryIndex) {
            0 -> if (state.currentSecondaryIndex == 1) RelationPage.FollowingPrivate else RelationPage.FollowingPublic
            1 -> RelationPage.MyPixiv
            2 -> RelationPage.Followers
            else -> RelationPage.FollowingPublic
        }
        
        if (targetPage == state.currentPage) {
            // 点击已选中项：滚动到顶部或刷新
            val listState = listStates[state.currentPage.key]
            if (listState?.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                onRefresh()
            } else {
                coroutineScope.launch {
                    listState?.animateScrollToItem(0)
                }
            }
        } else {
            onPageChange(targetPage)
        }
    }
    
    val handleSecondaryNavClick: (Int) -> Unit = { secondaryIndex ->
        val targetPage = if (secondaryIndex == 0) RelationPage.FollowingPublic else RelationPage.FollowingPrivate
        
        if (targetPage == state.currentPage) {
            // 点击已选中项：滚动到顶部或刷新
            val listState = listStates[state.currentPage.key]
            if (listState?.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                onRefresh()
            } else {
                coroutineScope.launch {
                    listState?.animateScrollToItem(0)
                }
            }
        } else {
            onPageChange(targetPage)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(Res.string.user_relations_title, state.userName))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 一级导航栏
            PrimaryNavigationBar(
                isSelf = state.isSelf,
                currentPrimaryIndex = state.currentPrimaryIndex,
                onPrimaryClick = handlePrimaryNavClick
            )
            
            // 二级导航栏（仅对"已关注"显示）
            if (state.hasSecondaryNavigation) {
                SecondaryNavigationBar(
                    currentSecondaryIndex = state.currentSecondaryIndex,
                    onSecondaryClick = handleSecondaryNavClick
                )
            }
            
            // 内容区域 - HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { state.availablePages.getOrNull(it)?.key ?: it }
            ) { pageIndex ->
                val page = state.availablePages[pageIndex]
                val pageData = state.pageDataCache[page.key] ?: RelationPageData()
                
                // 获取或创建列表状态
                val listState = listStates.getOrPut(page.key) { LazyListState() }
                
                // 收集所有作品ID（用于列表导航，使用完整列表）
                val allArtworkIds = remember(pageData.users) {
                    pageData.users.flatMap { user -> user.illusts.map { it.id } }
                }
                
                // 响应 scrollTargets 的变化执行滚动
                val scrollTarget = state.scrollTargets[page.key]
                LaunchedEffect(scrollTarget) {
                    if (scrollTarget != null && scrollTarget > 0) {
                        listState.animateScrollToItem(scrollTarget)
                        viewModel.clearScrollTarget(page.key)
                    }
                }
                
                UserListContent(
                    pageData = pageData,
                    listState = listState,
                    pageKey = page.key,
                    allArtworkIds = allArtworkIds,
                    onUserClick = onUserClick,
                    onArtworkClick = onArtworkClick,
                    onLoadMore = onLoadMore,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

/**
 * 一级导航栏
 */
@Composable
fun PrimaryNavigationBar(
    isSelf: Boolean,
    currentPrimaryIndex: Int,
    onPrimaryClick: (Int) -> Unit
) {
    val items = if (isSelf) {
        listOf(
            UserRelationType.FOLLOWING,
            UserRelationType.MY_PIXIV,
            UserRelationType.FOLLOWERS
        )
    } else {
        listOf(
            UserRelationType.FOLLOWING,
            UserRelationType.MY_PIXIV
        )
    }
    
    SimpleNavigationBar(
        items = items,
        selectedIndex = currentPrimaryIndex,
        onItemClick = onPrimaryClick,
        getItemLabel = { type ->
            stringResource(type.displayNameRes)
        }
    )
}

/**
 * 二级导航栏
 */
@Composable
fun SecondaryNavigationBar(
    currentSecondaryIndex: Int,
    onSecondaryClick: (Int) -> Unit
) {
    val items = listOf(
        FollowingVisibility.PUBLIC,
        FollowingVisibility.PRIVATE
    )
    
    SimpleNavigationBar(
        items = items,
        selectedIndex = currentSecondaryIndex,
        onItemClick = onSecondaryClick,
        getItemLabel = { visibility ->
            stringResource(visibility.displayNameRes)
        }
    )
}

/**
 * 用户列表内容
 */
@Composable
fun UserListContent(
    pageData: RelationPageData,
    listState: LazyListState,
    pageKey: String,
    allArtworkIds: List<String>,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int, List<String>, String) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState, pageData.users.size, pageData.isLoading) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !pageData.isLoading && pageData.hasMore) {
                onLoadMore()
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            (pageData.isLoading || pageData.isRefreshing) && pageData.users.isEmpty() -> {
                CircularProgressIndicator()
            }
            pageData.error != null && pageData.users.isEmpty() -> {
                ErrorDisplay(
                    message = pageData.error,
                    onRetry = onRefresh
                )
            }
            pageData.users.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.user_relations_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                // 预计算每个用户的作品起始索引（使用完整列表）
                val userArtworkStartIndices = remember(pageData.users) {
                    var index = 0
                    pageData.users.map { user ->
                        val startIndex = index
                        index += user.illusts.size
                        startIndex
                    }
                }
                
                PullToRefreshBox(
                    isRefreshing = pageData.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(pageData.users, key = { _, user -> user.id }) { index, user ->
                            val artworkStartIndex = userArtworkStartIndices.getOrElse(index) { 0 }
                            UserCard(
                                user = user,
                                onUserClick = onUserClick,
                                onArtworkClick = { artwork, localIndex ->
                                    val globalIndex = artworkStartIndex + localIndex
                                    onArtworkClick(artwork, globalIndex, allArtworkIds, pageKey)
                                },
                                artworkStartIndex = 0  // 传入 0，因为我们在外部计算全局索引
                            )
                        }
                        
                        // 加载更多指示器
                        if (pageData.isLoading && pageData.users.isNotEmpty()) {
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
}
