package com.projectu.ui.screens.mangaseries

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.user.UserScreen

/**
 * 漫画系列详情页面
 * 
 * 布局设计：
 * - 上方：系列信息卡片（封面、标题、简介、统计信息、追更按钮）
 * - 下方：系列中的作品列表（使用瀑布流布局）
 * 
 * 支持作品列表导航：点击作品进入详情页后，可以左右滑动浏览系列中的其他作品
 * 
 * @param seriesId 系列ID
 */
data class MangaSeriesScreen(
    private val seriesId: String
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<MangaSeriesViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 保存浏览历史
        com.projectu.ui.util.SaveMangaSeriesHistory(state.series)
        
        // 记录滚动位置，用于从详情页返回时滚动到对应位置
        var lastScrollIndex by remember { mutableIntStateOf(0) }
        
        // 加载系列数据
        LaunchedEffect(seriesId) {
            viewModel.loadSeries(seriesId)
        }
        
        MangaSeriesContent(
            state = state,
            onBackClick = { navigator.pop() },
            onToggleWatch = viewModel::toggleWatch,
            onLoadMore = viewModel::loadMore,
            onRetryDetails = viewModel::retrySeries,
            onRetryContents = viewModel::retryContents,
            onRefresh = viewModel::refresh,
            onArtworkClick = { artwork ->
                // 获取当前作品列表
                val artworkIds = state.artworks.map { it.id }
                val index = artworkIds.indexOf(artwork.id).coerceAtLeast(0)
                
                // 创建列表源，支持响应式列表更新
                val listSource = viewModel.createArtworkListSource()
                
                // 创建导航上下文
                val contextKey = NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { returnIndex ->
                        lastScrollIndex = returnIndex
                    }
                )
                
                // 跳转到作品详情页（列表导航模式）
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = artworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId = userId))
            }
        )
    }
}
