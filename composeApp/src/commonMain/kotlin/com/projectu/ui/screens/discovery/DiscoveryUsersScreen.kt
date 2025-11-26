package com.projectu.ui.screens.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.User
import com.projectu.ui.components.UserCard
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.discovery_recommended_users

/**
 * 发现用户页面
 */
class DiscoveryUsersScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscoveryUsersViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        DiscoveryUsersContent(
            state = state,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onUserClick = { user ->
                // TODO: 跳转到用户详情页
                println("点击用户: ${user.name}")
            },
            onArtworkClick = { artwork ->
                // TODO: 跳转到作品详情页
                println("点击作品: ${artwork.title}")
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryUsersContent(
    state: DiscoveryUsersState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.discovery_recommended_users)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.users.isEmpty() -> {
                    // 初次加载
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.users.isEmpty() -> {
                    // 错误状态
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
                state.users.isNotEmpty() -> {
                    // 用户列表展示
                    UserList(
                        users = state.users,
                        onUserClick = onUserClick,
                        onArtworkClick = onArtworkClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore
                    )
                }
            }
        }
    }
}

/**
 * 用户列表
 */
@Composable
fun UserList(
    users: List<User>,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val listState = rememberLazyListState()
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState, users.size, isLoadingMore) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            // 当最后一个可见项的索引 >= 总项数 - 3 时触发加载更多
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }
        .distinctUntilChanged() // 避免重复触发
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(users, key = { it.id }) { user ->
            UserCard(
                user = user,
                onUserClick = onUserClick,
                onArtworkClick = onArtworkClick
            )
        }
        
        // 加载更多指示器
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
