package com.projectu.ui.screens.novelseries

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.screens.user.UserScreen

/**
 * 小说系列详情页面
 * 
 * 布局设计：
 * - 上方：系列信息卡片（封面、标题、简介、统计信息、追更按钮）
 * - 下方：系列中的小说列表（复用 NovelCard）
 * 
 * @param seriesId 系列ID
 */
data class NovelSeriesScreen(
    private val seriesId: Long
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NovelSeriesViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 加载系列数据
        LaunchedEffect(seriesId) {
            viewModel.loadSeries(seriesId)
        }
        
        NovelSeriesContent(
            state = state,
            onBackClick = { navigator.pop() },
            onToggleWatch = viewModel::toggleWatch,
            onLoadMore = viewModel::loadMore,
            onRetryDetails = viewModel::retrySeries,
            onRetryContents = viewModel::retryContents,
            onRefresh = viewModel::refresh,
            onNovelClick = { novel ->
                // TODO: 跳转到小说详情页
                println("点击小说: ${novel.id} - ${novel.title}")
            },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            }
        )
    }
}
