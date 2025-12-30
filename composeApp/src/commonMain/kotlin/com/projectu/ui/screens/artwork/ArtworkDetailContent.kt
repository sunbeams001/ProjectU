package com.projectu.ui.screens.artwork

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.getUrlByQuality
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.RetryableAsyncImage
import com.projectu.ui.components.UgoiraDisplay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 作品详情页主内容
 * 
 * 支持两种模式：
 * 1. 单个作品模式：当 state.artworkIds 为空时
 * 2. 列表导航模式：当 state.artworkIds 不为空时，使用 HorizontalPager 支持左右滑动
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtworkDetailContent(
    state: ArtworkDetailState,
    onBackClick: () -> Unit,
    onPageChange: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onExpandInfo: () -> Unit = {},
    onCollapseInfo: () -> Unit = {},
    onUserClick: ((userId: String) -> Unit)? = null,
    onSeriesClick: ((seriesId: String) -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onSimilarClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDownloadLongClick: (() -> Unit)? = null,
    onImageClick: ((pageIndex: Int) -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onBlockTag: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.artwork == null -> {
                // 初次加载
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.artwork == null -> {
                // 错误状态 - 使用统一的ErrorDisplay组件
                ErrorDisplay(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                    isFullScreen = true
                )
            }

            state.artwork != null -> {
                // 判断是列表导航模式还是单个作品模式
                if (state.artworkIds.isNotEmpty()) {
                    // 列表导航模式：使用 HorizontalPager
                    ArtworkListPager(
                        state = state,
                        onPageChange = onPageChange,
                        onExpandInfo = onExpandInfo,
                        onCollapseInfo = onCollapseInfo,
                        onUserClick = onUserClick,
                        onSeriesClick = onSeriesClick,
                        onCommentClick = onCommentClick,
                        onSimilarClick = onSimilarClick,
                        onDownloadClick = onDownloadClick,
                        onDownloadLongClick = onDownloadLongClick,
                        onImageClick = onImageClick,
                        onTagClick = onTagClick,
                        onBlockTag = onBlockTag,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 单个作品模式：直接展示
                    ArtworkDetailLayout(
                        artwork = state.artwork,
                        authorFollowStatus = state.authorFollowStatus,
                        isInfoExpanded = state.isInfoExpanded,
                        onExpandInfo = onExpandInfo,
                        onCollapseInfo = onCollapseInfo,
                        onUserClick = onUserClick,
                        onSeriesClick = onSeriesClick,
                        onCommentClick = onCommentClick,
                        onSimilarClick = onSimilarClick,
                        onDownloadClick = onDownloadClick,
                        onDownloadLongClick = onDownloadLongClick,
                        onImageClick = onImageClick,
                        onTagClick = onTagClick,
                        onBlockTag = onBlockTag,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    )
                }
            }
        }
        
        // 浮动的返回按钮
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.nav_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Snackbar显示
        snackbarHostState?.let {
            SnackbarHost(
                hostState = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}

/**
 * 作品列表分页器
 * 使用 HorizontalPager 实现左右滑动浏览
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtworkListPager(
    state: ArtworkDetailState,
    onPageChange: (Int) -> Unit,
    onExpandInfo: () -> Unit = {},
    onCollapseInfo: () -> Unit = {},
    onUserClick: ((userId: String) -> Unit)? = null,
    onSeriesClick: ((seriesId: String) -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onSimilarClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDownloadLongClick: (() -> Unit)? = null,
    onImageClick: ((pageIndex: Int) -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onBlockTag: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.artworkIds.size }
    )
    
    val coroutineScope = rememberCoroutineScope()
    
    // 监听页面切换
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != state.currentIndex) {
            onPageChange(pagerState.currentPage)
        }
    }
    
    // 当状态中的索引改变时，同步 Pager（例如通过其他方式切换了作品）
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex != pagerState.currentPage) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(state.currentIndex)
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        key = { index -> state.artworkIds.getOrNull(index) ?: index }
    ) { pageIndex ->
        // 获取该页面对应的作品ID
        val artworkId = state.artworkIds.getOrNull(pageIndex) ?: return@HorizontalPager
        
        // 尝试从缓存中获取作品
        val cachedArtwork = state.artworkCache[artworkId]
        
        when {
            cachedArtwork != null -> {
                // 有缓存，直接显示
                val followStatus = if (pageIndex == state.currentIndex) {
                    state.authorFollowStatus
                } else {
                    FollowStatus.NOT_FOLLOWING
                }
                
                ArtworkDetailLayout(
                    artwork = cachedArtwork,
                    authorFollowStatus = followStatus,
                    isInfoExpanded = state.isInfoExpanded,
                    onExpandInfo = onExpandInfo,
                    onCollapseInfo = onCollapseInfo,
                    onUserClick = onUserClick,
                    onSeriesClick = onSeriesClick,
                    onCommentClick = onCommentClick,
                    onSimilarClick = onSimilarClick,
                    onDownloadClick = onDownloadClick,
                    onDownloadLongClick = onDownloadLongClick,
                    onImageClick = onImageClick,
                    onTagClick = onTagClick,
                    onBlockTag = onBlockTag,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
            
            pageIndex == state.currentIndex && state.artwork != null && state.artwork.id == artworkId -> {
                // 当前页面且作品ID匹配（确保显示的是正确的作品）
                ArtworkDetailLayout(
                    artwork = state.artwork,
                    authorFollowStatus = state.authorFollowStatus,
                    isInfoExpanded = state.isInfoExpanded,
                    onExpandInfo = onExpandInfo,
                    onCollapseInfo = onCollapseInfo,
                    onUserClick = onUserClick,
                    onSeriesClick = onSeriesClick,
                    onCommentClick = onCommentClick,
                    onSimilarClick = onSimilarClick,
                    onDownloadClick = onDownloadClick,
                    onDownloadLongClick = onDownloadLongClick,
                    onImageClick = onImageClick,
                    onTagClick = onTagClick,
                    onBlockTag = onBlockTag,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
            
            else -> {
                // 未加载或数据不匹配，显示加载指示器
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(Res.string.common_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 作品详情布局
 * 分层结构：
 * 底层：详情信息区域（固定在屏幕底部，高度为屏幕一半）
 * 上层：作品展示区域 + 基础信息区域（上下排列，可通过手势上移）
 * 注意：兼容系统导航栏高度
 */
@Composable
private fun ArtworkDetailLayout(
    artwork: Artwork,
    authorFollowStatus: com.projectu.shared.domain.model.FollowStatus,
    isInfoExpanded: Boolean = false,
    onExpandInfo: () -> Unit = {},
    onCollapseInfo: () -> Unit = {},
    onUserClick: ((userId: String) -> Unit)? = null,
    onSeriesClick: ((seriesId: String) -> Unit)? = null,
    onCommentClick: (() -> Unit)? = null,
    onSimilarClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDownloadLongClick: (() -> Unit)? = null,
    onImageClick: ((pageIndex: Int) -> Unit)? = null,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onBlockTag: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.navigationBarsPadding()
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        
        // 记录基础信息区域的高度
        var basicInfoHeight by remember { mutableStateOf(0.dp) }
        
        // 上移偏移量（使用 rememberSaveable 保存状态，页面返回时不会丢失）
        // 但当作品ID变化时需要重置（通过 key 参数）
        var dragOffset by rememberSaveable(artwork.id) { mutableFloatStateOf(0f) }
        val maxDragOffset = remember(screenHeight) {
            with(density) { (screenHeight / 2).toPx() }
        }
        
        // 动画状态（使用作品ID作为key，切换作品时重置）
        val animatedOffset = remember(artwork.id) { Animatable(dragOffset) }
        
        // 标志位：防止收起动画在滚动时重复触发
        var isCollapsing by remember(artwork.id) { mutableStateOf(false) }
        
        // 吸附阈值：当偏移量小于此值时，自动吸附到原位
        val snapThreshold = maxDragOffset * 0.2f
        
        // 同步动画值到 dragOffset
        LaunchedEffect(animatedOffset.value) {
            dragOffset = animatedOffset.value
        }
        
        // 底层：详情信息区域（固定在底部，高度为屏幕一半）
        ArtworkDetailInfoSection(
            artwork = artwork,
            onCommentClick = onCommentClick,
            onSimilarClick = onSimilarClick,
            onDownloadClick = onDownloadClick,
            onDownloadLongClick = onDownloadLongClick,
            onTagClick = onTagClick,
            onBlockTag = onBlockTag,
            onScrollAtTop = { delta ->
                // 当详情区域滚动到顶部时，继续下滑会触发基础信息区域收起
                if (delta > 0 && dragOffset > 0 && !isCollapsing) {
                    // 下滑且基础信息区域已展开，且未在收起中，触发收起
                    isCollapsing = true
                    coroutineScope.launch {
                        animatedOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 300)
                        )
                        isCollapsing = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight / 2)
                .align(Alignment.BottomCenter)
        )

        // 上层：作品展示区域 + 基础信息区域（上下排列，可上移）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -dragOffset.toInt()) }
        ) {
            // 作品展示区域（占据剩余空间）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                ArtworkDisplayArea(
                    artwork = artwork,
                    onImageClick = onImageClick
                )
            }

            // 基础信息区域（固定在底部，支持手势拖动）
            ArtworkBasicInfoSection(
                artwork = artwork,
                authorFollowStatus = authorFollowStatus,
                onUserClick = onUserClick,
                onSeriesClick = onSeriesClick,
                onDragDelta = { delta ->
                    // 检测拖动方向，不再实时跟手
                    // 只在手势结束时决定展开或收起
                },
                onDragEnd = {
                    // 手势结束时，判断拖动方向并触发动画
                    coroutineScope.launch {
                        // 如果当前接近原位（未展开），则展开到最大位置
                        if (dragOffset < maxDragOffset / 2) {
                            animatedOffset.animateTo(
                                targetValue = maxDragOffset,
                                animationSpec = tween(durationMillis = 250)
                            )
                        } else {
                            // 如果已经展开较多，则收起到原位
                            animatedOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 250)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        basicInfoHeight = with(density) { size.height.toDp() }
                    }
            )
        }
    }
}

/**
 * 作品展示区域
 * 根据作品类型和页数采用不同的展示方式
 */
@Composable
private fun ArtworkDisplayArea(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    settingsCache: SettingsCache = koinInject(),
    onImageClick: ((pageIndex: Int) -> Unit)? = null
) {
    val imageQuality = settingsCache.getDetailImageQuality()
    val pages = artwork.imageUrls.pages

    when {
        // 动图类型 - 使用 UgoiraDisplay 组件
        artwork.type == ArtworkType.UGOIRA -> {
            UgoiraDisplay(
                artworkId = artwork.id,
                modifier = modifier
            )
        }

        // 插画或漫画作品（单页或多页统一处理）
        else -> {
            ArtworkPagesDisplay(
                pages = pages,
                imageQuality = imageQuality,
                contentDescription = artwork.title,
                onImageClick = onImageClick,
                modifier = modifier
            )
        }
    }
}

/**
 * 作品图片展示（统一处理单页和多页）
 * 每一页宽度填充，高度根据实际情况自适应
 * 如果总高度小于等于区域高度，垂直居中展示
 * 如果总高度超过区域高度，第一张置顶，可滚动浏览
 */
@Composable
private fun ArtworkPagesDisplay(
    pages: List<com.projectu.shared.domain.model.PageImageUrls>,
    imageQuality: com.projectu.shared.domain.model.DetailImageQuality,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onImageClick: ((pageIndex: Int) -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidthDp = maxWidth
        val containerHeightDp = maxHeight

        // 计算每张图片按宽度填充后的高度
        val imageHeights = pages.map { page ->
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            containerWidthDp * aspectRatio
        }

        val totalHeight = imageHeights.fold(0.dp) { acc, height -> acc + height }

        // 判断是否需要滚动
        val needsScroll = totalHeight > containerHeightDp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (needsScroll) Arrangement.Top else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(pages.size) { index ->
                val page = pages[index]
                RetryableAsyncImage(
                    model = page.getUrlByQuality(imageQuality),
                    contentDescription = if (pages.size > 1) {
                        "$contentDescription - ${stringResource(Res.string.novel_page_number, page.page + 1)}"
                    } else {
                        contentDescription
                    },
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(page.width.toFloat() / page.height.toFloat())
                        .let { mod ->
                            if (onImageClick != null) {
                                mod.pointerInput(index) {
                                    detectTapGestures {
                                        onImageClick(index)
                                    }
                                }
                            } else {
                                mod
                            }
                        },
                    showErrorDetails = true
                )
            }
        }
    }
}
