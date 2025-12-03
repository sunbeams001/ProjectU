package com.projectu.ui.screens.followlatest.more

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.remote.mapper.toMangaSeries
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.ui.components.MangaSeriesCard
import com.projectu.ui.components.NovelSeriesCard
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 动态更多页面
 * 包含追更列表和好P友两个一级导航
 */
class FollowLatestMoreScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        FollowLatestMoreContent(
            navigator = navigator,
            onNavigateBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FollowLatestMoreContent(
    navigator: Navigator,
    onNavigateBack: () -> Unit
) {
    val contentTypes = remember { FollowLatestMoreContentType.getAll() }
    val coroutineScope = rememberCoroutineScope()
    
    // 预先创建所有 ViewModel
    val watchListMangaViewModel: WatchListMangaViewModel = koinInject()
    val watchListNovelsViewModel: WatchListNovelsViewModel = koinInject()
    
    // 收集所有状态
    val mangaState by watchListMangaViewModel.state.collectAsState()
    val novelsState by watchListNovelsViewModel.state.collectAsState()
    
    // 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { contentTypes.size }
    )
    
    // 监听页面切换，触发惰性加载
    LaunchedEffect(pagerState.currentPage) {
        when (contentTypes[pagerState.currentPage]) {
            FollowLatestMoreContentType.WATCH_LIST -> {
                // WatchList 页面会在自己的内部触发加载
            }
            FollowLatestMoreContentType.GOOD_P_FRIENDS -> {
                // 占位页面，无需加载
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.follow_latest_more_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
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
            // 第1层导航：追更列表 / 好P友
            SimpleNavigationBar(
                items = contentTypes,
                selectedIndex = pagerState.currentPage,
                onItemClick = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                getItemLabel = { type -> stringResource(type.displayNameRes) },
                modifier = Modifier.fillMaxWidth()
            )
            
            // HorizontalPager：支持左右滑动切换内容类型
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { contentTypes[it] }
            ) { page ->
                val contentType = contentTypes[page]
                
                when (contentType) {
                    FollowLatestMoreContentType.WATCH_LIST -> {
                        WatchListContent(
                            navigator = navigator,
                            mangaViewModel = watchListMangaViewModel,
                            novelsViewModel = watchListNovelsViewModel,
                            mangaState = mangaState,
                            novelsState = novelsState
                        )
                    }
                    FollowLatestMoreContentType.GOOD_P_FRIENDS -> {
                        GoodPFriendsPlaceholder()
                    }
                }
            }
        }
    }
}

/**
 * 追更列表内容
 * 包含漫画和小说两个二级导航
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchListContent(
    navigator: Navigator,
    mangaViewModel: WatchListMangaViewModel,
    novelsViewModel: WatchListNovelsViewModel,
    mangaState: WatchListMangaState,
    novelsState: WatchListNovelsState
) {
    val watchListTypes = remember { WatchListContentType.getAll() }
    val coroutineScope = rememberCoroutineScope()
    
    // 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { watchListTypes.size }
    )
    
    // 监听页面切换，触发惰性加载
    LaunchedEffect(pagerState.currentPage) {
        when (watchListTypes[pagerState.currentPage]) {
            WatchListContentType.MANGA -> mangaViewModel.initLoadIfNeeded()
            WatchListContentType.NOVELS -> novelsViewModel.initLoadIfNeeded()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 第2层导航：漫画 / 小说
        SimpleNavigationBar(
            items = watchListTypes,
            selectedIndex = pagerState.currentPage,
            onItemClick = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            getItemLabel = { type -> stringResource(type.displayNameRes) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // HorizontalPager：支持左右滑动切换
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { watchListTypes[it] }
        ) { page ->
            val contentType = watchListTypes[page]
            
            when (contentType) {
                WatchListContentType.MANGA -> {
                    WatchListMangaPage(
                        state = mangaState,
                        onLoadMore = mangaViewModel::loadMore,
                        onRefresh = mangaViewModel::refresh,
                        onSeriesClick = { series ->
                            // TODO: Navigate to manga series detail
                        }
                    )
                }
                WatchListContentType.NOVELS -> {
                    WatchListNovelsPage(
                        state = novelsState,
                        onLoadMore = novelsViewModel::loadMore,
                        onRefresh = novelsViewModel::refresh,
                        onSeriesClick = { series ->
                            series.id.toLongOrNull()?.let { seriesId ->
                                navigator.push(NovelSeriesScreen(seriesId))
                            }
                        }
                    )
                }
            }
        }
    }
}

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
        state.series.map { it.toMangaSeries() }
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
    onSeriesClick: (NovelSeries) -> Unit
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
 * 好P友占位页面
 */
@Composable
fun GoodPFriendsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.follow_latest_coming_soon),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.follow_latest_coming_soon_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
