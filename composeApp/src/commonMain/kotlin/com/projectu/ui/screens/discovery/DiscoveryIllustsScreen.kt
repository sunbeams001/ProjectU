package com.projectu.ui.screens.discovery

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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.ui.components.ArtworkCard
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
        
        DiscoveryIllustsContent(
            state = state,
            onModeChange = viewModel::switchMode,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onArtworkClick = { artwork ->
                // TODO: 跳转到作品详情页
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryIllustsContent(
    state: DiscoveryIllustsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onArtworkClick: (com.projectu.shared.domain.model.Artwork) -> Unit,
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
            ModeSelector(
                currentMode = state.currentMode,
                onModeChange = onModeChange,
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
                        // 瀑布流展示
                        ArtworkStaggeredGrid(
                            artworks = state.artworks,
                            onArtworkClick = onArtworkClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = state.isLoadingMore
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mode 选择器
 */
@Composable
fun ModeSelector(
    currentMode: DiscoveryMode,
    onModeChange: (DiscoveryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscoveryMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
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
 * 作品瀑布流网格
 */
@Composable
fun ArtworkStaggeredGrid(
    artworks: List<com.projectu.shared.domain.model.Artwork>,
    onArtworkClick: (com.projectu.shared.domain.model.Artwork) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val listState = rememberLazyStaggeredGridState()
    
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
