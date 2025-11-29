package com.projectu.ui.screens.ranking

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.projectu.shared.data.remote.model.RankingContent
import kotlinx.coroutines.launch
import com.projectu.shared.data.remote.model.RankingContentModeConfig
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.NavigationBar
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 排行榜内容区域
 * 用于在 HomeScreen 的 RankingTab 中显示
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankingContent(
    state: RankingState,
    scrollIndices: MutableMap<String, Int> = mutableMapOf(),
    onContentTypeChange: (RankingContent) -> Unit,
    onModeChange: (RankingMode) -> Unit,
    onDateChange: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onArtworkClick: (artwork: com.projectu.shared.domain.model.Artwork, index: Int) -> Unit,
    onNovelClick: (com.projectu.shared.domain.model.Novel) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onRegisterScrollToTopOrRefreshCallback: ((() -> Unit) -> Unit)? = null
) {
    // 获取当前内容类型支持的所有模式
    val supportedModes = remember(state.currentContentType) {
        RankingContentModeConfig.getSupportedModes(state.currentContentType)
    }
    
    // 为每个 mode 创建独立的列表状态缓存
    val listStates = remember(state.currentContentType) {
        mutableStateMapOf<String, Any>()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = remember(state.currentMode, listStates) {
        {
            val currentMode = state.currentMode
            val listState = listStates[currentMode.value]
            val isAtTop = when (listState) {
                is androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState -> 
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                is androidx.compose.foundation.lazy.LazyListState -> 
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                else -> true
            }
            
            if (isAtTop) {
                onRefresh()
            } else {
                coroutineScope.launch {
                    when (listState) {
                        is androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState -> 
                            listState.animateScrollToItem(0)
                        is androidx.compose.foundation.lazy.LazyListState -> 
                            listState.animateScrollToItem(0)
                    }
                }
            }
        }
    }
    
    // 注册回调
    LaunchedEffect(scrollToTopOrRefresh) {
        onRegisterScrollToTopOrRefreshCallback?.invoke(scrollToTopOrRefresh)
    }
    
    // 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = supportedModes.indexOf(state.currentMode).coerceAtLeast(0),
        pageCount = { supportedModes.size }
    )
    
    // 同步 Pager 页面切换到 ViewModel
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newMode = supportedModes.getOrNull(pagerState.currentPage)
            if (newMode != null && newMode != state.currentMode) {
                onModeChange(newMode)
            }
        }
    }
    
    // 当外部通过点击切换 mode 时，同步到 Pager
    LaunchedEffect(state.currentMode) {
        val targetPage = supportedModes.indexOf(state.currentMode)
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 第一层选择器：内容类型 + 日期选择（固定不滑动）
        ContentTypeSelector(
            state = state,
            currentContentType = state.currentContentType,
            selectedDate = state.selectedDate,
            onContentTypeChange = onContentTypeChange,
            onDateChange = onDateChange,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 第二层选择器：排行榜模式（固定不滑动，但会响应 Pager 的页面变化）
        RankingModeSelector(
            supportedModes = supportedModes,
            currentModeIndex = pagerState.currentPage,
            onModeChange = { mode ->
                onModeChange(mode)
            },
            onRefreshOrScrollToTop = scrollToTopOrRefresh,
            modifier = Modifier.fillMaxWidth()
        )
        
        // HorizontalPager：支持左右滑动切换 mode
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { supportedModes[it].value }
        ) { page ->
            val mode = supportedModes[page]
            val modeData = state.modeDataCache[mode.value] ?: ModeData()
            
            // 为当前 mode 获取或创建列表状态
            val listState = if (state.currentContentType == RankingContent.NOVEL) {
                val lazyListState = rememberLazyListState()
                remember(mode.value, state.currentContentType) {
                    listStates.getOrPut(mode.value) { lazyListState }
                }
            } else {
                val lazyStaggeredGridState = rememberLazyStaggeredGridState()
                remember(mode.value, state.currentContentType) {
                    listStates.getOrPut(mode.value) { lazyStaggeredGridState }
                }
            }
            

            
            // 监听 scrollIndices 变化，滚动到指定位置
            // 使用 derivedStateOf 建立响应式依赖
            val targetScrollIndex by remember(mode.value) {
                derivedStateOf { scrollIndices[mode.value] }
            }
            
            LaunchedEffect(targetScrollIndex) {
                val scrollIndex = targetScrollIndex
                if (scrollIndex != null && scrollIndex > 0) {
                    println("[RankingScreen] 开始滚动到索引: $scrollIndex")
                    
                    // 平滑滚动到目标位置
                    when (listState) {
                        is androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState -> {
                            listState.animateScrollToItem(scrollIndex)
                        }
                        is androidx.compose.foundation.lazy.LazyListState -> {
                            listState.animateScrollToItem(scrollIndex)
                        }
                    }

                    
                    // 清除标记，避免重复滚动
                    scrollIndices.remove(mode.value)
                }
            }
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && modeData.artworks.isEmpty() && modeData.novels.isEmpty() -> {
                        // 初次加载
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null && modeData.artworks.isEmpty() && modeData.novels.isEmpty() -> {
                        // 错误状态
                        ErrorDisplay(
                            message = state.error,
                            onRetry = onRefresh,
                            modifier = Modifier.align(Alignment.Center),
                            isFullScreen = true
                        )
                    }
                    state.currentContentType == RankingContent.NOVEL && modeData.novels.isNotEmpty() -> {
                        // 小说列表布局
                        NovelListLayout(
                            novels = modeData.novels,
                            onNovelClick = onNovelClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = modeData.isLoadingMore,
                            listState = listState as androidx.compose.foundation.lazy.LazyListState,
                            isRefreshing = state.isLoading && modeData.novels.isNotEmpty(),
                            onRefresh = onRefresh
                        )
                    }
                    state.currentContentType != RankingContent.NOVEL && modeData.artworks.isNotEmpty() -> {
                        // 作品瀑布流布局
                        ArtworkStaggeredGridLayout(
                            artworks = modeData.artworks,
                            onArtworkClick = onArtworkClick,
                            onUserClick = onUserClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = modeData.isLoadingMore,
                            listState = listState as androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
                            isRefreshing = state.isLoading && modeData.artworks.isNotEmpty(),
                            onRefresh = onRefresh
                        )
                    }
                }
            }
        }
    }
}

/**
 * 第一层选择器：内容类型 + 日期选择
 */
@Composable
fun ContentTypeSelector(
    state: RankingState,
    currentContentType: RankingContent,
    selectedDate: String?,
    onContentTypeChange: (RankingContent) -> Unit,
    onDateChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 内容类型列表
    val contentTypes = listOf(
        RankingContent.ALL,
        RankingContent.ILLUST,
        RankingContent.UGOIRA,
        RankingContent.MANGA,
        RankingContent.NOVEL
    )
    
    NavigationBar(
        items = contentTypes,
        selectedIndex = contentTypes.indexOf(currentContentType),
        onItemClick = { index -> onContentTypeChange(contentTypes[index]) },
        itemContent = { contentType, isSelected ->
            FilterChip(
                selected = isSelected,
                onClick = { onContentTypeChange(contentType) },
                label = { Text(text = contentType.displayName) }
            )
        },
        trailingContent = {
            DateSelector(
                selectedDate = selectedDate,
                currentDate = state.currentDate,
                prevDate = state.prevDate,
                nextDate = state.nextDate,
                onDateChange = onDateChange
            )
        },
        modifier = modifier
    )
}

/**
 * 日期选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    selectedDate: String?,
    currentDate: String?,
    prevDate: String?,
    nextDate: String?,
    onDateChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    FilterChip(
        selected = selectedDate != null,
        onClick = { showDatePicker = true },
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 只在选择了具体日期时显示日期文本，否则只显示图标
                if (selectedDate != null) {
                    Text(
                        text = try {
                            val month = selectedDate.substring(4, 6)
                            val day = selectedDate.substring(6, 8)
                            "$month/$day"
                        } catch (e: Exception) {
                            ""
                        }
                    )
                }
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "选择日期",
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        modifier = modifier
    )
    
    // DatePicker 弹窗
    if (showDatePicker) {
        // 将 yyyyMMdd 转换为 UTC 毫秒时间戳
        fun dateStringToMillis(dateString: String?): Long? {
            if (dateString == null) return null
            return try {
                val year = dateString.substring(0, 4).toInt()
                val month = dateString.substring(4, 6).toInt()
                val day = dateString.substring(6, 8).toInt()
                
                // 计算自 1970-01-01 以来的天数
                var days = 0L
                for (y in 1970 until year) {
                    days += if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 366 else 365
                }
                
                val isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
                val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
                for (m in 1 until month) {
                    days += daysInMonth[m - 1]
                }
                days += (day - 1)
                
                days * 24 * 60 * 60 * 1000L
            } catch (e: Exception) {
                null
            }
        }
        
        // 将 UTC 毫秒时间戳转换为 yyyyMMdd
        fun millisToDateString(millis: Long): String {
            val days = millis / (24 * 60 * 60 * 1000)
            var remainingDays = days
            var year = 1970
            
            while (true) {
                val daysInYear = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
                if (remainingDays < daysInYear) break
                remainingDays -= daysInYear
                year++
            }
            
            val isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
            val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var month = 1
            for (m in daysInMonth) {
                if (remainingDays < m) break
                remainingDays -= m
                month++
            }
            
            val day = remainingDays + 1
            return "%04d%02d%02d".format(year, month, day.toInt())
        }
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateStringToMillis(selectedDate ?: currentDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "最新日榜" 按钮 - 重置到 null
                    TextButton(
                        onClick = {
                            onDateChange(null)
                            showDatePicker = false
                        }
                    ) {
                        Text("最新日榜")
                    }
                    // "确定" 按钮 - 使用选择的日期
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                onDateChange(millisToDateString(millis))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("确定")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "选择排行榜日期",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                headline = {
                    // 显示可用的日期范围提示
                    val hints = buildList {
                        // 只有在未选择日期(selectedDate为null)时,currentDate才代表最新日榜日期
                        // 否则currentDate只是当前显示的日期
                        if (selectedDate == null && currentDate != null) {
                            add("最新日榜: ${currentDate.substring(4, 6)}/${currentDate.substring(6, 8)}")
                        } else if (currentDate != null) {
                            add("当前: ${currentDate.substring(4, 6)}/${currentDate.substring(6, 8)}")
                        }
                        if (prevDate != null) {
                            add("前一天: ${prevDate.substring(4, 6)}/${prevDate.substring(6, 8)}")
                        }
                        if (nextDate != null) {
                            add("后一天: ${nextDate.substring(4, 6)}/${nextDate.substring(6, 8)}")
                        }
                    }
                    if (hints.isNotEmpty()) {
                        Text(
                            text = hints.joinToString(" | "),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}

/**
 * 第二层选择器：排行榜模式
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RankingModeSelector(
    supportedModes: List<RankingMode>,
    currentModeIndex: Int,
    onModeChange: (RankingMode) -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    SimpleNavigationBar(
        items = supportedModes,
        selectedIndex = currentModeIndex,
        onItemClick = { index ->
            if (index == currentModeIndex) {
                // 点击已选中的 mode，触发刷新或滚动到顶部
                onRefreshOrScrollToTop()
            } else {
                // 切换到新的 mode
                onModeChange(supportedModes[index])
            }
        },
        getItemLabel = { mode -> mode.displayName },
        modifier = modifier
    )
}

/**
 * 作品瀑布流布局
 */
@Composable
fun ArtworkStaggeredGridLayout(
    artworks: List<com.projectu.shared.domain.model.Artwork>,
    onArtworkClick: (artwork: com.projectu.shared.domain.model.Artwork, index: Int) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState = rememberLazyStaggeredGridState(),
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
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
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
}

/**
 * 小说列表布局
 */
@Composable
fun NovelListLayout(
    novels: List<com.projectu.shared.domain.model.Novel>,
    onNovelClick: (com.projectu.shared.domain.model.Novel) -> Unit,
    onUserClick: (userId: Long) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
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
            items(
                items = novels,
                key = { it.id }
            ) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
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
}
