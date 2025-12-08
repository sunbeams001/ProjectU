package com.projectu.ui.screens.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 下载列表界面
 */
class DownloadScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: DownloadViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        
        DownloadScreenContent(
            viewModel = viewModel,
            onNavigateBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadScreenContent(
    viewModel: DownloadViewModel,
    onNavigateBack: () -> Unit
) {
    val tasks by viewModel.downloadTasks.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 状态筛选菜单
                    var showMenu by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.FilterList, "筛选")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部") },
                            onClick = {
                                viewModel.filterByStatus(null)
                                showMenu = false
                            },
                            leadingIcon = if (selectedStatus == null) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("下载中") },
                            onClick = {
                                viewModel.filterByStatus(DownloadStatus.DOWNLOADING)
                                showMenu = false
                            },
                            leadingIcon = if (selectedStatus == DownloadStatus.DOWNLOADING) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("已暂停") },
                            onClick = {
                                viewModel.filterByStatus(DownloadStatus.PAUSED)
                                showMenu = false
                            },
                            leadingIcon = if (selectedStatus == DownloadStatus.PAUSED) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("已完成") },
                            onClick = {
                                viewModel.filterByStatus(DownloadStatus.COMPLETED)
                                showMenu = false
                            },
                            leadingIcon = if (selectedStatus == DownloadStatus.COMPLETED) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("失败") },
                            onClick = {
                                viewModel.filterByStatus(DownloadStatus.FAILED)
                                showMenu = false
                            },
                            leadingIcon = if (selectedStatus == DownloadStatus.FAILED) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (selectedStatus == null) "暂无下载任务" else "该状态下暂无任务",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = tasks,
                    key = { it.id }
                ) { task ->
                    DownloadTaskItem(
                        task = task,
                        onStart = { viewModel.startDownload(task.id) },
                        onPause = { viewModel.pauseDownload(task.id) },
                        onDelete = { viewModel.deleteDownload(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 缩略图（仅插画和漫画显示）
            if (task.resourceType == ResourceType.ILLUSTRATION || 
                task.resourceType == ResourceType.MANGA) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val thumbnailUrl = getThumbnailUrl(task)
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = task.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 如果没有缩略图 URL，显示占位图标
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    // 状态图标覆盖在缩略图上
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            StatusIcon(
                                status = task.status,
                                modifier = Modifier.padding(2.dp).size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${task.authorName} • ${getResourceTypeText(task.resourceType)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 非图片类型显示状态图标
                    if (task.resourceType != ResourceType.ILLUSTRATION && 
                        task.resourceType != ResourceType.MANGA) {
                        StatusIcon(status = task.status)
                    }
                }
                
                // 进度条
                if (task.status != DownloadStatus.COMPLETED && task.status != DownloadStatus.FAILED) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { task.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatFileSize(task.downloadedSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(task.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 错误信息
                task.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (task.status) {
                        DownloadStatus.PENDING, DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                            FilledTonalButton(
                                onClick = onStart,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, "开始", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("开始")
                            }
                        }
                        DownloadStatus.DOWNLOADING -> {
                            FilledTonalButton(
                                onClick = onPause,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Pause, "暂停", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("暂停")
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            // 已完成状态不显示操作按钮，只保留删除按钮
                        }
                        DownloadStatus.CANCELLED -> {
                            FilledTonalButton(
                                onClick = onStart,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, "重试", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("重试")
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    status: DownloadStatus,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val (icon, tint) = when (status) {
        DownloadStatus.PENDING -> Icons.Default.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.DOWNLOADING -> Icons.Default.Download to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> Icons.Default.Pause to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> Icons.Default.Cancel to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = tint,
        modifier = modifier
    )
}

/**
 * 获取缩略图 URL
 * 直接使用存储在数据库中的真实 Pixiv API 返回的缩略图地址
 */
private fun getThumbnailUrl(task: DownloadTask): String? {
    return task.thumbnailUrl
}

private fun getResourceTypeText(type: ResourceType): String {
    return when (type) {
        ResourceType.ILLUSTRATION -> "插画"
        ResourceType.MANGA -> "漫画"
        ResourceType.UGOIRA -> "动图"
        ResourceType.NOVEL -> "小说"
        ResourceType.NOVEL_SERIES -> "小说系列"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
