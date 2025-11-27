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
                // TODO: 跳转到小说详情页
                println("点击小说: ${novel.title}")
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
            // Mode 切换导航（复用ModeSelector）
            ModeSelector(
                currentMode = state.currentMode,
                onModeChange = onModeChange,
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
 * 小说列表（单列）
 */
@Composable
fun NovelList(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val listState = rememberLazyListState()
    
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
                onClick = { onNovelClick(novel) }
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


