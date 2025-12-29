package com.projectu.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.cache.NovelCacheManager
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.comment.CommentsScreen
import com.projectu.ui.screens.download.DownloadScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.util.PlatformBackHandler
import com.projectu.shared.domain.model.CommentContentType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import com.projectu.ui.util.rememberTagClickHandler

/**
 * 小说详情页面
 * 
 * 布局设计：
 * - 上方：小说阅读区域（占据大部分空间）
 * - 下方：小说信息区域（可收缩/展开）
 * 
 * 支持两种导航模式：
 * 1. 单个小说模式：独立查看某部小说
 * 2. 列表导航模式：在小说列表上下文中查看，支持左右滑动浏览
 * 
 * @param novelId 单个小说ID（单独使用时）
 * @param novelIds 小说ID列表（列表导航模式）
 * @param initialIndex 初始小说在列表中的索引
 * @param contextKey 导航上下文的key
 */
data class NovelDetailScreen(
    val novelId: String = "",
    val novelIds: List<String> = emptyList(),
    val initialIndex: Int = 0,
    val contextKey: String = ""
) : Screen {
    
    // 为每个实例生成唯一的 key，确保 Voyager 将它们视为不同的 Screen
    // 这样在导航栈中就会有真正的页面切换动画和独立的 ViewModel
    override val key: ScreenKey = uniqueScreenKey
    
    @Composable
    override fun Content() {
        // 使用 rememberScreenModel 创建独立的 ViewModel 实例
        // 每个 Screen 实例会有自己独立的 ViewModel，由 Voyager 管理生命周期
        val novelRepository: NovelRepository = koinInject()
        val userRepository: UserRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val syncNovelStatesUseCase: SyncNovelStatesUseCase = koinInject()
        val stateCacheManager: StateCacheManager = koinInject()
        val novelCacheManager: NovelCacheManager = koinInject()
        
        val viewModel = rememberScreenModel {
            NovelDetailViewModel(
                novelRepository = novelRepository,
                userRepository = userRepository,
                authRepository = authRepository,
                syncNovelStatesUseCase = syncNovelStatesUseCase,
                stateCacheManager = stateCacheManager,
                novelCacheManager = novelCacheManager
            )
        }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val downloadRepository: DownloadRepository = koinInject()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val downloadTaskAddedMessage = stringResource(Res.string.download_task_added)
        val downloadActionViewLabel = stringResource(Res.string.download_action_view)
        
        // 从 NavigationContextManager 获取上下文
        val context = remember(contextKey) {
            if (contextKey.isNotEmpty()) {
                NavigationContextManager.getNovelContext(contextKey)
            } else null
        }
        
        // 订阅列表源的 StateFlow（如果有的话）
        val liveNovelIds by context?.listSource?.novelIdsFlow?.collectAsState()
            ?: remember { mutableStateOf(emptyList()) }
        
        // 当前实际使用的小说ID列表
        val currentNovelIds = if (liveNovelIds.isNotEmpty()) {
            liveNovelIds
        } else {
            novelIds
        }
        
        // 初始化加载小说详情
        LaunchedEffect(novelId, initialIndex) {
            if (currentNovelIds.isNotEmpty()) {
                val onLoadMore: (() -> Unit)? = context?.listSource?.let { source ->
                    { source.loadMoreNovels() }
                }
                viewModel.initWithNovelList(currentNovelIds, initialIndex, onLoadMore)
            } else if (novelId.isNotEmpty()) {
                viewModel.loadNovelDetail(novelId)
            }
        }
        
        // 监听 novelIds 变化
        LaunchedEffect(currentNovelIds.size) {
            if (currentNovelIds.isNotEmpty()) {
                viewModel.updateNovelList(currentNovelIds)
            }
        }
        
        // 退出页面的处理逻辑（左上角返回按钮使用）
        val handleExit: () -> Unit = {
            val currentIndex = viewModel.getCurrentIndex()
            context?.onReturnWithIndex?.invoke(currentIndex)
            if (contextKey.isNotEmpty()) {
                NavigationContextManager.removeNovelContext(contextKey)
            }
            navigator.pop()
        }
        
        // 系统返回键的处理逻辑
        val handleSystemBack: () -> Unit = {
            if (state.isInfoExpanded) {
                // 展开状态下，先收起信息区域
                viewModel.toggleInfoExpanded()
            } else {
                // 收缩状态下，直接退出页面
                handleExit()
            }
        }
        
        // 拦截系统返回键
        PlatformBackHandler(enabled = true, onBack = handleSystemBack)
        
        // 创建Tag点击处理器
        val tagClickHandler = rememberTagClickHandler(navigator)
        
        // 当小说有 marker 时，自动跳转到对应页面（仅首次）
        var hasJumpedToMarker by remember { mutableStateOf(false) }
        LaunchedEffect(state.novel?.id, state.novel?.marker) {
            val marker = state.novel?.marker
            if (marker != null && marker > 0 && !hasJumpedToMarker) {
                viewModel.goToPage(marker)
                hasJumpedToMarker = true
            }
        }
        
        NovelDetailContent(
            state = state,
            viewModel = viewModel,
            onBackClick = handleExit,  // 左上角返回按钮直接退出
            onListIndexChange = { index -> viewModel.onListIndexChanged(index) },
            onPreviousPage = { viewModel.previousPage() },
            onNextPage = { viewModel.nextPage() },
            onToggleInfo = { viewModel.toggleInfoExpanded() },
            onCollapseInfo = { if (state.isInfoExpanded) viewModel.toggleInfoExpanded() },
            onMarkerClick = { viewModel.toggleMarker() },
            onRetry = { viewModel.retry() },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onSeriesClick = { seriesId ->
                navigator.push(NovelSeriesScreen(seriesId))
            },
            onCommentClick = {
                state.novel?.let { novel ->
                    navigator.push(
                        CommentsScreen(
                            contentId = novel.id,
                            contentType = CommentContentType.NOVEL,
                            contentTitle = novel.title
                        )
                    )
                }
            },
            onDownloadClick = {
                state.novel?.let { novel ->
                    coroutineScope.launch {
                        val result = downloadRepository.addNovelDownload(novelId = novel.id)
                        if (result.isSuccess) {
                            val snackbarResult = snackbarHostState.showSnackbar(
                                message = downloadTaskAddedMessage,
                                actionLabel = downloadActionViewLabel,
                                duration = SnackbarDuration.Short
                            )
                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                navigator.push(DownloadScreen())
                            }
                        }
                    }
                }
            },
            onTagClick = tagClickHandler::handleTagClick,
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * 小说详情页主内容
 */
@Composable
private fun NovelDetailContent(
    state: NovelDetailState,
    viewModel: NovelDetailViewModel,
    onBackClick: () -> Unit,
    onListIndexChange: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleInfo: () -> Unit,
    onCollapseInfo: () -> Unit,
    onMarkerClick: () -> Unit,
    onRetry: () -> Unit,
    onUserClick: ((userId: String) -> Unit)?,
    onSeriesClick: ((seriesId: String) -> Unit)?,
    onCommentClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.novel == null -> {
                // 初次加载
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            state.error != null && state.novel == null -> {
                // 错误状态
                ErrorDisplay(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                    isFullScreen = true
                )
            }
            
            state.novel != null -> {
                // 小说详情布局
                NovelDetailLayout(
                    state = state,
                    viewModel = viewModel,
                    onPreviousPage = onPreviousPage,
                    onNextPage = onNextPage,
                    onToggleInfo = onToggleInfo,
                    onCollapseInfo = onCollapseInfo,
                    onMarkerClick = onMarkerClick,
                    onUserClick = onUserClick,
                    onSeriesClick = onSeriesClick,
                    onCommentClick = onCommentClick,
                    onDownloadClick = onDownloadClick,
                    onTagClick = onTagClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
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
 * 小说详情布局
 * 上方：阅读区域
 * 下方：信息区域（可收缩）
 */
@Composable
private fun NovelDetailLayout(
    state: NovelDetailState,
    viewModel: NovelDetailViewModel,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleInfo: () -> Unit,
    onCollapseInfo: () -> Unit,
    onMarkerClick: () -> Unit,
    onUserClick: ((userId: String) -> Unit)?,
    onSeriesClick: ((seriesId: String) -> Unit)?,
    onCommentClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val novel = state.novel ?: return
    
    // 用于检测下拉收起的 NestedScrollConnection
    // 当信息区域展开且内容滚动到顶部时，继续下拉会收起信息区域
    var accumulatedOverscroll by remember { mutableFloatStateOf(0f) }
    val collapseThreshold = 150f // 下拉阈值（像素）
    
    val nestedScrollConnection = remember(state.isInfoExpanded) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 仅在信息区域展开时处理
                if (!state.isInfoExpanded) {
                    accumulatedOverscroll = 0f
                    return Offset.Zero
                }
                
                // 如果是向上滚动（available.y < 0），重置累积值
                if (available.y < 0) {
                    accumulatedOverscroll = 0f
                }
                
                return Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 仅在信息区域展开时处理
                if (!state.isInfoExpanded) {
                    return Offset.Zero
                }
                
                // available.y > 0 表示有未被消费的向下滚动（即内容已在顶部，继续下拉）
                if (available.y > 0) {
                    accumulatedOverscroll += available.y
                    
                    if (accumulatedOverscroll > collapseThreshold) {
                        accumulatedOverscroll = 0f
                        onCollapseInfo()
                        return available // 消费掉这个滚动
                    }
                } else {
                    // 向上滚动时重置累积值
                    accumulatedOverscroll = 0f
                }
                
                return Offset.Zero
            }
        }
    }
    
    Column(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        // 阅读区域（占据剩余空间）
        NovelReadingArea(
            pages = state.parsedPages,
            currentPage = state.currentPage,
            embeddedImages = novel.embeddedImages,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onToggleInfo = onToggleInfo,
            savedScrollPosition = state.pageScrollPositions[state.currentPage],
            onScrollPositionChanged = { index, offset ->
                viewModel.saveScrollPosition(state.currentPage, index, offset)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        // 信息区域（可收缩/展开）
        NovelInfoSection(
            novel = novel,
            authorFollowStatus = state.authorFollowStatus,
            isExpanded = state.isInfoExpanded,
            markerStatus = state.markerStatus,
            isMarkerLoading = state.isMarkerLoading,
            onToggle = onToggleInfo,
            onCollapse = onCollapseInfo,
            onMarkerClick = onMarkerClick,
            onUserClick = onUserClick,
            onSeriesClick = onSeriesClick,
            onCommentClick = onCommentClick,
            onDownloadClick = onDownloadClick,
            onTagClick = onTagClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
