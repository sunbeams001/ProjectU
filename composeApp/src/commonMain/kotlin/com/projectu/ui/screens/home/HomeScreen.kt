package com.projectu.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.projectu.ui.components.SimpleAdaptiveLayout
import com.projectu.ui.screens.settings.SettingsScreen
import com.projectu.ui.util.TagClickHandler
import com.projectu.ui.util.WindowSize
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.nav_home
import projectu.composeapp.generated.resources.nav_discovery
import projectu.composeapp.generated.resources.nav_ranking
import projectu.composeapp.generated.resources.nav_profile
import projectu.composeapp.generated.resources.nav_follow_latest
import projectu.composeapp.generated.resources.home_framework_complete
import projectu.composeapp.generated.resources.settings_title
import projectu.composeapp.generated.resources.discovery_recommended_users
import projectu.composeapp.generated.resources.discovery_recommended_illusts
import projectu.composeapp.generated.resources.discovery_recommended_novels
import com.projectu.ui.screens.discovery.DiscoveryContent
import com.projectu.ui.screens.ranking.RankingContent
import com.projectu.ui.screens.ranking.RankingViewModel
import com.projectu.ui.screens.followlatest.FollowLatestContent
import com.projectu.ui.screens.followlatest.FollowLatestIllustsViewModel
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.screens.user.UserScreenContent
import com.projectu.ui.screens.user.UserViewModel
import com.projectu.ui.screens.user.UserProfileTab
import com.projectu.ui.screens.userrelations.UserRelationsScreen
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.remote.api.PixivApi
import cafe.adriel.voyager.koin.koinScreenModel
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import org.koin.compose.koinInject
import com.projectu.ui.screens.search.SearchPreparationContent
import com.projectu.ui.screens.search.SearchPreparationViewModel

/**
 * 主屏幕 - 包含底部导航栏的容器
 * 使用自适应布局支持手机和平板
 */
class HomeScreen : Screen {
    
    @Composable
    override fun Content() {
        // 从设置中获取启动Tab配置
        val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository = koinInject()
        
        // 使用 produceState 等待设置加载完成
        val settingsState by produceState<com.projectu.shared.data.local.AppSettings?>(initialValue = null) {
            settingsRepository.getSettings().collect { settings ->
                value = settings
            }
        }
        
        // 等待设置加载完成
        val settings = settingsState
        if (settings == null) {
            // 显示加载指示器
            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        
        // 根据设置决定初始Tab
        val initialTab = remember(settings.defaultStartupTab, settings.lastUsedTab) {
            when (settings.defaultStartupTab) {
                com.projectu.shared.data.local.StartupTab.LAST_USED -> {
                    // 使用上次的Tab
                    when (settings.lastUsedTab) {
                        "HOME" -> HomeTab
                        "DISCOVERY" -> DiscoveryTab
                        "FOLLOW_LATEST" -> FollowLatestTab
                        "RANKING" -> RankingTab
                        "PROFILE" -> ProfileTab
                        else -> HomeTab
                    }
                }
                com.projectu.shared.data.local.StartupTab.HOME -> HomeTab
                com.projectu.shared.data.local.StartupTab.DISCOVERY -> DiscoveryTab
                com.projectu.shared.data.local.StartupTab.FOLLOW_LATEST -> FollowLatestTab
                com.projectu.shared.data.local.StartupTab.RANKING -> RankingTab
                com.projectu.shared.data.local.StartupTab.PROFILE -> ProfileTab
            }
        }
        
        SimpleAdaptiveLayout(
            phoneContent = { windowSize -> HomeScreenPhone(windowSize, initialTab, settingsRepository, settings) },
            tabletContent = { windowSize -> HomeScreenTablet(windowSize, initialTab, settingsRepository, settings) }
        )
    }
}

/**
 * 手机布局 - 底部导航栏
 */
@Composable
private fun HomeScreenPhone(
    windowSize: WindowSize,
    initialTab: Tab,
    settingsRepository: com.projectu.shared.domain.repository.SettingsRepository,
    settings: com.projectu.shared.data.local.AppSettings
) {
    val scope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    
    TabNavigator(initialTab) {
        // 监听Tab切换，只在LAST_USED模式下保存
        LaunchedEffect(it.current, settings.defaultStartupTab) {
            if (!isInitialized) {
                isInitialized = true
                return@LaunchedEffect
            }
            
            // 只有在LAST_USED模式下才保存Tab
            if (settings.defaultStartupTab != com.projectu.shared.data.local.StartupTab.LAST_USED) {
                return@LaunchedEffect
            }
            
            val tabName = when (it.current) {
                HomeTab -> "HOME"
                DiscoveryTab -> "DISCOVERY"
                FollowLatestTab -> "FOLLOW_LATEST"
                RankingTab -> "RANKING"
                ProfileTab -> "PROFILE"
                else -> "HOME"
            }
            settingsRepository.updateLastUsedTab(tabName)
        }
        
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(DiscoveryTab)
                    TabNavigationItem(FollowLatestTab)
                    TabNavigationItem(RankingTab)
                    TabNavigationItem(ProfileTab)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                it.current.Content()
            }
        }
    }
}

/**
 * 平板/桌面布局 - 侧边导航栏
 */
@Composable
private fun HomeScreenTablet(
    windowSize: WindowSize,
    initialTab: Tab,
    settingsRepository: com.projectu.shared.domain.repository.SettingsRepository,
    settings: com.projectu.shared.data.local.AppSettings
) {
    val scope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    
    TabNavigator(initialTab) {
        // 监听Tab切换，只在LAST_USED模式下保存
        LaunchedEffect(it.current, settings.defaultStartupTab) {
            if (!isInitialized) {
                isInitialized = true
                return@LaunchedEffect
            }
            
            // 只有在LAST_USED模式下才保存Tab
            if (settings.defaultStartupTab != com.projectu.shared.data.local.StartupTab.LAST_USED) {
                return@LaunchedEffect
            }
            
            val tabName = when (it.current) {
                HomeTab -> "HOME"
                DiscoveryTab -> "DISCOVERY"
                FollowLatestTab -> "FOLLOW_LATEST"
                RankingTab -> "RANKING"
                ProfileTab -> "PROFILE"
                else -> "HOME"
            }
            settingsRepository.updateLastUsedTab(tabName)
        }
        
        Row(modifier = Modifier.fillMaxSize()) {
            // 侧边导航栏
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = it.current == HomeTab,
                    onClick = { it.current = HomeTab },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_home)) }
                )
                NavigationRailItem(
                    selected = it.current == DiscoveryTab,
                    onClick = { 
                        if (it.current == DiscoveryTab) {
                            DiscoveryTab.triggerScrollToTopOrRefresh()
                        } else {
                            it.current = DiscoveryTab
                        }
                    },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_discovery)) }
                )
                NavigationRailItem(
                    selected = it.current == FollowLatestTab,
                    onClick = { 
                        if (it.current == FollowLatestTab) {
                            FollowLatestTab.triggerScrollToTopOrRefresh()
                        } else {
                            it.current = FollowLatestTab
                        }
                    },
                    icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_follow_latest)) }
                )
                NavigationRailItem(
                    selected = it.current == RankingTab,
                    onClick = { 
                        if (it.current == RankingTab) {
                            // 重复点击排行榜 Tab，触发刷新或滚动到顶部
                            RankingTab.triggerScrollToTopOrRefresh()
                        } else {
                            it.current = RankingTab
                        }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_ranking)) }
                )
                NavigationRailItem(
                    selected = it.current == ProfileTab,
                    onClick = { 
                        if (it.current == ProfileTab) {
                            ProfileTab.triggerScrollToTopOrRefresh()
                        } else {
                            it.current = ProfileTab
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_profile)) }
                )
            }
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                it.current.Content()
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current
    val title = when (tab) {
        HomeTab -> stringResource(Res.string.nav_home)
        DiscoveryTab -> stringResource(Res.string.nav_discovery)
        FollowLatestTab -> stringResource(Res.string.nav_follow_latest)
        RankingTab -> stringResource(Res.string.nav_ranking)
        ProfileTab -> stringResource(Res.string.nav_profile)
        else -> ""
    }
    val icon = when (tab) {
        HomeTab -> Icons.Default.Search
        DiscoveryTab -> Icons.Default.Explore
        FollowLatestTab -> Icons.Default.FavoriteBorder
        RankingTab -> Icons.Default.Star
        ProfileTab -> Icons.Default.Person
        else -> Icons.Default.Search
    }
    
    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { 
            if (tabNavigator.current == tab) {
                when (tab) {
                    DiscoveryTab -> DiscoveryTab.triggerScrollToTopOrRefresh()
                    FollowLatestTab -> FollowLatestTab.triggerScrollToTopOrRefresh()
                    RankingTab -> RankingTab.triggerScrollToTopOrRefresh()
                    ProfileTab -> ProfileTab.triggerScrollToTopOrRefresh()
                    else -> {}
                }
            } else {
                tabNavigator.current = tab
            }
        },
        icon = { Icon(icon, contentDescription = title) },
        label = { Text(title) }
    )
}

// 主页标签
object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Search)
            val title = stringResource(Res.string.nav_home)
            return remember(title) {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SearchPreparationViewModel>()
        val state by viewModel.state.collectAsState()
        val scope = rememberCoroutineScope()
        val parentNavigator = LocalNavigator.current?.parent
        
        // 监听Tab是否被覆盖（通过parentNavigator的栈大小判断）
        val parentNavSize = parentNavigator?.size ?: 0
        
        // 使用LaunchedEffect监听栈大小变化
        LaunchedEffect(parentNavSize) {
            // 当栈大小为1时（即返回到HomeTab），刷新搜索历史
            if (parentNavSize == 1) {
                viewModel.refreshHistory()
            }
        }
        
        HomeTabContent(
            state = state,
            onSearchKeywordChange = viewModel::onSearchKeywordChange,
            onSearch = {
                scope.launch {
                    val keyword = viewModel.performSearch()
                    if (keyword != null) {
                        // Navigate to search result screen using parent navigator
                        parentNavigator?.push(com.projectu.ui.screens.search.SearchResultScreen(keyword))
                    }
                }
            },
            onSearchHistoryClick = viewModel::onHistoryClick,
            onClearHistory = viewModel::clearHistory,
            viewModel = viewModel,
            parentNavigator = parentNavigator
        )
    }
}

@Composable
fun HomeTabContent(
    state: com.projectu.ui.screens.search.SearchPreparationState,
    onSearchKeywordChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onSearchHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    viewModel: com.projectu.ui.screens.search.SearchPreparationViewModel,
    parentNavigator: Navigator?
) {
    SearchPreparationContent(
        state = state,
        onSearchKeywordChange = onSearchKeywordChange,
        onSearch = onSearch,
        onAutocompleteSuggestionClick = viewModel::onAutocompleteSuggestionClick,
        onHistoryClick = onSearchHistoryClick,
        onRecommendationTagClick = viewModel::onRecommendationTagClick,
        onClearHistory = onClearHistory,
        onRefreshRecommendations = viewModel::refreshRecommendations,
        onThumbnailClick = { thumbnail ->
            // 根据 illustType 判断导航到插画还是小说详情页
            when (thumbnail.illustType) {
                3 -> parentNavigator?.push(NovelDetailScreen(novelId = thumbnail.id))
                else -> parentNavigator?.push(ArtworkDetailScreen(artworkId = thumbnail.id))
            }
        },
        onRemoveHistory = viewModel::removeHistory,
        onTogglePin = viewModel::togglePinHistory
    )
}

// 发现标签
object DiscoveryTab : Tab {
    // 用于触发刷新或滚动到顶部的事件
    private val _scrollToTopOrRefreshTrigger = mutableStateOf(0L)
    val scrollToTopOrRefreshTrigger: State<Long> = _scrollToTopOrRefreshTrigger
    
    // 将 scrollIndices 提升到 Tab 级别，避免导航时丢失
    private val scrollIndices = mutableStateMapOf<String, Int>()
    
    // 保存当前选中的内容类型页面索引，避免切换 Tab 时重置
    private var currentPageIndex = mutableIntStateOf(1) // 默认为 ILLUSTS (索引1)
    
    fun triggerScrollToTopOrRefresh() {
        _scrollToTopOrRefreshTrigger.value = System.currentTimeMillis()
    }
    
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Explore)
            val title = stringResource(Res.string.nav_discovery)
            return remember(title) {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        val scrollToTopOrRefreshCallback = remember { mutableStateOf<(() -> Unit)?>(null) }
        
        // 监听触发器
        LaunchedEffect(scrollToTopOrRefreshTrigger.value) {
            if (scrollToTopOrRefreshTrigger.value > 0) {
                scrollToTopOrRefreshCallback.value?.invoke()
            }
        }
        
        DiscoveryContent(
            scrollIndices = scrollIndices,
            initialPageIndex = currentPageIndex.intValue,
            onPageChanged = { index -> currentPageIndex.intValue = index },
            onRegisterScrollToTopOrRefreshCallback = { callback ->
                scrollToTopOrRefreshCallback.value = callback
            }
        )
    }
}

// 动态标签（关注用户最新作品）
object FollowLatestTab : Tab {
    // 用于触发刷新或滚动到顶部的事件
    private val _scrollToTopOrRefreshTrigger = mutableStateOf(0L)
    val scrollToTopOrRefreshTrigger: State<Long> = _scrollToTopOrRefreshTrigger
    
    // 将 scrollIndices 提升到 Tab 级别，避免导航时丢失
    private val scrollIndices = mutableStateMapOf<String, Int>()
    
    // 保存当前选中的内容类型页面索引，避免切换 Tab 时重置
    private var currentPageIndex = mutableIntStateOf(0) // 默认为 ILLUSTS (索引0)
    
    fun triggerScrollToTopOrRefresh() {
        _scrollToTopOrRefreshTrigger.value = System.currentTimeMillis()
    }
    
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.FavoriteBorder)
            val title = stringResource(Res.string.nav_follow_latest)
            return remember(title) {
                TabOptions(
                    index = 2u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        val scrollToTopOrRefreshCallback = remember { mutableStateOf<(() -> Unit)?>(null) }
        
        // 监听触发器
        LaunchedEffect(scrollToTopOrRefreshTrigger.value) {
            if (scrollToTopOrRefreshTrigger.value > 0) {
                scrollToTopOrRefreshCallback.value?.invoke()
            }
        }
        
        FollowLatestContent(
            scrollIndices = scrollIndices,
            initialPageIndex = currentPageIndex.intValue,
            onPageChanged = { index -> currentPageIndex.intValue = index },
            onRegisterScrollToTopOrRefreshCallback = { callback ->
                scrollToTopOrRefreshCallback.value = callback
            }
        )
    }
}

// 排行榜标签
object RankingTab : Tab {
    // 用于触发刷新或滚动到顶部的事件
    private val _scrollToTopOrRefreshTrigger = mutableStateOf(0L)
    val scrollToTopOrRefreshTrigger: State<Long> = _scrollToTopOrRefreshTrigger
    
    // 将 scrollIndices 提升到 Tab 级别，避免导航时丢失
    private val scrollIndices = mutableStateMapOf<String, Int>()
    
    fun triggerScrollToTopOrRefresh() {
        _scrollToTopOrRefreshTrigger.value = System.currentTimeMillis()
    }
    
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Star)
            val title = stringResource(Res.string.nav_ranking)
            return remember(title) {
                TabOptions(
                    index = 3u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<RankingViewModel>()
        val state by viewModel.state.collectAsState()
        val parentNavigator = LocalNavigator.current?.parent
        
        // 惰性加载：只在首次显示且没有数据时加载
        LaunchedEffect(Unit) {
            viewModel.initLoadIfNeeded()
        }
        
        // 用于管理刷新或滚动到顶部的触发
        val scrollToTopOrRefreshCallback = remember { mutableStateOf<(() -> Unit)?>(null) }
        
        // 监听触发器
        LaunchedEffect(scrollToTopOrRefreshTrigger.value) {
            if (scrollToTopOrRefreshTrigger.value > 0) {
                scrollToTopOrRefreshCallback.value?.invoke()
            }
        }
        
        // 创建Tag点击处理器
        val scope = rememberCoroutineScope()
        val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
        val tagClickHandler = remember(parentNavigator) {
            parentNavigator?.let { nav ->
                TagClickHandler(nav, searchHistoryStore, scope)
            }
        }
        
        RankingContent(
            state = state,
            scrollIndices = scrollIndices,
            onContentTypeChange = viewModel::switchContentType,
            onModeChange = viewModel::switchMode,
            onDateChange = viewModel::switchDate,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onArtworkClick = { artwork, index ->
                val currentMode = state.currentMode.value
                val currentArtworkIds = state.modeDataCache[currentMode]?.artworks?.map { it.id } ?: emptyList()
                
                // 创建绑定到当前 mode 的列表源
                val listSource = viewModel.createArtworkListSource(currentMode)
                
                // 创建导航上下文
                val contextKey = com.projectu.ui.navigation.NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { lastIndex ->
                        scrollIndices[state.currentMode.value] = lastIndex
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
            onRegisterScrollToTopOrRefreshCallback = { callback ->
                scrollToTopOrRefreshCallback.value = callback
            }
        )
    }
}

// 个人资料标签
object ProfileTab : Tab {
    // 用于触发刷新或滚动到顶部的事件
    private val _scrollToTopOrRefreshTrigger = mutableStateOf(0L)
    val scrollToTopOrRefreshTrigger: State<Long> = _scrollToTopOrRefreshTrigger
    
    // 将 scrollIndices 提升到 Tab 级别，避免导航时丢失
    private val scrollIndices = mutableStateMapOf<UserProfileTab, Int>()
    
    // 将 ViewModel 提升到 Tab 级别，确保实例稳定
    private var _viewModel: UserViewModel? = null
    
    fun triggerScrollToTopOrRefresh() {
        _scrollToTopOrRefreshTrigger.value = System.currentTimeMillis()
    }
    
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Person)
            val title = stringResource(Res.string.nav_profile)
            return remember(title) {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = icon
                )
            }
        }
    
    @Composable
    override fun Content() {
        // 获取或创建 ViewModel（提升到 object 级别保持稳定）
        val pixivApi: PixivApi = koinInject()
        val ageLimitDeterminer: AgeLimitDeterminer = koinInject()
        val tagTranslationUtil: TagTranslationUtil = koinInject()
        val syncArtworkStatesUseCase: SyncArtworkStatesUseCase = koinInject()
        val syncNovelStatesUseCase: SyncNovelStatesUseCase = koinInject()
        val stateCacheManager: StateCacheManager = koinInject()
        
        val viewModel = remember {
            _viewModel ?: UserViewModel(
                pixivApi = pixivApi,
                ageLimitDeterminer = ageLimitDeterminer,
                tagTranslationUtil = tagTranslationUtil,
                syncArtworkStatesUseCase = syncArtworkStatesUseCase,
                syncNovelStatesUseCase = syncNovelStatesUseCase,
                stateCacheManager = stateCacheManager
            ).also { _viewModel = it }
        }
        
        val state by viewModel.state.collectAsState()
        val parentNavigator = LocalNavigator.current?.parent
        val pixivConfigStore: PixivConfigStore = koinInject()
        
        // 获取当前登录用户ID
        val pixivConfig by pixivConfigStore.config
            .collectAsState(initial = com.projectu.shared.data.local.PixivConfig.DEFAULT)
        val currentUserId = pixivConfig.getUserId()?.toString()
        
        // 用于管理刷新或滚动到顶部的触发
        val scrollToTopOrRefreshCallback = remember { mutableStateOf<(() -> Unit)?>(null) }
        
        // 监听触发器
        LaunchedEffect(scrollToTopOrRefreshTrigger.value) {
            if (scrollToTopOrRefreshTrigger.value > 0) {
                scrollToTopOrRefreshCallback.value?.invoke()
            }
        }
        
        // 加载用户数据（loadUser 内部会判断是否需要重新加载）
        LaunchedEffect(currentUserId) {
            currentUserId?.let { userId ->
                viewModel.loadUser(userId)
            }
        }
        
        if (currentUserId == null) {
            // 未登录状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(Res.string.nav_profile),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    // 设置按钮
                    Button(
                        onClick = { parentNavigator?.push(SettingsScreen()) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.settings_title))
                    }
                }
            }
        } else {
            // 已登录状态 - 显示用户页面
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
                    parentNavigator?.push(
                        ArtworkDetailScreen(
                            artworkIds = currentArtworkIds,
                            initialIndex = index,
                            contextKey = contextKey
                        )
                    )
                },
                onNovelClick = { novel ->
                    // 跳转到小说详情页
                    parentNavigator?.push(NovelDetailScreen(novelId = novel.id))
                },
                onNovelSeriesClick = { seriesId ->
                    // 跳转到小说系列详情页
                    parentNavigator?.push(NovelSeriesScreen(seriesId))
                },
                onMangaSeriesClick = { seriesId ->
                    // 跳转到漫画系列详情页
                    parentNavigator?.push(MangaSeriesScreen(seriesId))
                },
                onUserClick = { clickedUserId ->
                    // 跳转到用户页面（如果不是当前用户）
                    if (clickedUserId != currentUserId) {
                        parentNavigator?.push(UserScreen(clickedUserId))
                    }
                },
                onFollowingClick = { clickedUserId, userName ->
                    // 跳转到用户关系页面
                    parentNavigator?.push(UserRelationsScreen(clickedUserId, userName))
                },
                onBackClick = { /* 不需要返回操作 */ },
                onToggleTagFilter = viewModel::toggleTagFilter,
                onSelectTag = viewModel::selectTag,
                showBackButton = false,
                showFollowIndicator = false,
                useScaffold = false,
                topBarActions = {
                    // 设置按钮
                    IconButton(onClick = { parentNavigator?.push(SettingsScreen()) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings_title)
                        )
                    }
                },
                onRegisterScrollToTopOrRefreshCallback = { callback ->
                    scrollToTopOrRefreshCallback.value = callback
                }
            )
        }
    }
}
