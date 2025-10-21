package com.projectu.ui.components

import androidx.compose.foundation.Image
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

/**
 * Ugoira动图播放器组件
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
    
    // 播放动画
    LaunchedEffect(frameBitmaps, isPlaying, playbackSpeed) {
        if (frameBitmaps.isNotEmpty() && isPlaying) {
            while (isPlaying) {
                metadata.frames.forEachIndexed { index, frame ->
                    if (!isPlaying) return@LaunchedEffect
                    currentFrameIndex = index
                    val adjustedDelay = (frame.delay / playbackSpeed).toLong()
                    delay(adjustedDelay)
                }
            }
        }
    }
    
    Box(modifier = modifier) {
        // 显示当前帧
        if (frameBitmaps.isNotEmpty() && currentFrameIndex < frameBitmaps.size) {
            Image(
                bitmap = frameBitmaps[currentFrameIndex],
                contentDescription = "Ugoira Frame ${currentFrameIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // 加载中或错误状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // 控制栏
        if (showControls && frameBitmaps.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放/暂停按钮
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                    
                    // 进度指示
                    Text(
                        text = "${currentFrameIndex + 1} / ${frameBitmaps.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    // 速度控制
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "×${playbackSpeed.toString().take(3)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
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
                            }
                        ) {
                            Text("Speed")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ugoira加载状态
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
 * 带加载状态的Ugoira播放器
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
                        text = "Downloading animation...",
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
                        text = "Extracting frames...",
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
                        text = "Loading frames... ${(loadState.progress * 100).toInt()}%",
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
                        text = "Error: ${loadState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

