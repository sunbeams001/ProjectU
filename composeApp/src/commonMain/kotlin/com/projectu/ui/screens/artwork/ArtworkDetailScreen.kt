package com.projectu.ui.screens.artwork

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.Artwork

/**
 * 作品详情页面
 * 
 * 布局设计：
 * - 上方：作品展示区域（占据剩余全部高度）
 *   - 单页作品：居中缩放完全展示
 *   - 多页作品：宽度填充，可垂直滚动
 *   - 动图：居中展示（后期完善）
 * - 下方：固定高度的作品信息区域
 */
data class ArtworkDetailScreen(
    val artworkId: String
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ArtworkDetailViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 初始化加载作品详情
        LaunchedEffect(artworkId) {
            viewModel.loadArtworkDetail(artworkId)
        }
        
        ArtworkDetailContent(
            state = state,
            onBackClick = { navigator.pop() }
        )
    }
}
