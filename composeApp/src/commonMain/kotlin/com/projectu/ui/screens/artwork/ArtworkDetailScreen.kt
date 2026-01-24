package com.projectu.ui.screens.artwork

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.local.UgoiraFormat
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.ArtworkShareType
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.comment.CommentsScreen
import com.projectu.ui.screens.download.DownloadScreen
import com.projectu.ui.screens.blocklist.BlockListScreen
import com.projectu.ui.screens.share.ShareViewModel
import com.projectu.ui.screens.share.ShareIntent
import com.projectu.ui.util.ShareTextFormatter
import com.projectu.ui.components.share.ArtworkShareBottomSheet
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.ui.util.PlatformBackHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.download_action_view
import projectu.composeapp.generated.resources.download_task_added

/**
 * 作品详情页面
 * 
 * 布局设计：
 * - 上方：作品展示区域（占据剩余全部高度）
 *   - 单页作品：居中缩放完全展示
 *   - 多页作品：宽度填充，可垂直滚动
 *   - 动图：居中展示（后期完善）
 * - 下方：可收缩/展开的作品信息区域
 * 
 * 支持列表上下文导航：
 * - 当提供 artworkIds 和 initialIndex 时，支持左右滑动浏览列表中的其他作品
 * - 单独使用 artworkId 时，仅展示单个作品详情
 * 
 * 注意：所有参数必须是可序列化的，以支持 Activity 状态恢复
 * 不可序列化的回调通过 NavigationContextManager 传递
 * 
 * @param artworkId 单个作品ID（单独使用时）
 * @param artworkIds 作品ID列表（列表导航模式，作为 Activity 恢复时的快照）
 * @param initialIndex 初始作品在列表中的索引（配合 artworkIds 使用）
 * @param contextKey 导航上下文的 key，用于获取回调等不可序列化的数据
 */
data class ArtworkDetailScreen(
    val artworkId: String = "",
    val artworkIds: List<String> = emptyList(),
    val initialIndex: Int = 0,
    val contextKey: String = ""
) : Screen {
    
    /**
     * 自定义 Screen key，确保不同的作品详情页有不同的 ViewModel 实例
     * 
     * 使用 artworkId 和 contextKey 组合：
     * - 单个作品模式：基于 artworkId 唯一
     * - 列表导航模式：基于 contextKey 唯一（来自同一列表的详情页共享 ViewModel）
     */
    override val key: ScreenKey
        get() = if (contextKey.isNotEmpty()) {
            "ArtworkDetailScreen_$contextKey"
        } else {
            "ArtworkDetailScreen_$artworkId"
        }
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ArtworkDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val downloadRepository: DownloadRepository = koinInject()
        val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
        val shareViewModel: ShareViewModel = koinInject()
        val shareState by shareViewModel.state.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val downloadTaskAddedMessage = stringResource(Res.string.download_task_added)
        val downloadActionViewLabel = stringResource(Res.string.download_action_view)
        
        // 分享选项底部面板状态
        var showShareBottomSheet by remember { mutableStateOf(false) }
        
        // 预计算分享文本（在@Composable上下文中）
        val formattedShareText = state.artwork?.let { artwork ->
            ShareTextFormatter.formatArtworkText(artwork)
        } ?: ""
        
        // 检查翻译功能是否启用
        val isTranslationEnabled by remember { derivedStateOf { settingsCache.isTranslationEnabled() } }
        
        // 保存浏览历史
        com.projectu.ui.util.SaveArtworkHistory(state.artwork)
        
        // 创建Tag点击处理器
        val tagClickHandler = com.projectu.ui.util.rememberTagClickHandler(navigator)
        
        // 从 NavigationContextManager 获取上下文
        val context = remember(contextKey) {
            if (contextKey.isNotEmpty()) {
                NavigationContextManager.getContext(contextKey)
            } else null
        }
        
        // 订阅列表源的 StateFlow（如果有的话）
        // 当 listSource 存在时，使用其响应式的 artworkIdsFlow
        // 否则使用序列化的 artworkIds 作为快照（Activity 恢复场景）
        val liveArtworkIds by context?.listSource?.artworkIdsFlow?.collectAsState() 
            ?: remember { mutableStateOf(emptyList()) }
        
        // 当前实际使用的作品ID列表
        // 优先使用 listSource 的实时数据，否则降级到序列化的快照
        val currentArtworkIds = if (liveArtworkIds.isNotEmpty()) {
            liveArtworkIds
        } else {
            artworkIds
        }
        
        // 初始化加载作品详情
        LaunchedEffect(artworkId, initialIndex) {
            if (currentArtworkIds.isNotEmpty()) {
                // 列表导航模式
                // 如果有 listSource，传递其 loadMoreArtworks 方法
                val onLoadMore: (() -> Unit)? = context?.listSource?.let { source ->
                    { source.loadMoreArtworks() }
                }
                viewModel.initWithArtworkList(currentArtworkIds, initialIndex, onLoadMore)
            } else if (artworkId.isNotEmpty()) {
                // 单个作品模式
                viewModel.loadArtworkDetail(artworkId)
            }
        }
        
        // 监听 artworkIds 变化，动态更新 ViewModel
        // 当 listSource 的 StateFlow 发出新列表时，这里会自动触发
        LaunchedEffect(currentArtworkIds.size) {
            if (currentArtworkIds.isNotEmpty()) {
                viewModel.updateArtworkList(currentArtworkIds)
            }
        }
        
        // 退出页面的处理逻辑（左上角返回按钮使用）
        val handleExit: () -> Unit = {
            val currentIndex = viewModel.getCurrentIndex()
            context?.onReturnWithIndex?.invoke(currentIndex)
            // 清理上下文
            if (contextKey.isNotEmpty()) {
                NavigationContextManager.removeContext(contextKey)
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
        
        // 页面销毁时清理上下文（防止内存泄漏）
        DisposableEffect(contextKey) {
            onDispose {
                // 注意：这里不清理，因为可能是配置变化导致的重组
                // 清理在 handleExit 中进行
            }
        }
        
        // 拦截系统返回键和手势返回
        PlatformBackHandler(enabled = true, onBack = handleSystemBack)
        
        ArtworkDetailContent(
            state = state,
            onBackClick = handleExit,  // 左上角返回按钮直接退出
            onPageChange = { index -> viewModel.onPageChanged(index) },
            onRetry = { viewModel.retry() },
            onExpandInfo = { viewModel.expandInfo() },
            onCollapseInfo = { viewModel.collapseInfo() },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onSeriesClick = { seriesId ->
                navigator.push(MangaSeriesScreen(seriesId))
            },
            onCommentClick = {
                state.artwork?.let { artwork ->
                    navigator.push(
                        CommentsScreen(
                            contentId = artwork.id,
                            contentType = CommentContentType.ILLUST,
                            contentTitle = artwork.title
                        )
                    )
                }
            },
            onShareClick = {
                // 点击分享按钮：默认分享链接
                state.artwork?.let { artwork ->
                    shareViewModel.handleIntent(
                        ShareIntent.ShareArtwork(
                            artwork = artwork,
                            formattedText = formattedShareText,
                            shareType = ArtworkShareType.LINK_ONLY
                        )
                    )
                }
            },
            onShareLongClick = {
                // 长按分享按钮：显示分享选项
                showShareBottomSheet = true
            },
            onSimilarClick = {
                // 推荐作品按钮点击
                state.artwork?.let { artwork ->
                    navigator.push(ArtworkRecommendScreen(artworkId = artwork.id))
                }
            },
            onDownloadClick = {
                state.artwork?.let { artwork ->
                    coroutineScope.launch {
                        // 根据作品类型选择下载方法
                        val result = when (artwork.type) {
                            ArtworkType.UGOIRA -> {
                                // Ugoira 默认下载为 GIF（点击）
                                downloadRepository.addUgoiraDownload(artwork, UgoiraFormat.GIF)
                            }
                            else -> {
                                // 插画和漫画使用插画下载方法
                                downloadRepository.addIllustrationDownload(
                                    artwork = artwork,
                                    pageIndex = null  // null表示下载所有页
                                )
                            }
                        }
                        if (result.isSuccess) {
                            // 显示提示：已添加到下载列表，带跳转按钮
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
            onDownloadLongClick = {
                state.artwork?.let { artwork ->
                    coroutineScope.launch {
                        // 长按仅对 Ugoira 有效，下载为 MP4
                        if (artwork.type == ArtworkType.UGOIRA) {
                            val result = downloadRepository.addUgoiraDownload(artwork, UgoiraFormat.MP4)
                            if (result.isSuccess) {
                                // 显示提示：已添加到下载列表（MP4格式），带跳转按钮
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
                }
            },
            onImageClick = { pageIndex ->
                // 点击图片，跳转到大图浏览页面
                state.artwork?.let { artwork ->
                    // 只对插画和漫画有效（不包括动图）
                    if (artwork.type != ArtworkType.UGOIRA) {
                        navigator.push(
                            ArtworkImageViewerScreen(
                                artworkId = artwork.id,
                                artworkTitle = artwork.title,
                                pages = artwork.imageUrls.pages,
                                initialPage = pageIndex
                            )
                        )
                    }
                }
            },
            onTagClick = { tag -> tagClickHandler(tag) },
            onBlockTag = { tag ->
                // 跳转到屏蔽列表页面，并传入Tag进行预填充
                navigator.push(BlockListScreen(prefilledTag = tag.name))
            },
            onTranslateClick = if (isTranslationEnabled) {
                { viewModel.translateDescription() }
            } else null,
            onClearTranslation = if (isTranslationEnabled) {
                { viewModel.clearTranslation() }
            } else null,
            snackbarHostState = snackbarHostState
        )
        
        // 分享选项底部面板
        if (showShareBottomSheet) {
            state.artwork?.let { artwork ->
                ArtworkShareBottomSheet(
                    artwork = artwork,
                    onDismiss = { showShareBottomSheet = false },
                    onShareSelected = { shareType, pageIndex ->
                        showShareBottomSheet = false
                        shareViewModel.handleIntent(
                            ShareIntent.ShareArtwork(
                                artwork = artwork,
                                formattedText = formattedShareText,
                                shareType = shareType,
                                pageIndex = pageIndex
                            )
                        )
                    }
                )
            }
        }

    }
}
