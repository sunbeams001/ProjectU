package com.projectu.ui.screens.followlatest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectu.shared.data.remote.mapper.toMangaSeries
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.ui.components.MangaSeriesCard
import com.projectu.ui.components.NovelSeriesCard
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 漫画追更列表页面
 */
@Composable
fun WatchListMangaPage(
    state: WatchListMangaState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSeriesClick: (MangaSeries) -> Unit
) {
    val listState = rememberLazyListState()
    
    // 转换为领域模型
    val mangaSeriesList = remember(state.series) {
        state.series.map { it.toMangaSeries() }  // 使用默认参数，追更列表中没有作者名
    }
    
    // 监听滚动到底部，加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && !state.isLastPage) {
                onLoadMore()
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.series.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null && state.series.isEmpty() -> {
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
            state.series.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.common_no_data),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading && state.series.isNotEmpty(),
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = mangaSeriesList,
                            key = { it.id }
                        ) { series ->
                            MangaSeriesCard(
                                series = series,
                                onClick = { onSeriesClick(series) }
                            )
                        }
                        
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 小说追更列表页面
 */
@Composable
fun WatchListNovelsPage(
    state: WatchListNovelsState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSeriesClick: (NovelSeries) -> Unit,
    onTagClick: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    
    // 转换为领域模型
    val novelSeriesList = remember(state.series) {
        state.series.map { it.toNovelSeries() }
    }
    
    // 监听滚动到底部，加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && !state.isLastPage) {
                onLoadMore()
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.series.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null && state.series.isEmpty() -> {
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
            state.series.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.common_no_data),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading && state.series.isNotEmpty(),
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = novelSeriesList,
                            key = { it.id }
                        ) { series ->
                            NovelSeriesCard(
                                series = series,
                                onClick = { onSeriesClick(series) },
                                onTagClick = onTagClick
                            )
                        }
                        
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
