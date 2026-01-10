package com.projectu.ui.screens.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticle
import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import com.projectu.ui.components.PixivisionArticleCard
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import projectu.composeapp.generated.resources.Res

/**
 * 发现页 - Pixivision 文章列表
 * 支持插画和漫画两个类别
 */
class DiscoveryPixivisionScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscoveryPixivisionViewModel>()
        val state by viewModel.state.collectAsState()
        
        // 初次加载
        LaunchedEffect(Unit) {
            viewModel.initLoadIfNeeded()
        }
        
        Column(modifier = Modifier.fillMaxSize()) {
            // 类别切换 Tab
            CategoryTabs(
                currentCategory = state.currentCategory,
                onCategoryChange = { viewModel.switchCategory(it) }
            )
            
            // 文章列表
            ArticleList(
                articles = state.getCurrentArticles(),
                isLoading = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                error = state.error,
                hasMore = state.getCurrentHasMore(),
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadMore() },
                onArticleClick = { article ->
                    // TODO: 导航到文章详情页
                    // navigationContext.navigateToPixivisionArticleDetail(article.id)
                }
            )
        }
    }
}

/**
 * 类别切换 Tab
 */
@Composable
private fun CategoryTabs(
    currentCategory: PixivisionCategory,
    onCategoryChange: (PixivisionCategory) -> Unit
) {
    TabRow(
        selectedTabIndex = when (currentCategory) {
            PixivisionCategory.ILLUSTRATION -> 0
            PixivisionCategory.MANGA -> 1
        }
    ) {
        Tab(
            selected = currentCategory == PixivisionCategory.ILLUSTRATION,
            onClick = { onCategoryChange(PixivisionCategory.ILLUSTRATION) },
            text = { Text(stringResource(Res.string.pixivision_illustration)) }
        )
        Tab(
            selected = currentCategory == PixivisionCategory.MANGA,
            onClick = { onCategoryChange(PixivisionCategory.MANGA) },
            text = { Text(stringResource(Res.string.pixivision_manga)) }
        )
    }
}

/**
 * 文章列表
 */
@Composable
private fun ArticleList(
    articles: List<PixivisionArticle>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    hasMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onArticleClick: (PixivisionArticle) -> Unit
) {
    val listState = rememberLazyListState()
    
    // 滚动到底部时自动加载更多
    LaunchedEffect(listState.canScrollForward, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= articles.size - 3 && 
                    !isLoadingMore && 
                    hasMore &&
                    articles.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 初次加载中
            isLoading && articles.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 加载失败
            error != null && articles.isEmpty() -> {
                ErrorContent(
                    error = error,
                    onRetry = onRefresh
                )
            }
            
            // 显示列表
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = articles,
                        key = { it.id }
                    ) { article ->
                        PixivisionArticleCard(
                            article = article,
                            onClick = { onArticleClick(article) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore && hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    // 没有更多数据提示
                    if (!hasMore && articles.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.list_no_more_items),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 错误内容显示
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}
