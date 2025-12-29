package com.projectu.ui.screens.novelseries

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.screens.download.DownloadScreen
import com.projectu.ui.screens.search.SearchResultScreen
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.shared.data.local.SearchHistoryStore
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.download_action_view
import projectu.composeapp.generated.resources.download_task_added

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
    private val seriesId: String
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NovelSeriesViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val downloadRepository: DownloadRepository = koinInject()
        val searchHistoryStore: SearchHistoryStore = koinInject()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val downloadTaskAddedMessage = stringResource(Res.string.download_task_added)
        val downloadActionViewLabel = stringResource(Res.string.download_action_view)
        
        // 创建Tag点击处理器
        val tagClickHandler = remember(navigator, searchHistoryStore, coroutineScope) {
            TagClickHandler(navigator, searchHistoryStore, coroutineScope)
        }
        
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
                // 跳转到小说详情页
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onTagClick = tagClickHandler::handleTagClick,
            onDownloadClick = {
                coroutineScope.launch {
                    val result = downloadRepository.addNovelSeriesDownload(seriesId = seriesId)
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
            },
            snackbarHostState = snackbarHostState
        )
    }
}
