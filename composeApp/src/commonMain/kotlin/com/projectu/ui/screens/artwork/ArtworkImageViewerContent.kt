package com.projectu.ui.screens.artwork

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.PageImageUrls
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.ui.components.RetryableAsyncImage
import com.projectu.ui.util.HideSystemUI
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*
import kotlin.math.abs

/**
 * 作品大图浏览器内容
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtworkImageViewerContent(
    artworkId: String,
    artworkTitle: String,
    pages: List<PageImageUrls>,
    initialPage: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadRepository: DownloadRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 是否显示底部信息蒙版
    var showOverlay by remember { mutableStateOf(false) }
    
    // HorizontalPager状态
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pages.size - 1),
        pageCount = { pages.size }
    )
    
    // 跟踪每个页面的缩放状态，用于控制Pager是否可滑动
    var isZoomed by remember { mutableStateOf(false) }
    
    // 隐藏系统导航栏（仅Android）
    HideSystemUI()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 图片查看器 - HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed  // 放大时禁用Pager滑动
        ) { pageIndex ->
            val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
            
            ZoomableImage(
                page = page,
                contentDescription = if (pages.size > 1) {
                    "$artworkTitle - ${stringResource(Res.string.novel_page_number, pageIndex + 1)}"
                } else {
                    artworkTitle
                },
                onSingleTap = {
                    // 单击切换蒙版显示/隐藏
                    showOverlay = !showOverlay
                },
                onZoomChange = { zoomed ->
                    // 更新缩放状态，控制Pager滑动
                    isZoomed = zoomed
                },
                pagerState = pagerState,
                currentPage = pageIndex
            )
        }
        
        // 顶部返回按钮（始终显示）
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.nav_back),
                tint = Color.White
            )
        }
        
        // 底部信息蒙版（可切换显示）
        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ImageInfoOverlay(
                artworkId = artworkId,
                currentPage = pagerState.currentPage,
                totalPages = pages.size,
                pageInfo = pages.getOrNull(pagerState.currentPage),
                onDownloadClick = {
                    coroutineScope.launch {
                        // 下载当前页（使用插画下载API，指定页码）
                        downloadRepository.addIllustrationDownload(
                            illustId = artworkId.toLong(),
                            pageIndex = pagerState.currentPage
                        ).onSuccess {
                            // 下载任务已添加，可以显示提示
                        }.onFailure { error ->
                            // 处理错误
                        }
                    }
                }
            )
        }
    }
}

/**
 * 可缩放的图片组件
 * 支持双击缩放、双指缩放、拖动、单击回调
 * 当图片放大时，禁用Pager滑动；原始大小时允许Pager滑动
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(
    page: PageImageUrls,
    contentDescription: String,
    onSingleTap: () -> Unit,
    onZoomChange: (Boolean) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    // 缩放和偏移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // 动画状态
    val animatedScale = remember { Animatable(1f) }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 同步动画值到实际状态
    LaunchedEffect(animatedScale.value) {
        scale = animatedScale.value
        // 通知父组件缩放状态变化
        onZoomChange(scale > 1f)
    }
    LaunchedEffect(animatedOffset.value) {
        offset = animatedOffset.value
    }
    
    // 重置状态（切换页面时）
    LaunchedEffect(currentPage) {
        animatedScale.snapTo(1f)
        animatedOffset.snapTo(Offset.Zero)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // 单击和双击检测
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (animatedScale.value > 1f) {
                                // 缩小到原始大小（带动画）
                                animatedScale.animateTo(1f, tween(300))
                                animatedOffset.animateTo(Offset.Zero, tween(300))
                            } else {
                                // 放大到2.5倍（带动画）
                                animatedScale.animateTo(2.5f, tween(300))
                            }
                        }
                    },
                    onTap = {
                        // 只有在未放大时才触发单击
                        if (scale <= 1f) {
                            onSingleTap()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // 双指缩放和拖动手势
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    
                    awaitFirstDown(requireUnconsumed = false)
                    
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            
                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()
                                
                                if (zoomMotion > touchSlop) {
                                    pastTouchSlop = true
                                } else if (panMotion > touchSlop) {
                                    // 检查是否为水平滑动
                                    val horizontalMovement = abs(panChange.x)
                                    val verticalMovement = abs(panChange.y)
                                    
                                    // 如果是未放大且主要是水平滑动，不消费事件，让 Pager 处理
                                    if (animatedScale.value <= 1f && horizontalMovement > verticalMovement * 2) {
                                        // 不消费事件，让 HorizontalPager 处理
                                        break
                                    } else {
                                        pastTouchSlop = true
                                    }
                                }
                            }
                            
                            if (pastTouchSlop) {
                                coroutineScope.launch {
                                    val newScale = (animatedScale.value * zoomChange).coerceIn(1f, 2.5f)
                                    if (abs(newScale - animatedScale.value) > 0.01f) {
                                        animatedScale.snapTo(newScale)
                                    }
                                    
                                    // 平移（仅在放大时生效）
                                    if (newScale > 1f) {
                                        val newOffset = animatedOffset.value + panChange
                                        animatedOffset.snapTo(newOffset)
                                    } else {
                                        if (animatedOffset.value != Offset.Zero) {
                                            animatedOffset.snapTo(Offset.Zero)
                                        }
                                    }
                                }
                                
                                // 消费事件
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 使用原图质量
        val imageUrl = page.urls.original ?: page.urls.master1200 ?: page.urls.large ?: ""
        
        // RetryableAsyncImage已经内置了加载指示器
        RetryableAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            showErrorDetails = true
        )
    }
}

/**
 * 底部信息蒙版
 * 显示分辨率、页号、下载按钮
 */
@Composable
private fun ImageInfoOverlay(
    artworkId: String,
    currentPage: Int,
    totalPages: Int,
    pageInfo: PageImageUrls?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：分辨率和页号信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 分辨率
                if (pageInfo != null) {
                    Text(
                        text = "${pageInfo.width} × ${pageInfo.height}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                
                // 页号（仅多页时显示）
                if (totalPages > 1) {
                    Text(
                        text = "${currentPage + 1}/$totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // 右侧：下载按钮
            FilledTonalButton(
                onClick = onDownloadClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(Res.string.artwork_download),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.artwork_download),
                    color = Color.White
                )
            }
        }
    }
}
