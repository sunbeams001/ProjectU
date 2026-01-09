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
import org.koin.compose.koinInject
import com.projectu.ui.components.NavigationBar
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.components.TabbedNavigationBar
import com.projectu.ui.components.PageMapping
import com.projectu.ui.components.CustomTwoLayerMapper
import com.projectu.ui.components.rememberPagedNavigationState
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 排行榜页面的映射信息
 */
data class RankingPageMapping(
    override val primaryIndex: Int,
    override val secondaryIndex: Int,
    override val showSecondaryNav: Boolean,
    val contentType: RankingContent,
    val mode: RankingMode
) : PageMapping

/**
 * 排行榜内容区域
 * 用于在 HomeScreen 的 RankingTab 中显示
 * 
 * 使用"一层 Pager + 页码映射"机制实现双层导航
 * - 第一层：内容类型（综合/插画/动图/漫画/小说）
 * - 第二层：排行榜模式（每日/每周/每月等，根据内容类型动态变化）
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
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onRegisterScrollToTopOrRefreshCallback: ((() -> Unit) -> Unit)? = null
) {
    // 1. 准备数据：内容类型列表
    val contentTypes = remember {
        listOf(
            RankingContent.ALL,
            RankingContent.ILLUST,
            RankingContent.UGOIRA,
            RankingContent.MANGA,
            RankingContent.NOVEL
        )
    }
    
    // 2. 准备数据：每个内容类型支持的模式列表
    val modesPerType = remember {
        contentTypes.map { type ->
            RankingContentModeConfig.getSupportedModes(type)
        }
    }
    
    // 3. 创建页码映射器（使用 CustomTwoLayerMapper）
    val mapper = remember {
        CustomTwoLayerMapper(
            secondaryCountPerPrimary = modesPerType.map { it.size }, // [16, 7, 4, 7, 14]
            createMapping = { primaryIndex, secondaryIndex, showSecondary ->
                val contentType = contentTypes[primaryIndex]
                val mode = modesPerType[primaryIndex][secondaryIndex]
                RankingPageMapping(
                    primaryIndex = primaryIndex,
                    secondaryIndex = secondaryIndex,
                    showSecondaryNav = showSecondary,
                    contentType = contentType,
                    mode = mode
                )
            }
        )
    }
    
    // 4. 计算初始页码
    val initialPage = remember(state.currentContentType, state.currentMode) {
        val primaryIndex = contentTypes.indexOf(state.currentContentType).coerceAtLeast(0)
        val modes = modesPerType.getOrNull(primaryIndex) ?: emptyList()
        val secondaryIndex = modes.indexOf(state.currentMode).coerceAtLeast(0)
        mapper.calculatePageIndex(primaryIndex, secondaryIndex)
    }
    
    // 5. 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { mapper.totalPages }
    )
    
    // 6. 创建页码导航状态
    val navState = rememberPagedNavigationState(pagerState, mapper)
    val coroutineScope = rememberCoroutineScope()
    
    // 7. 为每个页面创建独立的列表状态缓存
    val listStates = remember {
        mutableStateMapOf<String, Any>()
    }
    
    // 8. 同步 Pager 页面切换到 ViewModel
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val mapping = mapper.parsePageIndex(pagerState.currentPage)
            if (mapping.contentType != state.currentContentType) {
                onContentTypeChange(mapping.contentType)
            }
            if (mapping.mode != state.currentMode) {
                onModeChange(mapping.mode)
            }
        }
    }
    
    // 9. 当外部通过 ViewModel 切换时，同步到 Pager
    LaunchedEffect(state.currentContentType, state.currentMode) {
        val primaryIndex = contentTypes.indexOf(state.currentContentType).coerceAtLeast(0)
        val modes = modesPerType.getOrNull(primaryIndex) ?: emptyList()
        val secondaryIndex = modes.indexOf(state.currentMode).coerceAtLeast(0)
        val targetPage = mapper.calculatePageIndex(primaryIndex, secondaryIndex)
        
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    // 10. 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = remember(state.currentContentType, state.currentMode, listStates) {
        {
            val currentContentType = state.currentContentType
            val currentMode = state.currentMode
            // 使用和列表状态缓存相同的 key
            val pageKey = "${currentContentType.name}_${currentMode.value}"
            val listState = listStates[pageKey]
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
    
    // 11. 注册回调
    LaunchedEffect(scrollToTopOrRefresh) {
        onRegisterScrollToTopOrRefreshCallback?.invoke(scrollToTopOrRefresh)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 12. 使用 TabbedNavigationBar（双层导航）
        val currentMapping = navState.currentMapping
        
        TabbedNavigationBar(
            primaryItems = contentTypes,
            primarySelectedIndex = currentMapping.primaryIndex,
            onPrimaryItemClick = { index ->
                if (index == currentMapping.primaryIndex) {
                    // 重复点击当前分类，刷新或滚动到顶部
                    scrollToTopOrRefresh()
                } else {
                    // 切换到新的分类，尝试保持相同的 Mode
                    val currentMode = currentMapping.mode
                    val newModes = modesPerType[index]
                    
                    // 尝试在新分类中找到相同的 Mode
                    val newSecondaryIndex = newModes.indexOf(currentMode).let {
                        if (it >= 0) it else 0  // 找到则使用，否则使用第一个
                    }
                    
                    val targetPage = mapper.calculatePageIndex(index, newSecondaryIndex)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            },
            getPrimaryItemLabel = { it.getLocalizedDisplayName() },
            secondaryItems = modesPerType[currentMapping.primaryIndex],
            secondarySelectedIndex = currentMapping.secondaryIndex,
            onSecondaryItemClick = { index ->
                navState.handleSecondaryClick(
                    secondaryIndex = index,
                    currentPrimaryIndex = currentMapping.primaryIndex,
                    scope = coroutineScope,
                    onSamePage = scrollToTopOrRefresh
                )
            },
            getSecondaryItemLabel = { it.getLocalizedDisplayName() },
            showSecondaryNav = true,  // 排行榜的所有内容类型都有第二层导航
            primaryTrailingContent = {
                // 日期选择器放在第一层导航末尾
                DateSelectorChip(
                    selectedDate = state.selectedDate,
                    currentDate = state.currentDate,
                    prevDate = state.prevDate,
                    nextDate = state.nextDate,
                    onDateChange = onDateChange
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // 14. 单一 HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page ->
                val mapping = mapper.parsePageIndex(page)
                "${mapping.contentType.name}_${mapping.mode.value}"
            }
        ) { page ->
            val mapping = mapper.parsePageIndex(page)
            val modeData = state.modeDataCache[mapping.mode.value] ?: ModeData()
            
            // 为当前页面创建唯一的 key（包含内容类型和模式，避免不同类型共享状态）
            val pageKey = "${mapping.contentType.name}_${mapping.mode.value}"
            
            // 为当前页面获取或创建列表状态
            val listState = if (mapping.contentType == RankingContent.NOVEL) {
                val lazyListState = rememberLazyListState()
                remember(pageKey) {
                    listStates.getOrPut(pageKey) { lazyListState }
                }
            } else {
                val lazyStaggeredGridState = rememberLazyStaggeredGridState()
                remember(pageKey) {
                    listStates.getOrPut(pageKey) { lazyStaggeredGridState }
                }
            }
            
            // 监听 scrollIndices 变化，滚动到指定位置（仍使用 mode.value 作为 key）
            val targetScrollIndex by remember(mapping.mode.value) {
                derivedStateOf { scrollIndices[mapping.mode.value] }
            }
            
            LaunchedEffect(targetScrollIndex) {
                val scrollIndex = targetScrollIndex
                if (scrollIndex != null && scrollIndex > 0) {
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
                    scrollIndices.remove(mapping.mode.value)
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
                    mapping.contentType == RankingContent.NOVEL && modeData.novels.isNotEmpty() -> {
                        // 小说列表布局
                        NovelListLayout(
                            novels = modeData.novels,
                            onNovelClick = onNovelClick,
                            onSeriesClick = onSeriesClick,
                            onUserClick = onUserClick,
                            onTagClick = onTagClick,
                            onLoadMore = onLoadMore,
                            isLoadingMore = modeData.isLoadingMore,
                            listState = listState as androidx.compose.foundation.lazy.LazyListState,
                            isRefreshing = state.isLoading && modeData.novels.isNotEmpty(),
                            onRefresh = onRefresh
                        )
                    }
                    mapping.contentType != RankingContent.NOVEL && modeData.artworks.isNotEmpty() -> {
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
 * 日期选择器 Chip（用于放在导航栏末尾）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorChip(
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
                    contentDescription = stringResource(Res.string.ranking_select_date),
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
                    // "最新榜单" 按钮 - 重置到 null（使用API默认的最新日期）
                    TextButton(
                        onClick = {
                            onDateChange(null)
                            showDatePicker = false
                        }
                    ) {
                        Text(stringResource(Res.string.ranking_latest))
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
                        Text(stringResource(Res.string.common_confirm))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = stringResource(Res.string.ranking_select_ranking_date),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                },
                headline = {
                    // 显示可用的日期范围提示
                    val latestText = stringResource(Res.string.ranking_latest)
                    val currentText = stringResource(Res.string.ranking_current, "")
                    val prevText = stringResource(Res.string.ranking_previous_day, "")
                    val nextText = stringResource(Res.string.ranking_next_day, "")
                    
                    val hints = buildList {
                        // 只有在未选择日期(selectedDate为null)时,currentDate才代表最新榜单日期
                        // 否则currentDate只是当前显示的日期
                        if (selectedDate == null && currentDate != null) {
                            add("$latestText: ${currentDate.substring(4, 6)}/${currentDate.substring(6, 8)}")
                        } else if (currentDate != null) {
                            add(stringResource(Res.string.ranking_current, "${currentDate.substring(4, 6)}/${currentDate.substring(6, 8)}"))
                        }
                        if (prevDate != null) {
                            add(stringResource(Res.string.ranking_previous_day, "${prevDate.substring(4, 6)}/${prevDate.substring(6, 8)}"))
                        }
                        if (nextDate != null) {
                            add(stringResource(Res.string.ranking_next_day, "${nextDate.substring(4, 6)}/${nextDate.substring(6, 8)}"))
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
 * 作品瀑布流布局
 */
@Composable
fun ArtworkStaggeredGridLayout(
    artworks: List<com.projectu.shared.domain.model.Artwork>,
    onArtworkClick: (artwork: com.projectu.shared.domain.model.Artwork, index: Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    
    val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
    val columns by settingsCache.staggeredGridColumns.collectAsState()
    
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
            columns = StaggeredGridCells.Fixed(columns),
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
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
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
 * 获取 RankingMode 的本地化显示名称
 */
@Composable
fun RankingMode.getLocalizedDisplayName(): String {
    return when (this) {
        RankingMode.DAILY -> stringResource(Res.string.ranking_daily)
        RankingMode.WEEKLY -> stringResource(Res.string.ranking_weekly)
        RankingMode.MONTHLY -> stringResource(Res.string.ranking_monthly)
        RankingMode.ROOKIE -> stringResource(Res.string.ranking_rookie)
        RankingMode.ORIGINAL -> stringResource(Res.string.ranking_original)
        RankingMode.MALE -> stringResource(Res.string.ranking_male)
        RankingMode.FEMALE -> stringResource(Res.string.ranking_female)
        RankingMode.DAILY_AI -> stringResource(Res.string.ranking_ai)
        RankingMode.WEEKLY_ORIGINAL -> stringResource(Res.string.ranking_weekly_original)
        RankingMode.WEEKLY_AI -> stringResource(Res.string.ranking_weekly_ai)
        RankingMode.WEEKLY_R18_AI -> stringResource(Res.string.ranking_weekly_r18_ai)
        RankingMode.DAILY_R18 -> stringResource(Res.string.ranking_daily_r18)
        RankingMode.WEEKLY_R18 -> stringResource(Res.string.ranking_weekly_r18)
        RankingMode.MALE_R18 -> stringResource(Res.string.ranking_male_r18)
        RankingMode.FEMALE_R18 -> stringResource(Res.string.ranking_female_r18)
        RankingMode.DAILY_R18_AI -> stringResource(Res.string.ranking_ai_r18)
        RankingMode.R18G -> stringResource(Res.string.ranking_r18g)
    }
}

/**
 * 获取 RankingContent 的本地化显示名称
 */
@Composable
fun RankingContent.getLocalizedDisplayName(): String {
    return when (this) {
        RankingContent.ALL -> stringResource(Res.string.ranking_content_all)
        RankingContent.ILLUST -> stringResource(Res.string.ranking_content_illust)
        RankingContent.MANGA -> stringResource(Res.string.ranking_content_manga)
        RankingContent.UGOIRA -> stringResource(Res.string.ranking_content_ugoira)
        RankingContent.NOVEL -> stringResource(Res.string.ranking_content_novel)
    }
}
