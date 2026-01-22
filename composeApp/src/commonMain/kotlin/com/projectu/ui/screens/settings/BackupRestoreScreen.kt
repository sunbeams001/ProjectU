package com.projectu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.util.rememberFilePicker
import com.projectu.ui.util.rememberPathPicker
import com.projectu.ui.util.needsSafAuthorization
import com.projectu.ui.util.PathDisplay
import com.projectu.shared.domain.model.BackupConfig
import com.projectu.shared.domain.model.BackupInfo
import com.projectu.shared.domain.model.BackupModule
import com.projectu.shared.domain.model.BackupResult
import com.projectu.shared.domain.model.RestoreResult
import com.projectu.shared.util.DateTimeFormatter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import projectu.composeapp.generated.resources.*

/**
 * 备份与恢复功能界面
 */
class BackupRestoreScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: BackupRestoreViewModel = koinViewModel()
        val state by viewModel.state.collectAsState()
        
        var showSuccessDialog by remember { mutableStateOf<BackupSuccessData?>(null) }
        var showErrorDialog by remember { mutableStateOf<ErrorData?>(null) }
        
        // 预先获取所有字符串资源
        val backupCreatedTitle = stringResource(Res.string.backup_created_title)
        val backupCreatedMessage = Res.string.backup_created_message
        val restoreCompleteTitle = stringResource(Res.string.restore_complete_title)
        val restoreCompleteMessage = Res.string.restore_complete_message
        val setupFailedTitle = stringResource(Res.string.setup_failed_title)
        val setupFailedNoPermission = stringResource(Res.string.setup_failed_no_permission)
        val backupFailedTitle = stringResource(Res.string.backup_failed_title)
        val restoreFailedTitle = stringResource(Res.string.restore_failed_title)
        val invalidFileTitle = stringResource(Res.string.invalid_file_title)
        val invalidFileMessage = stringResource(Res.string.invalid_file_message)
        val fileErrorTitle = stringResource(Res.string.file_error_title)
        val fileErrorMessage = stringResource(Res.string.file_error_message)
        val noFileSelectedTitle = stringResource(Res.string.no_file_selected_title)
        val noFileSelectedMessage = stringResource(Res.string.no_file_selected_message)
        val deleteFailedTitle = stringResource(Res.string.delete_failed_title)
        val deleteFailedMessage = stringResource(Res.string.delete_failed_message)
        val loadFailedTitle = stringResource(Res.string.load_failed_title)
        val loadFailedMessage = stringResource(Res.string.load_failed_message)
        
        // 监听副作用
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is BackupRestoreEffect.ShowBackupCreated -> {
                        showSuccessDialog = BackupSuccessData(
                            title = backupCreatedTitle,
                            message = org.jetbrains.compose.resources.getString(backupCreatedMessage, effect.filePath)
                        )
                    }
                    is BackupRestoreEffect.ShowRestoreComplete -> {
                        showSuccessDialog = BackupSuccessData(
                            title = restoreCompleteTitle,
                            message = org.jetbrains.compose.resources.getString(restoreCompleteMessage, effect.modulesCount, effect.recordsCount),
                            needsRestart = true
                        )
                    }
                    is BackupRestoreEffect.ShowSetupFailed -> {
                        showErrorDialog = ErrorData(
                            title = setupFailedTitle,
                            message = effect.message ?: setupFailedNoPermission
                        )
                    }
                    is BackupRestoreEffect.ShowBackupFailed -> {
                        showErrorDialog = ErrorData(
                            title = backupFailedTitle,
                            message = effect.message
                        )
                    }
                    is BackupRestoreEffect.ShowRestoreFailed -> {
                        showErrorDialog = ErrorData(
                            title = restoreFailedTitle,
                            message = effect.message
                        )
                    }
                    is BackupRestoreEffect.ShowInvalidFile -> {
                        showErrorDialog = ErrorData(
                            title = invalidFileTitle,
                            message = effect.message ?: invalidFileMessage
                        )
                    }
                    is BackupRestoreEffect.ShowFileError -> {
                        showErrorDialog = ErrorData(
                            title = fileErrorTitle,
                            message = effect.message ?: fileErrorMessage
                        )
                    }
                    is BackupRestoreEffect.ShowNoFileSelected -> {
                        showErrorDialog = ErrorData(
                            title = noFileSelectedTitle,
                            message = noFileSelectedMessage
                        )
                    }
                    is BackupRestoreEffect.ShowDeleteFailed -> {
                        showErrorDialog = ErrorData(
                            title = deleteFailedTitle,
                            message = effect.message ?: deleteFailedMessage
                        )
                    }
                    is BackupRestoreEffect.ShowLoadFailed -> {
                        showErrorDialog = ErrorData(
                            title = loadFailedTitle,
                            message = effect.message ?: loadFailedMessage
                        )
                    }
                    is BackupRestoreEffect.ShowBackupDirectorySet -> {
                        // TODO: 显示 Snackbar
                    }
                    is BackupRestoreEffect.ShowBackupDeleted -> {
                        // TODO: 显示 Snackbar
                    }
                    is BackupRestoreEffect.ShowMessage -> {
                        // TODO: 显示 Snackbar
                    }
                    is BackupRestoreEffect.RestartApp -> {
                        // 重启标志已在ShowRestoreComplete中设置
                        // 实际重启将在用户关闭成功对话框后执行
                    }
                }
            }
        }
        
        BackupRestoreContent(
            state = state,
            onIntent = viewModel::handleIntent,
            onNavigateBack = { navigator.pop() }
        )
        
        // 成功对话框
        showSuccessDialog?.let { data ->
            BackupSuccessDialog(
                title = data.title,
                message = data.message,
                onDismiss = {
                    showSuccessDialog = null
                    // 如果需要重启，在对话框关闭后执行
                    if (data.needsRestart) {
                        com.projectu.ui.util.AppRestarter.restartApp(1000)
                    }
                }
            )
        }
        
        // 错误对话框
        showErrorDialog?.let { data ->
            ErrorDialog(
                title = data.title,
                message = data.message,
                onDismiss = { showErrorDialog = null }
            )
        }
    }
}

data class BackupSuccessData(
    val title: String,
    val message: String,
    val needsRestart: Boolean = false
)

data class ErrorData(
    val title: String,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupRestoreContent(
    state: BackupRestoreState,
    onIntent: (BackupRestoreIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    // 进入页面时刷新备份列表
    LaunchedEffect(Unit) {
        onIntent(BackupRestoreIntent.RefreshBackupList)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        BackupRestoreMainContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
    
    // 进度对话框
    if (state.isCreatingBackup && state.currentProgress != null) {
        BackupProgressDialog(
            progress = state.currentProgress,
            onDismiss = { onIntent(BackupRestoreIntent.DismissDialog) }
        )
    }
    
    if (state.isRestoringBackup && state.currentRestoreProgress != null) {
        RestoreProgressDialog(
            progress = state.currentRestoreProgress,
            onDismiss = { onIntent(BackupRestoreIntent.DismissDialog) }
        )
    }
}

@Composable
private fun BackupRestoreMainContent(
    state: BackupRestoreState,
    onIntent: (BackupRestoreIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var comment by remember { mutableStateOf("") }
    var selectedModules by remember { 
        mutableStateOf(setOf(BackupModule.SETTINGS, BackupModule.CREDENTIALS))
    }
    var showDeleteConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf<BackupInfo?>(null) }
    var showRestoreFromFileDialog by remember { mutableStateOf(false) }
    var showDirectoryGuideDialog by remember { mutableStateOf(false) }
    
    // 使用现有的PathPicker和FilePicker
    val pathPicker = rememberPathPicker()
    val filePicker = rememberFilePicker(
        mimeTypes = arrayOf("application/octet-stream", "*/*")
    ) { selectedPath ->
        selectedPath?.let {
            onIntent(BackupRestoreIntent.SelectBackupFile(it))
            showRestoreFromFileDialog = true
        }
    }
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Android 10+: 备份目录选择提示（卡片式状态指示器）
        if (needsSafAuthorization()) {
            item {
                if (state.hasBackupDirectoryAccess) {
                    // 已设置状态：显示目录信息和统计
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = stringResource(Res.string.backup_directory_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                // 显示目录路径（使用PathDisplay组件）
                                state.backupDirectoryPath?.let { path ->
                                    PathDisplay(
                                        path = path,
                                        modifier = Modifier.fillMaxWidth(),
                                        allowCopy = true,
                                        allowExpand = true,
                                        showIcon = true
                                    )
                                }
                                
                                // 显示扫描统计
                                Text(
                                    text = if (state.isLoadingBackups) {
                                        stringResource(Res.string.backup_loading)
                                    } else {
                                        stringResource(Res.string.backup_scanned_count, state.backupHistory.size)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 设置按钮
                            IconButton(
                                onClick = { showDirectoryGuideDialog = true }
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                    contentDescription = "重新设置备份目录",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // 未设置状态：紧凑的提示卡片
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ℹ️",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(Res.string.backup_directory_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            TextButton(
                                onClick = { showDirectoryGuideDialog = true }
                            ) {
                                Text(stringResource(Res.string.backup_guide_start))
                            }
                        }
                    }
                }
            }
        }
        
        // 创建备份卡片
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.backup_create_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // 备注输入
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(stringResource(Res.string.backup_comment_label)) },
                        placeholder = { Text(stringResource(Res.string.backup_comment_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // 模块选择
                    Text(
                        text = stringResource(Res.string.backup_modules_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedModules.contains(BackupModule.SETTINGS),
                            onClick = {
                                selectedModules = if (selectedModules.contains(BackupModule.SETTINGS)) {
                                    selectedModules - BackupModule.SETTINGS
                                } else {
                                    selectedModules + BackupModule.SETTINGS
                                }
                            },
                            label = { Text(stringResource(Res.string.backup_module_settings)) }
                        )
                        
                        FilterChip(
                            selected = selectedModules.contains(BackupModule.CREDENTIALS),
                            onClick = {
                                selectedModules = if (selectedModules.contains(BackupModule.CREDENTIALS)) {
                                    selectedModules - BackupModule.CREDENTIALS
                                } else {
                                    selectedModules + BackupModule.CREDENTIALS
                                }
                            },
                            label = { Text(stringResource(Res.string.backup_module_credentials)) }
                        )
                    }
                    
                    // 创建备份按钮
                    Button(
                        onClick = {
                            if (selectedModules.isNotEmpty()) {
                                val config = BackupConfig(
                                    modules = selectedModules,
                                    comment = comment.takeIf { it.isNotBlank() }
                                )
                                onIntent(BackupRestoreIntent.CreateBackup(config))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedModules.isNotEmpty() && !state.isCreatingBackup
                    ) {
                        if (state.isCreatingBackup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(Res.string.backup_button))
                    }
                }
            }
        }
        
        // 备份历史标题和从文件恢复按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.backup_history_title),
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedButton(
                    onClick = { filePicker() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.restore_from_file),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        // 备份历史列表
        if (state.isLoadingBackups) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.backupHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.backup_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.backupHistory) { backupInfo ->
                BackupHistoryItem(
                    backupInfo = backupInfo,
                    onClick = { 
                        // 点击后弹出恢复确认对话框
                        showRestoreConfirmDialog = backupInfo
                    },
                    onDelete = { showDeleteConfirmDialog = backupInfo }
                )
            }
        }
    }
    
    // 删除确认对话框
    showDeleteConfirmDialog?.let { backupInfo ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(stringResource(Res.string.backup_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.backup_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(BackupRestoreIntent.DeleteBackup(backupInfo))
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    
    // 恢复确认对话框（从备份历史）
    showRestoreConfirmDialog?.let { backupInfo ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text(stringResource(Res.string.restore_warning_title)) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 警告信息
                    Text(
                        text = stringResource(Res.string.restore_warning_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    // 备份信息
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // 时间
                        Text(
                            text = stringResource(Res.string.backup_time_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateTimeFormatter.formatTimestamp(backupInfo.metadata.timestamp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        // 备注
                        backupInfo.metadata.comment?.let { comment ->
                            Text(
                                text = stringResource(Res.string.backup_comment_label_short),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // 文件位置
                        Text(
                            text = stringResource(Res.string.backup_file_location),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PathDisplay(
                            path = backupInfo.filePath,
                            modifier = Modifier.fillMaxWidth(),
                            allowCopy = true,
                            allowExpand = true
                        )
                        
                        // 包含内容
                        Text(
                            text = stringResource(Res.string.backup_content_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val localizedModules = backupInfo.metadata.modules.map { moduleName ->
                            when (moduleName) {
                                "SETTINGS" -> stringResource(Res.string.backup_module_settings)
                                "CREDENTIALS" -> stringResource(Res.string.backup_module_credentials)
                                else -> moduleName
                            }
                        }.joinToString(", ")
                        Text(
                            text = localizedModules,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 选择备份文件并触发恢复流程
                        onIntent(BackupRestoreIntent.SelectBackupFile(backupInfo.filePath))
                        onIntent(BackupRestoreIntent.RestoreBackup)
                        showRestoreConfirmDialog = null
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.restore_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    
    // 恢复确认对话框（从文件选择）
    if (showRestoreFromFileDialog && state.selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showRestoreFromFileDialog = false },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text(stringResource(Res.string.restore_warning_title)) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(Res.string.restore_warning_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    Text(
                        text = stringResource(Res.string.backup_file_location),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PathDisplay(
                        path = state.selectedBackupFile,
                        modifier = Modifier.fillMaxWidth(),
                        allowCopy = true,
                        allowExpand = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(BackupRestoreIntent.RestoreBackup)
                        showRestoreFromFileDialog = false
                    },
                    enabled = !state.isRestoringBackup
                ) {
                    Text(
                        text = stringResource(Res.string.restore_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreFromFileDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    
    // 备份目录选择引导对话框
    if (showDirectoryGuideDialog) {
        DirectoryGuideDialog(
            onConfirm = {
                showDirectoryGuideDialog = false
                pathPicker.pickDirectory(
                    title = "选择备份目录",
                    initialPath = "/storage/emulated/0/Download/ProjectU/Backups",
                    onPathSelected = { uri ->
                        uri?.let {
                            onIntent(BackupRestoreIntent.SetBackupDirectory(it))
                        }
                    }
                )
            },
            onDismiss = { showDirectoryGuideDialog = false }
        )
    }
}

/**
 * 备份目录选择引导对话框
 */
@Composable
private fun DirectoryGuideDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val recommendedPath = "/storage/emulated/0/Download/ProjectU/Backups"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.backup_guide_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.backup_guide_recommend),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = recommendedPath,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { 
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(recommendedPath))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                contentDescription = "复制路径",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = stringResource(Res.string.backup_guide_navigation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(Res.string.backup_guide_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

@Composable
private fun BackupHistoryItem(
    backupInfo: BackupInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        val localizedModules = backupInfo.metadata.modules.map { moduleName ->
            when (moduleName) {
                "SETTINGS" -> stringResource(Res.string.backup_module_settings)
                "CREDENTIALS" -> stringResource(Res.string.backup_module_credentials)
                else -> moduleName
            }
        }.joinToString(", ")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = DateTimeFormatter.formatTimestamp(backupInfo.metadata.timestamp),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(
                        Res.string.backup_item_modules,
                        localizedModules
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        Res.string.backup_item_size,
                        formatFileSize(backupInfo.fileSize)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                backupInfo.metadata.comment?.let { comment ->
                    Text(
                        text = stringResource(Res.string.backup_item_comment, comment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.common_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BackupProgressDialog(
    progress: BackupResult.Progress,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.backup_progress_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = progress.message)
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun RestoreProgressDialog(
    progress: RestoreResult.Progress,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.restore_progress_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = progress.message)
            }
        },
        confirmButton = {}
    )
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

/**
 * 备份成功对话框
 */
@Composable
private fun BackupSuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    // 尝试从消息中提取文件路径
    val filePath = message.substringAfter(":\n", "").takeIf { it.isNotEmpty() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(Res.string.backup_success_created))
                
                if (filePath != null && filePath.isNotEmpty()) {
                    HorizontalDivider()
                    
                    Text(
                        text = stringResource(Res.string.backup_success_location),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    PathDisplay(
                        path = filePath,
                        modifier = Modifier.fillMaxWidth(),
                        allowCopy = true,
                        allowExpand = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.common_done))
            }
        }
    )
}

/**
 * 错误对话框
 */
@Composable
private fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.common_done))
            }
        }
    )
}
