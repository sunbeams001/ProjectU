package com.projectu.ui.screens.artwork

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.comment.CommentsScreen
import com.projectu.ui.screens.download.DownloadScreen
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.ui.util.PlatformBackHandler
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
        val coroutineScope = rememberCoroutineScope()
        
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
            onSimilarClick = null,  // 暂未实现
            onDownloadClick = {
                state.artwork?.let { artwork ->
                    coroutineScope.launch {
                        // 直接传入artwork对象，避免重复请求
                        val result = downloadRepository.addIllustrationDownload(
                            artwork = artwork,
                            pageIndex = null  // null表示下载所有页
                        )
                        if (result.isSuccess) {
                            // 可以显示一个提示：已添加到下载列表
                            // 跳转到下载页面
                            navigator.push(DownloadScreen())
                        }
                    }
                }
            }
        )
    }
}
