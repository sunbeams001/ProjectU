package com.projectu.ui.screens.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.PixivisionArticleCard
import com.projectu.ui.components.UserCard
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.components.TabbedNavigationBar
import com.projectu.ui.components.PageMapping
import com.projectu.ui.components.CustomTwoLayerMapper
import com.projectu.ui.components.rememberPagedNavigationState
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 发现页面统一内容区域
 * 参照排行榜设计，支持横向滑动切换内容类型
 * 
 * @param preferences 发现页导航偏好配置，用于控制显示哪些导航项
 * @param scrollIndices 滚动位置缓存
 * @param initialPageIndex 初始页面索引
 * @param onPageChanged 页面切换回调，用于保存当前页面索引
 * @param onRegisterScrollToTopOrRefreshCallback 注册滚动到顶部或刷新的回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoveryContent(
    preferences: com.projectu.shared.domain.model.DiscoveryNavigationPreferences = com.projectu.shared.domain.model.DiscoveryNavigationPreferences.DEFAULT,
    scrollIndices: MutableMap<String, Int> = mutableMapOf(),
    initialPageIndex: Int = 1,
    onPageChanged: ((Int) -> Unit)? = null,
    onRegisterScrollToTopOrRefreshCallback: ((() -> Unit) -> Unit)? = null
) {
    val parentNavigator = LocalNavigator.current?.parent
    
    // 1. 根据配置过滤内容类型
    val contentTypes = remember(preferences) {
        DiscoveryContentType.getAll().filter { 
            preferences.isContentTypeEnabled(it.name)
        }.ifEmpty { 
            // 保护措施：至少保留一个
            listOf(DiscoveryContentType.USERS)
        }
    }
    
    // 预先创建所有 ViewModel，避免切换时重新创建
    val usersViewModel: DiscoveryUsersViewModel = koinInject()
    val illustsViewModel: DiscoveryIllustsViewModel = koinInject()
    val novelsViewModel: DiscoveryNovelsViewModel = koinInject()
    val pixivisionViewModel: DiscoveryPixivisionViewModel = koinInject()
    
    // 收集所有状态
    val usersState by usersViewModel.state.collectAsState()
    val illustsState by illustsViewModel.state.collectAsState()
    val novelsState by novelsViewModel.state.collectAsState()
    val pixivisionState by pixivisionViewModel.state.collectAsState()
    
    // 为每个内容类型创建独立的列表状态缓存
    val listStates = remember {
        mutableStateMapOf<String, Any>()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 定义发现页面的映射信息
    data class DiscoveryPageMapping(
        override val primaryIndex: Int,
        override val secondaryIndex: Int,
        override val showSecondaryNav: Boolean,
        val contentType: DiscoveryContentType,
        val mode: DiscoveryMode?,
        val pixivisionCategory: com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory?
    ) : PageMapping
    
    //2. 根据配置为每个内容类型过滤二级导航项
    val modesForIllusts = remember(preferences) {
        val allModes = DiscoveryMode.entries
        allModes.filter { preferences.illustsEnabledModes.contains(it.name) }
            .ifEmpty { listOf(DiscoveryMode.ALL) }
    }
    
    val modesForNovels = remember(preferences) {
        val allModes = DiscoveryMode.entries
        allModes.filter { preferences.novelsEnabledModes.contains(it.name) }
            .ifEmpty { listOf(DiscoveryMode.ALL) }
    }
    
    val pixivisionCategoriesFiltered = remember(preferences) {
        val allCategories = listOf(
            com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory.ILLUSTRATION,
            com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory.MANGA
        )
        allCategories.filter { preferences.pixivisionEnabledCategories.contains(it.name) }
            .ifEmpty { listOf(com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory.ILLUSTRATION) }
    }
    
    // 3. 创建页码映射器（使用 CustomTwoLayerMapper）
    val mapper = remember(contentTypes, modesForIllusts, modesForNovels, pixivisionCategoriesFiltered) {
        // 计算每个内容类型的二级导航数量
        val secondaryCountsPerPrimary = contentTypes.map { contentType ->
            when (contentType) {
                DiscoveryContentType.USERS -> 1 // 用户没有二级导航
                DiscoveryContentType.ILLUSTS -> modesForIllusts.size
                DiscoveryContentType.NOVELS -> modesForNovels.size
                DiscoveryContentType.PIXIVISION -> pixivisionCategoriesFiltered.size
            }
        }
        
        CustomTwoLayerMapper(
            secondaryCountPerPrimary = secondaryCountsPerPrimary,
            createMapping = { primaryIndex, secondaryIndex, showSecondary ->
                val contentType = contentTypes[primaryIndex]
                when (contentType) {
                    DiscoveryContentType.USERS -> {
                        DiscoveryPageMapping(primaryIndex, secondaryIndex, false, contentType, null, null)
                    }
                    DiscoveryContentType.ILLUSTS -> {
                        val mode = modesForIllusts[secondaryIndex]
                        DiscoveryPageMapping(primaryIndex, secondaryIndex, true, contentType, mode, null)
                    }
                    DiscoveryContentType.NOVELS -> {
                        val mode = modesForNovels[secondaryIndex]
                        DiscoveryPageMapping(primaryIndex, secondaryIndex, true, contentType, mode, null)
                    }
                    DiscoveryContentType.PIXIVISION -> {
                        val category = pixivisionCategoriesFiltered[secondaryIndex]
                        DiscoveryPageMapping(primaryIndex, secondaryIndex, true, contentType, null, category)
                    }
                }
            }
        )
    }
    
    // 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, mapper.totalPages - 1),
        pageCount = { mapper.totalPages }
    )
    
    // 创建页码导航状态
    val navState = rememberPagedNavigationState(pagerState, mapper)
    
    // 监听页面切换，触发惰性加载并通知外部
    LaunchedEffect(pagerState.currentPage) {
        // 通知外部保存当前页面索引
        onPageChanged?.invoke(pagerState.currentPage)
        
        val mapping = mapper.parsePageIndex(pagerState.currentPage)
        when (mapping.contentType) {
            DiscoveryContentType.USERS -> usersViewModel.initLoadIfNeeded()
            DiscoveryContentType.ILLUSTS -> {
                illustsViewModel.initLoadIfNeeded()
                // 同步模式状态
                mapping.mode?.let { if (it != illustsState.currentMode) illustsViewModel.switchMode(it) }
            }
            DiscoveryContentType.NOVELS -> {
                novelsViewModel.initLoadIfNeeded()
                // 同步模式状态
                mapping.mode?.let { if (it != novelsState.currentMode) novelsViewModel.switchMode(it) }
            }
            DiscoveryContentType.PIXIVISION -> {
                pixivisionViewModel.initLoadIfNeeded()
                // 同步类别状态
                mapping.pixivisionCategory?.let { 
                    if (it != pixivisionState.currentCategory) pixivisionViewModel.switchCategory(it) 
                }
            }
        }
    }
    
    // 创建刷新回调
    val refreshCurrentPage: () -> Unit = {
        val mapping = mapper.parsePageIndex(pagerState.currentPage)
        when (mapping.contentType) {
            DiscoveryContentType.USERS -> usersViewModel.refresh()
            DiscoveryContentType.ILLUSTS -> illustsViewModel.refresh()
            DiscoveryContentType.NOVELS -> novelsViewModel.refresh()
            DiscoveryContentType.PIXIVISION -> pixivisionViewModel.refresh()
        }
    }
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = {
        val pageKey = "page_${pagerState.currentPage}"
        val listState = listStates[pageKey]
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
            refreshCurrentPage()
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
        // 双层导航：使用 TabbedNavigationBar（Tab + FilterChip）
        // 使用页码映射机制，滑动时先切换二级导航，再切换一级导航
        val currentMapping = navState.currentMapping
        
        // 根据当前一级导航动态确定二级导航项
        val secondaryItems = remember(currentMapping.contentType, modesForIllusts, modesForNovels, pixivisionCategoriesFiltered) {
            when (currentMapping.contentType) {
                DiscoveryContentType.ILLUSTS -> modesForIllusts
                DiscoveryContentType.NOVELS -> modesForNovels
                DiscoveryContentType.PIXIVISION -> pixivisionCategoriesFiltered
                DiscoveryContentType.USERS -> emptyList()
            }
        }
        
        TabbedNavigationBar(
            primaryItems = contentTypes,
            primarySelectedIndex = currentMapping.primaryIndex,
            onPrimaryItemClick = { index ->
                navState.handlePrimaryClick(
                    primaryIndex = index,
                    currentSecondaryIndex = currentMapping.secondaryIndex,
                    scope = coroutineScope,
                    onSamePage = scrollToTopOrRefresh
                )
            },
            getPrimaryItemLabel = { type -> stringResource(type.displayNameRes) },
            secondaryItems = secondaryItems,
            secondarySelectedIndex = currentMapping.secondaryIndex,
            onSecondaryItemClick = { secondaryIndex ->
                navState.handleSecondaryClick(
                    secondaryIndex = secondaryIndex,
                    currentPrimaryIndex = currentMapping.primaryIndex,
                    scope = coroutineScope,
                    onSamePage = scrollToTopOrRefresh
                )
            },
            getSecondaryItemLabel = { item ->
                when (item) {
                    is DiscoveryMode -> when (item) {
                        DiscoveryMode.ALL -> stringResource(Res.string.discovery_mode_all)
                        DiscoveryMode.SAFE -> stringResource(Res.string.discovery_mode_safe)
                        DiscoveryMode.R18 -> stringResource(Res.string.discovery_mode_r18)
                    }
                    is com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory -> when (item) {
                        com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory.ILLUSTRATION -> 
                            stringResource(Res.string.pixivision_illustration)
                        com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory.MANGA -> 
                            stringResource(Res.string.pixivision_manga)
                    }
                    else -> ""
                }
            },
            showSecondaryNav = currentMapping.showSecondaryNav,
            modifier = Modifier.fillMaxWidth()
        )
        
        // HorizontalPager：支持左右滑动切换所有页面
        // 滑动顺序：用户 → 插画-ALL → 插画-SAFE → 插画-R18 → 小说-ALL → 小说-SAFE → 小说-R18
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it }
        ) { page ->
            val mapping = mapper.parsePageIndex(page)
            val pageKey = "page_$page"
            
            when (mapping.contentType) {
                DiscoveryContentType.USERS -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    // 监听滚动索引变化，滚动到指定位置（这里是用户索引）
                    val targetScrollIndex by remember(pageKey) {
                        derivedStateOf { scrollIndices["users"] }
                    }
                    
                    LaunchedEffect(targetScrollIndex) {
                        val scrollIndex = targetScrollIndex
                        if (scrollIndex != null && scrollIndex > 0) {
                            listState.animateScrollToItem(scrollIndex)
                            scrollIndices.remove("users")
                        }
                    }
                    
                    // 创建响应式作品列表 State
                    val userArtworkIdsState = remember {
                        derivedStateOf {
                            usersState.users.flatMap { it.illusts?.map { it.id } ?: emptyList() }
                        }
                    }
                    
                    // 创建作品索引到用户索引的映射
                    val artworkToUserIndexMap = remember(usersState.users) {
                        val map = mutableMapOf<Int, Int>()
                        var artworkIndex = 0
                        usersState.users.forEachIndexed { userIndex, user ->
                            user.illusts?.forEach { _ ->
                                map[artworkIndex] = userIndex
                                artworkIndex++
                            }
                        }
                        map
                    }
                    
                    DiscoveryUsersPage(
                        state = usersState,
                        onLoadMore = usersViewModel::loadMore,
                        onRefresh = usersViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onUserClick = { user ->
                            // 跳转到用户详情页
                            parentNavigator?.push(UserScreen(user.id))
                        },
                        onArtworkClick = { artwork, artworkIndex ->
                            val userIndex = artworkToUserIndexMap[artworkIndex] ?: 0
                            val currentArtworkIds = userArtworkIdsState.value
                            
                            // 创建列表源
                            val listSource = usersViewModel.createArtworkListSource()
                            
                            // 创建导航上下文
                            val contextKey = NavigationContextManager.createContext(
                                listSource = listSource,
                                onReturnWithIndex = { lastArtworkIndex ->
                                    val targetUserIndex = artworkToUserIndexMap[lastArtworkIndex] ?: 0
                                    scrollIndices["users"] = targetUserIndex
                                }
                            )
                            
                            parentNavigator?.push(
                                ArtworkDetailScreen(
                                    artworkIds = currentArtworkIds,
                                    initialIndex = artworkIndex,
                                    contextKey = contextKey
                                )
                            )
                        },
                        listState = listState as LazyListState
                    )
                }
                
                DiscoveryContentType.ILLUSTS -> {
                    val mode = mapping.mode ?: DiscoveryMode.ALL
                    val listState = rememberLazyStaggeredGridState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    val scrollKey = "illusts_${mode.name}"
                    
                    // 监听滚动索引变化，滚动到指定位置（从详情页返回时）
                    val targetScrollIndex by remember(pageKey) {
                        derivedStateOf { scrollIndices[scrollKey] }
                    }
                    
                    LaunchedEffect(targetScrollIndex) {
                        val scrollIndex = targetScrollIndex
                        if (scrollIndex != null && scrollIndex > 0) {
                            listState.animateScrollToItem(scrollIndex)
                            scrollIndices.remove(scrollKey)
                        }
                    }
                    
                    DiscoveryIllustsPage(
                        state = illustsState,
                        onModeChange = { }, // 不再需要，通过滑动切换
                        onLoadMore = illustsViewModel::loadMore,
                        onRefresh = illustsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onArtworkClick = { artwork, index ->
                            val currentArtworkIds = illustsState.artworks.map { it.id }
                            
                            // 创建绑定到当前模式的列表源
                            val listSource = illustsViewModel.createArtworkListSource(mode)
                            
                            // 创建导航上下文
                            val contextKey = NavigationContextManager.createContext(
                                listSource = listSource,
                                onReturnWithIndex = { lastIndex ->
                                    scrollIndices[scrollKey] = lastIndex
                                }
                            )
                            
                            parentNavigator?.push(
                                ArtworkDetailScreen(
                                    artworkIds = currentArtworkIds,
                                    initialIndex = index,
                                    contextKey = contextKey
                                )
                            )
                        },
                        onUserClick = { userId ->
                            parentNavigator?.push(UserScreen(userId))
                        },
                        listState = listState as LazyStaggeredGridState
                    )
                }
                
                DiscoveryContentType.NOVELS -> {
                    val mode = mapping.mode ?: DiscoveryMode.ALL
                    val listState = rememberLazyListState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    // 创建Tag点击处理器
                    val scope = rememberCoroutineScope()
                    val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
                    val tagClickHandler = remember(parentNavigator) {
                        parentNavigator?.let { nav ->
                            TagClickHandler(nav, searchHistoryStore, scope)
                        }
                    }
                    
                    DiscoveryNovelsPage(
                        state = novelsState,
                        onModeChange = { }, // 不再需要，通过滑动切换
                        onLoadMore = novelsViewModel::loadMore,
                        onRefresh = novelsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onNovelClick = { novel ->
                            // 跳转到小说详情页
                            parentNavigator?.push(NovelDetailScreen(novelId = novel.id))
                        },
                        onSeriesClick = { seriesId ->
                            parentNavigator?.push(NovelSeriesScreen(seriesId))
                        },
                        onUserClick = { userId ->
                            parentNavigator?.push(UserScreen(userId))
                        },
                        onTagClick = tagClickHandler?.let { handler ->
                            { tag: com.projectu.shared.domain.model.Tag -> handler.handleTagClick(tag) }
                        },
                        listState = listState as LazyListState
                    )
                }
                
                DiscoveryContentType.PIXIVISION -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    DiscoveryPixivisionPage(
                        state = pixivisionState,
                        onLoadMore = pixivisionViewModel::loadMore,
                        onRefresh = pixivisionViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onArticleClick = { article ->
                            // 跳转到Pixivision文章详情页
                            parentNavigator?.push(
                                com.projectu.ui.screens.pixivision.PixivisionDetailScreen(article.id)
                            )
                        },
                        listState = listState
                    )
                }
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
    onArtworkClick: (Artwork, Int) -> Unit,
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
                        Text(stringResource(Res.string.common_retry))
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
 * 注意：第二层导航（Mode选择器）已集成到主导航栏中，此组件不再显示
 */
@Composable
fun DiscoveryIllustsPage(
    state: DiscoveryIllustsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
    listState: LazyStaggeredGridState
) {
    // 第二层导航已移到主导航栏，此处直接显示内容
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
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }
            state.artworks.isNotEmpty() -> {
                ArtworkStaggeredGridLayout(
                    artworks = state.artworks,
                    onArtworkClick = onArtworkClick,
                    onUserClick = onUserClick,
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

/**
 * 推荐小说页面内容
 * 注意：第二层导航（Mode选择器）已集成到主导航栏中，此组件不再显示
 */
@Composable
fun DiscoveryNovelsPage(
    state: DiscoveryNovelsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    listState: LazyListState
) {
    // 第二层导航已移到主导航栏，此处直接显示内容
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
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }
            state.novels.isNotEmpty() -> {
                NovelListLayout(
                    novels = state.novels,
                    onNovelClick = onNovelClick,
                    onSeriesClick = onSeriesClick,
                    onUserClick = onUserClick,
                    onTagClick = onTagClick,
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

/**
 * 用户列表布局（带下拉刷新）
 */
@Composable
fun UserListLayout(
    users: List<User>,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyListState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 构建全局作品列表和索引映射
    val allArtworks = remember(users) {
        users.flatMap { it.illusts ?: emptyList() }
    }
    
    // 预计算每个用户的作品起始索引（使用完整列表）
    val userArtworkStartIndices = remember(users) {
        var index = 0
        users.map { user ->
            val startIndex = index
            index += (user.illusts?.size ?: 0)
            startIndex
        }
    }
    
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

/**
 * 作品瀑布流布局（带下拉刷新）
 */
@Composable
fun ArtworkStaggeredGridLayout(
    artworks: List<Artwork>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
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
    
    val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
    val columns by settingsCache.staggeredGridColumns.collectAsState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = listState,
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
                    onUserClick = onUserClick
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
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
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
                    onClick = { onNovelClick(novel) },
                    onSeriesClick = onSeriesClick,
                    onUserClick = onUserClick,
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

/**
 * Pixivision 文章列表页面内容
 */
@Composable
fun DiscoveryPixivisionPage(
    state: DiscoveryPixivisionState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onArticleClick: (com.projectu.shared.data.remote.dto.pixivision.PixivisionArticle) -> Unit,
    listState: LazyListState
) {
    // 监听滚动到底部时自动加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val articles = state.getCurrentArticles()
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= articles.size - 3 && 
                    !state.isLoadingMore && 
                    state.getCurrentHasMore() &&
                    articles.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 初次加载中
            state.isLoading && state.getCurrentArticles().isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 加载失败
            state.error != null && state.getCurrentArticles().isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRefresh) {
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }
            
            // 显示列表
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = state.getCurrentArticles(),
                        key = { it.id }
                    ) { article ->
                        PixivisionArticleCard(
                            article = article,
                            onClick = { onArticleClick(article) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    
                    // 加载更多指示器
                    if (state.isLoadingMore && state.getCurrentHasMore()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // 没有更多数据提示
                    if (!state.getCurrentHasMore() && state.getCurrentArticles().isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.list_no_more_items),
                                    style = MaterialTheme.typography.bodySmall,
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
