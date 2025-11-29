package com.projectu.ui.screens.discovery

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.user.UserScreen
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.discovery_recommended_illusts
import projectu.composeapp.generated.resources.discovery_mode_all
import projectu.composeapp.generated.resources.discovery_mode_safe
import projectu.composeapp.generated.resources.discovery_mode_r18

/**
 * 发现插画页面
 */
class DiscoveryIllustsScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscoveryIllustsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 保存滚动位置，用于返回时恢复
        val scrollToIndex = remember { mutableStateOf<Int?>(null) }
        
        // 惰性加载：只在首次显示且没有数据时加载
        LaunchedEffect(Unit) {
            viewModel.initLoadIfNeeded()
        }
        
        // 创建响应式的作品ID列表 State
        val artworkIdsState by remember {
            derivedStateOf {
                state.artworks.map { it.id }
            }
        }
        
        // 将列表包装为 mutableStateOf 以便传递
        val artworkIdsStateWrapper = remember { mutableStateOf(artworkIdsState) }
        artworkIdsStateWrapper.value = artworkIdsState
        
        DiscoveryIllustsContent(
            state = state,
            scrollToIndex = scrollToIndex.value,
            onScrollComplete = { scrollToIndex.value = null },
            onModeChange = viewModel::switchMode,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onArtworkClick = { artwork, index ->
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = artworkIdsStateWrapper,
                        initialIndex = index,
                        onLoadMore = { viewModel.loadMore() },
                        onReturnWithIndex = { lastIndex ->
                            // 返回时设置滚动目标
                            scrollToIndex.value = lastIndex
                        }
                    )
                )
            },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryIllustsContent(
    state: DiscoveryIllustsState,
    scrollToIndex: Int? = null,
    onScrollComplete: () -> Unit = {},
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onArtworkClick: (artwork: com.projectu.shared.domain.model.Artwork, index: Int) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.discovery_recommended_illusts)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
            // Mode 切换导航
            SimpleNavigationBar(
                items = DiscoveryMode.entries,
                selectedIndex = DiscoveryMode.entries.indexOf(state.currentMode),
                onItemClick = { index -> onModeChange(DiscoveryMode.entries[index]) },
                getItemLabel = { mode ->
                    when (mode) {
                        DiscoveryMode.ALL -> stringResource(Res.string.discovery_mode_all)
                        DiscoveryMode.SAFE -> stringResource(Res.string.discovery_mode_safe)
                        DiscoveryMode.R18 -> stringResource(Res.string.discovery_mode_r18)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.artworks.isEmpty() -> {
                        // 初次加载
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null && state.artworks.isEmpty() -> {
                        // 错误状态
                        ErrorDisplay(
                            message = state.error,
                            onRetry = onRefresh,
                            modifier = Modifier.align(Alignment.Center),
                            isFullScreen = true
                        )
                    }
                    state.artworks.isNotEmpty() -> {
                        // 瀑布流展示
                        ArtworkStaggeredGrid(
                            artworks = state.artworks,
                            onArtworkClick = onArtworkClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = state.isLoadingMore,
                            scrollToIndex = scrollToIndex,
                            onScrollComplete = onScrollComplete
                        )
                    }
                }
            }
        }
    }
}

/**
 * 作品瀑布流网格
 */
@Composable
fun ArtworkStaggeredGrid(
    artworks: List<com.projectu.shared.domain.model.Artwork>,
    onArtworkClick: (artwork: com.projectu.shared.domain.model.Artwork, index: Int) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    scrollToIndex: Int? = null,
    onScrollComplete: () -> Unit = {}
) {
    val listState = rememberLazyStaggeredGridState()
    
    // 监听 scrollToIndex 变化，滚动到指定位置
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex != null && scrollToIndex > 0) {
            println("[DiscoveryIllusts] 开始滚动到索引: $scrollToIndex")
            
            // 平滑滚动
            listState.animateScrollToItem(scrollToIndex)

            
            onScrollComplete()
        }
    }
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10) {
                    onLoadMore()
                }
            }
    }
    
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(3),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = artworks,
            key = { it.id }
        ) { artwork ->
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
