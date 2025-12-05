package com.projectu.ui.screens.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 发现用户页面
 */
class DiscoveryUsersScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscoveryUsersViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        // 滚动位置记录
        val scrollIndex = remember { mutableStateOf(0) }
        
        // 惰性加载：只在首次显示且没有数据时加载
        LaunchedEffect(Unit) {
            viewModel.initLoadIfNeeded()
        }
        
        DiscoveryUsersContent(
            state = state,
            scrollIndex = scrollIndex,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            onUserClick = { user ->
                // 跳转到用户主页
                navigator.push(UserScreen(user.id))
            },
            onArtworkClick = { artwork, index, allArtworkIds ->
                // 创建列表源
                val listSource = viewModel.createArtworkListSource()
                
                // 创建导航上下文
                val contextKey = NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { returnIndex ->
                        scrollIndex.value = returnIndex
                    }
                )
                
                // 跳转到作品详情页
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = allArtworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryUsersContent(
    state: DiscoveryUsersState,
    scrollIndex: MutableState<Int>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int, List<String>) -> Unit,
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
                            contentDescription = stringResource(Res.string.nav_back)
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
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
                state.users.isNotEmpty() -> {
                    // 收集所有作品ID（使用完整列表）
                    val allArtworkIds = remember(state.users) {
                        state.users.flatMap { user -> user.illusts.map { it.id } }
                    }
                    
                    // 用户列表展示
                    UserList(
                        users = state.users,
                        scrollIndex = scrollIndex,
                        allArtworkIds = allArtworkIds,
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
    scrollIndex: MutableState<Int>,
    allArtworkIds: List<String>,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int, List<String>) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean
) {
    val listState = rememberLazyListState()
    
    // 返回后恢复滚动位置
    LaunchedEffect(scrollIndex.value) {
        if (scrollIndex.value > 0) {
            // 计算需要滚动到的用户索引
            var accumulatedArtworks = 0
            var targetUserIndex = 0
            for ((index, user) in users.withIndex()) {
                val userArtworkCount = user.illusts.size
                if (accumulatedArtworks + userArtworkCount > scrollIndex.value) {
                    targetUserIndex = index
                    break
                }
                accumulatedArtworks += userArtworkCount
                targetUserIndex = index + 1
            }
            listState.animateScrollToItem(targetUserIndex.coerceAtMost(users.size - 1).coerceAtLeast(0))
        }
    }
    
    // 预计算每个用户的作品起始索引（使用完整列表）
    val userArtworkStartIndices = remember(users) {
        var index = 0
        users.map { user ->
            val startIndex = index
            index += user.illusts.size
            startIndex
        }
    }
    
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
        itemsIndexed(users, key = { _, user -> user.id }) { index, user ->
            val artworkStartIndex = userArtworkStartIndices.getOrElse(index) { 0 }
            UserCard(
                user = user,
                onUserClick = onUserClick,
                onArtworkClick = { artwork, localIndex ->
                    val globalIndex = artworkStartIndex + localIndex
                    onArtworkClick(artwork, globalIndex, allArtworkIds)
                },
                artworkStartIndex = 0  // 传入 0，因为我们在外部计算全局索引
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
