package com.projectu.ui.screens.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.projectu.shared.domain.model.Novel
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.discovery_recommended_novels
import projectu.composeapp.generated.resources.discovery_mode_all
import projectu.composeapp.generated.resources.discovery_mode_safe
import projectu.composeapp.generated.resources.discovery_mode_r18

/**
 * 发现小说页面
 */
class DiscoveryNovelsScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscoveryNovelsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 惰性加载：只在首次显示且没有数据时加载
        LaunchedEffect(Unit) {
            viewModel.initLoadIfNeeded()
        }
        
        DiscoveryNovelsContent(
            state = state,
            onModeChange = viewModel::switchMode,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onNovelClick = { novel ->
                // 跳转到小说详情页
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onSeriesClick = { seriesId ->
                navigator.push(NovelSeriesScreen(seriesId))
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
fun DiscoveryNovelsContent(
    state: DiscoveryNovelsState,
    onModeChange: (DiscoveryMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.discovery_recommended_novels)) },
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
                    state.isLoading && state.novels.isEmpty() -> {
                        // 初次加载
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null && state.novels.isEmpty() -> {
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
                    state.novels.isNotEmpty() -> {
                        // 小说列表展示
                        NovelList(
                            novels = state.novels,
                            onNovelClick = onNovelClick,
                            onSeriesClick = onSeriesClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = state.isLoadingMore,
                            currentMode = state.currentMode
                        )
                    }
                }
            }
        }
    }
}

/**
 * 小说列表（单列）
 */
@Composable
fun NovelList(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (Long) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    currentMode: DiscoveryMode = DiscoveryMode.ALL
) {
    // 为每个模式保存独立的滚动位置
    val scrollPositions = remember { mutableStateMapOf<DiscoveryMode, Int>() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollPositions[currentMode] ?: 0
    )
    
    // 当模式变化时，保存当前滚动位置，然后恢复目标模式的滚动位置
    var previousMode by remember { mutableStateOf(currentMode) }
    LaunchedEffect(currentMode) {
        if (previousMode != currentMode) {
            // 保存上一个模式的滚动位置
            scrollPositions[previousMode] = listState.firstVisibleItemIndex
            
            // 恢复目标模式的滚动位置
            val targetIndex = scrollPositions[currentMode] ?: 0
            if (targetIndex > 0) {
                listState.scrollToItem(targetIndex)
            } else {
                listState.scrollToItem(0)
            }
            previousMode = currentMode
        }
    }
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState, novels.size, isLoadingMore) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            // 当最后一个可见项的索引 >= 总项数 - 5 时触发加载更多
            lastVisibleItem?.index?.let { it >= totalItems - 5 } ?: false
        }
        .distinctUntilChanged() // 避免重复触发
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }
    
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
                onUserClick = onUserClick
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


