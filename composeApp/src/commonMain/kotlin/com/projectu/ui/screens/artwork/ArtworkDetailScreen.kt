package com.projectu.ui.screens.artwork

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.Artwork
import androidx.activity.compose.BackHandler

/**
 * 作品详情页面
 * 
 * 布局设计：
 * - 上方：作品展示区域（占据剩余全部高度）
 *   - 单页作品：居中缩放完全展示
 *   - 多页作品：宽度填充，可垂直滚动
 *   - 动图：居中展示（后期完善）
 * - 下方：固定高度的作品信息区域
 * 
 * 支持列表上下文导航：
 * - 当提供 artworkIds 和 initialIndex 时，支持左右滑动浏览列表中的其他作品
 * - 单独使用 artworkId 时，仅展示单个作品详情
 * 
 * @param artworkId 单个作品ID（单独使用时）
 * @param artworkIds 作品ID列表的 State（列表导航模式，响应式更新）
 * @param initialIndex 初始作品在列表中的索引（配合 artworkIds 使用）
 * @param onLoadMore 加载更多回调（接近列表末尾时触发）
 * @param onReturnWithIndex 返回时的回调，传递最后浏览的作品索引
 */
data class ArtworkDetailScreen(
    val artworkId: String = "",
    val artworkIds: State<List<String>>? = null,
    val initialIndex: Int = 0,
    val onLoadMore: (() -> Unit)? = null,
    val onReturnWithIndex: ((Int) -> Unit)? = null
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ArtworkDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 获取响应式的作品ID列表
        val currentArtworkIds = artworkIds?.value ?: emptyList()
        
        // 初始化加载作品详情
        LaunchedEffect(artworkId, initialIndex) {
            if (artworkIds != null) {
                // 列表导航模式
                viewModel.initWithArtworkList(currentArtworkIds, initialIndex, onLoadMore)
            } else {
                // 单个作品模式
                viewModel.loadArtworkDetail(artworkId)
            }
        }
        
        // 监听 artworkIds 变化，动态更新列表
        LaunchedEffect(currentArtworkIds.size) {
            if (artworkIds != null && currentArtworkIds.isNotEmpty()) {
                viewModel.updateArtworkList(currentArtworkIds)
            }
        }
        
        // 处理返回逻辑（统一处理按钮返回和系统返回）
        val handleBack: () -> Unit = {
            val currentIndex = viewModel.getCurrentIndex()
            onReturnWithIndex?.invoke(currentIndex)
            navigator.pop()
        }
        
        // 拦截系统返回键和手势返回
        BackHandler(enabled = true, onBack = handleBack)
        
        ArtworkDetailContent(
            state = state,
            onBackClick = handleBack,
            onPageChange = { index -> viewModel.onPageChanged(index) },
            onRetry = { viewModel.retry() }
        )
    }
}
