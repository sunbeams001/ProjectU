package com.projectu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Ugoira动图播放器组件
 * 
 * 布局设计：
 * - 上方：动图展示区域（占据剩余空间）
 * - 下方：固定高度的播放控制栏（仅当 showControls=true 且有帧时显示）
 */
@Composable
fun UgoiraPlayer(
    metadata: UgoiraMetadata,
    frameBitmaps: List<ImageBitmap>,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    showControls: Boolean = true
) {
    var currentFrameIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    
    // 播放动画 - 从当前帧继续播放
    LaunchedEffect(frameBitmaps, isPlaying, playbackSpeed) {
        if (frameBitmaps.isNotEmpty() && isPlaying) {
            while (isPlaying) {
                // 从当前帧开始播放到末尾
                for (index in currentFrameIndex until metadata.frames.size) {
                    if (!isPlaying) return@LaunchedEffect
                    currentFrameIndex = index
                    val adjustedDelay = (metadata.frames[index].delay / playbackSpeed).toLong()
                    delay(adjustedDelay)
                }
                // 循环：回到第一帧继续
                currentFrameIndex = 0
            }
        }
    }
    
    // 是否显示控制栏
    val shouldShowControls = showControls && frameBitmaps.isNotEmpty()
    
    Column(modifier = modifier) {
        // 动图展示区域（占据剩余空间）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (frameBitmaps.isNotEmpty() && currentFrameIndex < frameBitmaps.size) {
                Image(
                    bitmap = frameBitmaps[currentFrameIndex],
                    contentDescription = "Ugoira Frame ${currentFrameIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // 加载中或错误状态
                CircularProgressIndicator()
            }
        }
        
        // 控制栏（固定在底部，紧凑布局）
        if (shouldShowControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 帧序号（左侧，占据剩余空间并居中）
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentFrameIndex + 1}/${frameBitmaps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 播放/暂停按钮（靠右）
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 速度控制（最右侧）
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "×${playbackSpeed.toString().take(3)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 速度选择按钮
                    TextButton(
                        onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                0.5f -> 1.0f
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.5f
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "倍速",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 完整的 Ugoira 展示组件
 * 
 * 自动处理：
 * - 元数据获取
 * - ZIP 下载
 * - 帧解压
 * - 图片加载
 * - 动画播放
 * 
 * @param artworkId 作品ID
 * @param modifier 修饰符
 */
@Composable
fun UgoiraDisplay(
    artworkId: String,
    modifier: Modifier = Modifier
) {
    val loaderManager: UgoiraLoaderManager = koinInject()
    val loadingState by loaderManager.loadingState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // 加载动图
    LaunchedEffect(artworkId) {
        loaderManager.load(artworkId)
    }
    
    // 清理
    DisposableEffect(artworkId) {
        onDispose {
            loaderManager.reset()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = loadingState) {
            is UgoiraLoadingState.Idle -> {
                CircularProgressIndicator()
            }
            
            is UgoiraLoadingState.FetchingMetadata -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "获取动图信息...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            is UgoiraLoadingState.Downloading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.progress > 0f) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.width(200.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                    Text(
                        text = "下载动画文件...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            is UgoiraLoadingState.Extracting -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "解压帧图片...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            is UgoiraLoadingState.LoadingFrames -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val progress = state.current.toFloat() / state.total
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.width(200.dp)
                    )
                    Text(
                        text = "加载帧图片 ${state.current}/${state.total}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            is UgoiraLoadingState.Ready -> {
                UgoiraPlayer(
                    metadata = state.metadata,
                    frameBitmaps = state.frames,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is UgoiraLoadingState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "加载失败: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        coroutineScope.launch {
                            loaderManager.retry()
                        }
                    }) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

/**
 * Ugoira加载状态（旧版，保持兼容）
 */
sealed interface UgoiraLoadState {
    data object Idle : UgoiraLoadState
    data object Downloading : UgoiraLoadState
    data object Extracting : UgoiraLoadState
    data class Loading(val progress: Float) : UgoiraLoadState
    data class Ready(val frames: List<ImageBitmap>) : UgoiraLoadState
    data class Error(val message: String) : UgoiraLoadState
}

/**
 * 带加载状态的Ugoira播放器（旧版，保持兼容）
 */
@Composable
fun UgoiraPlayerWithLoader(
    metadata: UgoiraMetadata,
    loadState: UgoiraLoadState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (loadState) {
            is UgoiraLoadState.Idle -> {
                CircularProgressIndicator()
            }
            
            is UgoiraLoadState.Downloading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "下载动画文件...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            is UgoiraLoadState.Extracting -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "解压帧图片...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            is UgoiraLoadState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { loadState.progress },
                        modifier = Modifier.width(200.dp)
                    )
                    Text(
                        text = "加载帧图片... ${(loadState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            is UgoiraLoadState.Ready -> {
                UgoiraPlayer(
                    metadata = metadata,
                    frameBitmaps = loadState.frames,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is UgoiraLoadState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "加载失败: ${loadState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

