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
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.projectu.ui.components.SimpleAdaptiveLayout
import com.projectu.ui.screens.settings.SettingsScreen
import com.projectu.ui.util.WindowSize
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.nav_home
import projectu.composeapp.generated.resources.nav_discovery
import projectu.composeapp.generated.resources.nav_ranking
import projectu.composeapp.generated.resources.nav_profile
import projectu.composeapp.generated.resources.home_framework_complete
import projectu.composeapp.generated.resources.settings_title
import projectu.composeapp.generated.resources.discovery_recommended_users
import projectu.composeapp.generated.resources.discovery_recommended_illusts
import projectu.composeapp.generated.resources.discovery_recommended_novels
import com.projectu.ui.screens.discovery.DiscoveryContent
import com.projectu.ui.screens.ranking.RankingContent
import com.projectu.ui.screens.ranking.RankingViewModel
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import cafe.adriel.voyager.koin.koinScreenModel

/**
 * 主屏幕 - 包含底部导航栏的容器
 * 使用自适应布局支持手机和平板
 */
class HomeScreen : Screen {
    
    @Composable
    override fun Content() {
        SimpleAdaptiveLayout(
            phoneContent = { windowSize -> HomeScreenPhone(windowSize) },
            tabletContent = { windowSize -> HomeScreenTablet(windowSize) }
        )
    }
}

/**
 * 手机布局 - 底部导航栏
 */
@Composable
private fun HomeScreenPhone(windowSize: WindowSize) {
    TabNavigator(HomeTab) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(DiscoveryTab)
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
private fun HomeScreenTablet(windowSize: WindowSize) {
    TabNavigator(HomeTab) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 侧边导航栏
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = it.current == HomeTab,
                    onClick = { it.current = HomeTab },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
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
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_discovery)) }
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
                    onClick = { it.current = ProfileTab },
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
        RankingTab -> stringResource(Res.string.nav_ranking)
        ProfileTab -> stringResource(Res.string.nav_profile)
        else -> ""
    }
    val icon = when (tab) {
        HomeTab -> Icons.Default.Home
        DiscoveryTab -> Icons.Default.Search
        RankingTab -> Icons.Default.Star
        ProfileTab -> Icons.Default.Person
        else -> Icons.Default.Home
    }
    
    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { 
            if (tabNavigator.current == tab) {
                when (tab) {
                    DiscoveryTab -> DiscoveryTab.triggerScrollToTopOrRefresh()
                    RankingTab -> RankingTab.triggerScrollToTopOrRefresh()
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
            val icon = rememberVectorPainter(Icons.Default.Home)
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
        HomeTabContent()
    }
}

@Composable
fun HomeTabContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.nav_home),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(Res.string.home_framework_complete),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            val icon = rememberVectorPainter(Icons.Default.Search)
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
                    index = 2u,
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
            onRegisterScrollToTopOrRefreshCallback = { callback ->
                scrollToTopOrRefreshCallback.value = callback
            }
        )
    }
}

// 个人资料标签
object ProfileTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Person)
            val title = stringResource(Res.string.nav_profile)
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
        val navigator = LocalNavigator.currentOrThrow
        
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
                    onClick = { navigator.parent?.push(SettingsScreen()) },
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
    }
}
