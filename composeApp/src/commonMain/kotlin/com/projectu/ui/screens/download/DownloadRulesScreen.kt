package com.projectu.ui.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.DownloadRule
import org.koin.compose.viewmodel.koinViewModel

/**
 * 下载规则管理界面
 */
class DownloadRulesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        DownloadRulesContent(
            onBack = { navigator.pop() }
        )
    }
}

/**
 * 下载规则管理界面内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRulesContent(
    onBack: () -> Unit,
    viewModel: DownloadRulesViewModel = koinViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<DownloadRule?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载路径规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加规则")
                    }
                }
            )
        },
        snackbarHost = {
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (rules.isEmpty()) {
                // 空状态提示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "暂无自定义规则",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "将使用默认规则（按资源类型分类）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加规则")
                    }
                }
            } else {
                // 规则列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            onEdit = { editingRule = rule },
                            onDelete = { viewModel.deleteRule(rule.id) },
                            onToggleEnabled = { enabled ->
                                viewModel.toggleRuleEnabled(rule.id, enabled)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 添加规则对话框
    if (showAddDialog) {
        AddEditRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { resourceTypes, r18, ai, authorGrouping, targetPath ->
                viewModel.addRule(resourceTypes, r18, ai, authorGrouping, targetPath)
                showAddDialog = false
            }
        )
    }
    
    // 编辑规则对话框
    editingRule?.let { rule ->
        AddEditRuleDialog(
            existingRule = rule,
            onDismiss = { editingRule = null },
            onConfirm = { resourceTypes, r18, ai, authorGrouping, targetPath ->
                viewModel.updateRule(
                    rule.copy(
                        resourceTypes = resourceTypes,
                        r18Filter = r18,
                        aiFilter = ai,
                        authorGrouping = authorGrouping,
                        targetPath = targetPath
                    )
                )
                editingRule = null
            }
        )
    }
}
