package com.projectu.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.ui.components.FileNameTemplateSection
import com.projectu.ui.components.VariableHelpDialog
import com.projectu.ui.screens.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 文件命名规则设置界面
 */
class FileNameRulesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: SettingsViewModel = koinViewModel()
        
        FileNameRulesContent(
            viewModel = viewModel,
            onBack = { navigator.pop() }
        )
    }
}

/**
 * 文件命名规则设置界面内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileNameRulesContent(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val fileNamePreviewExamples by viewModel.fileNamePreviewExamples.collectAsState()
    val templateValidationError by viewModel.templateValidationError.collectAsState()
    val templateValidationWarning by viewModel.templateValidationWarning.collectAsState()
    
    var showVariableHelpDialog by remember { mutableStateOf(false) }
    
    val currentFileNameMode = settings.downloadSettings.fileNameMode
    val currentCustomFileNameTemplate = settings.downloadSettings.customFileNameTemplate
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.filename_rules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.nav_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 说明文本
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.filename_rules_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.filename_rules_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.filename_rules_single_page_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.filename_rules_multi_page_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 文件命名模板设置
            item {
                FileNameTemplateSection(
                    currentMode = currentFileNameMode,
                    customTemplate = currentCustomFileNameTemplate,
                    previewExamples = fileNamePreviewExamples,
                    validationError = templateValidationError,
                    validationWarning = templateValidationWarning,
                    onModeChange = { mode ->
                        viewModel.updateFileNameMode(mode)
                    },
                    onCustomTemplateChange = { template ->
                        viewModel.updateCustomFileNameTemplate(template)
                    },
                    onShowVariableHelp = { showVariableHelpDialog = true }
                )
            }
        }
        
        // 变量帮助对话框
        if (showVariableHelpDialog) {
            VariableHelpDialog(
                onDismiss = { showVariableHelpDialog = false }
            )
        }
    }
}
