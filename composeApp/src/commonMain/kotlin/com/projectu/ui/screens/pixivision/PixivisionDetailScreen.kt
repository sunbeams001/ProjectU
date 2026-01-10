package com.projectu.ui.screens.pixivision

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.cache.ArtworkCacheManager
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * Pixivision 文章详情页面
 */
data class PixivisionDetailScreen(
    val articleId: String
) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = koinScreenModel<PixivisionDetailViewModel>()
        val state by viewModel.state.collectAsState()
        
        // 初始化加载（如果未加载或者不是当前文章）
        LaunchedEffect(articleId) {
            if (state.detail == null || state.detail!!.id != articleId) {
                viewModel.loadArticleDetail(articleId)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.pixivision_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
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
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null && state.detail == null -> {
                        ErrorDisplay(
                            message = state.error ?: "Unknown error",
                            onRetry = { viewModel.retry(articleId) },
                            modifier = Modifier.align(Alignment.Center),
                            isFullScreen = true
                        )
                    }
                    state.detail != null -> {
                        PixivisionDetailContent(
                            detail = state.detail!!,
                            artworkAuthors = state.detail!!.artworkAuthors,
                            onArtworkClick = { artworkId, index, artworkIds ->
                                // 创建列表源，支持左右翻页
                                val contextKey = NavigationContextManager.createContext()
                                
                                navigator?.push(
                                    ArtworkDetailScreen(
                                        artworkIds = artworkIds,
                                        initialIndex = index,
                                        contextKey = contextKey
                                    )
                                )
                            },
                            onOpenInBrowser = {
                                navigator?.push(PixivisionWebViewScreen(state.detail!!.url))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 详情页内容 - 使用瀑布流展示作品，统一滚动
 */
@Composable
fun PixivisionDetailContent(
    detail: com.projectu.shared.domain.model.pixivision.PixivisionDetail,
    artworkAuthors: Map<String, com.projectu.shared.domain.model.pixivision.PixivisionArtworkAuthor>,
    onArtworkClick: (String, Int, List<String>) -> Unit,
    onOpenInBrowser: () -> Unit = {}
) {
    val settingsCache: SettingsCache = koinInject()
    val columns by settingsCache.staggeredGridColumns.collectAsState()
    val artworkCacheManager: ArtworkCacheManager = koinInject()
    val listState = rememberLazyStaggeredGridState()
    
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = listState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        // 特辑信息卡片 - 占满整行
        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
            PixivisionInfoCard(
                detail = detail,
                onOpenInBrowser = onOpenInBrowser,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 间隔
        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 作品标题 - 占满整行
        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
            Text(
                text = stringResource(Res.string.pixivision_artworks_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // 作品瀑布流
        items(
            items = detail.artworkIds,
            key = { it }
        ) { artworkId ->
            val index = detail.artworkIds.indexOf(artworkId)
            
            // 响应式监听缓存中的作品数据
            val artwork by artworkCacheManager.getArtworkFlow(artworkId).collectAsState(initial = null)
            
            if (artwork != null) {
                // 仅在UI层临时使用Pixivision的高质量头像,不修改缓存
                val authorInfo = artworkAuthors[artworkId]
                val displayArtwork = if (authorInfo != null) {
                    artwork!!.copy(userProfileImageUrl = authorInfo.authorAvatarUrl)
                } else {
                    artwork!!
                }
                
                ArtworkCard(
                    artwork = displayArtwork,
                    onClick = { onArtworkClick(artworkId, index, detail.artworkIds) }
                )
            } else {
                // 占位卡片
                Card(
                    onClick = { onArtworkClick(artworkId, index, detail.artworkIds) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 特辑信息卡片
 */
@Composable
fun PixivisionInfoCard(
    detail: com.projectu.shared.domain.model.pixivision.PixivisionDetail,
    onOpenInBrowser: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 封面图片
            if (detail.coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = detail.coverImageUrl,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 标题和打开浏览器按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onOpenInBrowser,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = stringResource(Res.string.pixivision_open_in_browser),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 类别和发布日期
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类别标签
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = detail.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // 发布日期
                Text(
                    text = stringResource(Res.string.pixivision_published_date) + " ${detail.publishDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 简介
            if (detail.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(Res.string.pixivision_description),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
