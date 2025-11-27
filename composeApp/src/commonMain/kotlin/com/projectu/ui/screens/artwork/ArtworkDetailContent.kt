package com.projectu.ui.screens.artwork

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.getUrlByQuality
import org.koin.compose.koinInject

/**
 * 作品详情页主内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkDetailContent(
    state: ArtworkDetailState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            state.artwork != null -> {
                ArtworkDetailLayout(
                    artwork = state.artwork,
                    authorFollowStatus = state.authorFollowStatus,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding() // 为状态栏添加 padding（Android）
                )
            }
        }
        
        // 浮动的返回按钮
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding() // 返回按钮也需要避开状态栏
                .padding(start = 4.dp, top = 4.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 作品详情布局
 * 上方：作品展示区域（占据剩余空间）
 * 下方：固定高度的信息区域
 */
@Composable
private fun ArtworkDetailLayout(
    artwork: Artwork,
    authorFollowStatus: com.projectu.shared.domain.model.FollowStatus,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 作品展示区域（占据剩余空间）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            ArtworkDisplayArea(artwork = artwork)
        }

        // 作品信息区域（固定高度）
        ArtworkInfoSection(
            artwork = artwork,
            authorFollowStatus = authorFollowStatus,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 作品展示区域
 * 根据作品类型和页数采用不同的展示方式
 */
@Composable
private fun ArtworkDisplayArea(
    artwork: Artwork,
    modifier: Modifier = Modifier,
    settingsCache: SettingsCache = koinInject()
) {
    val imageQuality = settingsCache.getDetailImageQuality()
    val pages = artwork.imageUrls.pages

    when {
        // 动图类型（暂时简单展示第一页）
        artwork.type == ArtworkType.UGOIRA -> {
            SinglePageDisplay(
                imageUrl = pages.firstOrNull()?.getUrlByQuality(imageQuality) ?: "",
                contentDescription = artwork.title,
                modifier = modifier
            )
        }

        // 单页作品（插画或漫画）
        pages.size == 1 -> {
            SinglePageDisplay(
                imageUrl = pages.first().getUrlByQuality(imageQuality),
                contentDescription = artwork.title,
                modifier = modifier
            )
        }

        // 多页作品
        else -> {
            MultiPageDisplay(
                pages = pages,
                imageQuality = imageQuality,
                contentDescription = artwork.title,
                modifier = modifier
            )
        }
    }
}

/**
 * 单页作品展示
 * 居中并缩放至完全展示
 */
@Composable
private fun SinglePageDisplay(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 多页作品展示
 * 每一页宽度填充，高度根据实际情况自适应
 * 如果总高度小于等于区域高度，垂直居中展示
 * 如果总高度超过区域高度，第一张置顶，可滚动浏览
 */
@Composable
private fun MultiPageDisplay(
    pages: List<com.projectu.shared.domain.model.PageImageUrls>,
    imageQuality: com.projectu.shared.domain.model.DetailImageQuality,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidthDp = maxWidth
        val containerHeightDp = maxHeight

        // 计算每张图片按宽度填充后的高度
        val imageHeights = pages.map { page ->
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            containerWidthDp * aspectRatio
        }

        val totalHeight = imageHeights.fold(0.dp) { acc, height -> acc + height }

        // 判断是否需要滚动
        val needsScroll = totalHeight > containerHeightDp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (needsScroll) Arrangement.Top else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(pages) { page ->
                AsyncImage(
                    model = page.getUrlByQuality(imageQuality),
                    contentDescription = "$contentDescription - 第${page.page + 1}页",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(page.width.toFloat() / page.height.toFloat())
                )
            }
        }
    }
}
