package com.projectu.ui.screens.blocklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import com.projectu.shared.domain.model.ContentScope
import com.projectu.shared.domain.model.TagMatchMode
import com.projectu.ui.screens.search.SearchResultScreen
import com.projectu.ui.screens.user.UserScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 屏蔽列表页面
 * 
 * @param prefilledTag 预填充的Tag名称，如果不为空则自动显示添加Tag对话框
 * @param prefilledAuthorId 预填充的作者ID，如果不为空则自动显示添加作者对话框
 */
class BlockListScreen(
    private val prefilledTag: String? = null,
    private val prefilledAuthorId: String? = null
) : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: BlockListViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()
        val allRules by viewModel.allRules.collectAsState()
        
        // 处理预填充参数
        LaunchedEffect(prefilledTag, prefilledAuthorId) {
            when {
                prefilledTag != null -> {
                    // 预填充Tag，显示添加Tag对话框
                    viewModel.showAddTagDialog()
                    viewModel.updateTagInput(prefilledTag)
                }
                prefilledAuthorId != null -> {
                    // 预填充作者ID，显示添加作者对话框
                    viewModel.showAddAuthorDialog()
                    viewModel.updateAuthorIdInput(prefilledAuthorId)
                }
            }
        }
        
        BlockListScreenContent(
            allRules = allRules,
            uiState = uiState,
            onBack = { navigator.pop() },
            onToggleRule = viewModel::toggleRuleEnabled,
            onDeleteRule = viewModel::showDeleteConfirmDialog,
            onEditRule = viewModel::showEditDialog,
            onAddAuthor = viewModel::showAddAuthorDialog,
            onAddTag = viewModel::showAddTagDialog,
            onRuleClick = { rule ->
                when (rule.type) {
                    BlockRuleType.AUTHOR_ID -> {
                        // 跳转到用户页面
                        navigator.push(UserScreen(rule.value))
                    }
                    BlockRuleType.TAG -> {
                        // 跳转到Tag搜索结果页面
                        navigator.push(SearchResultScreen(initialKeyword = rule.value))
                    }
                    else -> { /* 固定规则不需要跳转 */ }
                }
            }
        )
        
        // 添加作者对话框
        if (uiState.showAddAuthorDialog) {
            AddAuthorDialog(
                authorId = uiState.authorIdInput,
                selectedScopes = uiState.selectedScopes,
                error = uiState.inputError,
                onAuthorIdChange = viewModel::updateAuthorIdInput,
                onToggleScope = viewModel::toggleScope,
                onConfirm = viewModel::addAuthorRule,
                onDismiss = viewModel::hideAddAuthorDialog
            )
        }
        
        // 添加标签对话框
        if (uiState.showAddTagDialog) {
            AddTagDialog(
                tag = uiState.tagInput,
                selectedScopes = uiState.selectedScopes,
                selectedMatchMode = uiState.selectedMatchMode,
                error = uiState.inputError,
                onTagChange = viewModel::updateTagInput,
                onToggleScope = viewModel::toggleScope,
                onToggleMatchMode = viewModel::toggleMatchMode,
                onConfirm = viewModel::addTagRule,
                onDismiss = viewModel::hideAddTagDialog
            )
        }
        
        // 编辑规则对话框
        if (uiState.showEditDialog && uiState.editingRule != null) {
            EditRuleDialog(
                rule = uiState.editingRule!!,
                selectedScopes = uiState.selectedScopes,
                selectedMatchMode = uiState.selectedMatchMode,
                error = uiState.inputError,
                onToggleScope = viewModel::toggleScope,
                onToggleMatchMode = viewModel::toggleMatchMode,
                onConfirm = viewModel::saveEditedRule,
                onDismiss = viewModel::hideEditDialog
            )
        }
        
        // 删除确认对话框
        if (uiState.showDeleteConfirmDialog && uiState.ruleToDelete != null) {
            DeleteConfirmDialog(
                rule = uiState.ruleToDelete!!,
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::hideDeleteConfirmDialog
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockListScreenContent(
    allRules: List<BlockRule>,
    uiState: BlockListUiState,
    onBack: () -> Unit,
    onToggleRule: (BlockRule) -> Unit,
    onDeleteRule: (BlockRule) -> Unit,
    onEditRule: (BlockRule) -> Unit,
    onAddAuthor: () -> Unit,
    onAddTag: () -> Unit,
    onRuleClick: ((BlockRule) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.block_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            var showMenu by remember { mutableStateOf(false) }
            
            Box {
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.common_add_rule))
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.block_rule_type_author)) },
                        onClick = {
                            showMenu = false
                            onAddAuthor()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.block_rule_type_tag)) },
                        onClick = {
                            showMenu = false
                            onAddTag()
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (allRules.isEmpty()) {
            // 空状态
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            // 规则列表
            RuleList(
                rules = allRules,
                onToggleRule = onToggleRule,
                onDeleteRule = onDeleteRule,
                onEditRule = onEditRule,
                onRuleClick = onRuleClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.block_list_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.block_list_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RuleList(
    rules: List<BlockRule>,
    onToggleRule: (BlockRule) -> Unit,
    onDeleteRule: (BlockRule) -> Unit,
    onEditRule: (BlockRule) -> Unit,
    onRuleClick: ((BlockRule) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 分组：固定规则和自定义规则
    val fixedRules = rules.filter { it.type.isFixed }
    val customRules = rules.filter { !it.type.isFixed }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 固定规则区域
        if (fixedRules.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.block_list_fixed_rules),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(fixedRules) { rule ->
                FixedRuleItem(
                    rule = rule,
                    onToggle = { onToggleRule(rule) },
                    onEdit = { onEditRule(rule) }
                )
            }
        }
        
        // 自定义规则区域
        if (customRules.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.block_list_custom_rules),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(customRules) { rule ->
                CustomRuleItem(
                    rule = rule,
                    onToggle = { onToggleRule(rule) },
                    onDelete = { onDeleteRule(rule) },
                    onEdit = { onEditRule(rule) },
                    onClick = { onRuleClick?.invoke(rule) }
                )
            }
        }
    }
}

@Composable
private fun FixedRuleItem(
    rule: BlockRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (rule.type) {
                        BlockRuleType.R18_CONTENT -> stringResource(Res.string.block_rule_r18_content)
                        BlockRuleType.AI_GENERATED -> stringResource(Res.string.block_rule_ai_generated)
                        else -> rule.value
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                // 显示适用范围
                ScopeDisplay(scopes = rule.scopes)
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(Res.string.common_edit))
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

@Composable
private fun CustomRuleItem(
    rule: BlockRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (rule.type) {
                        BlockRuleType.AUTHOR_ID -> stringResource(Res.string.block_rule_author_id, rule.value)
                        BlockRuleType.TAG -> stringResource(Res.string.block_rule_tag, rule.value)
                        else -> rule.value
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                // 显示适用范围
                ScopeDisplay(scopes = rule.scopes)
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(Res.string.common_edit))
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.common_delete))
                }
            }
        }
    }
}

@Composable
private fun AddAuthorDialog(
    authorId: String,
    selectedScopes: Set<ContentScope>,
    error: InputError?,
    onAuthorIdChange: (String) -> Unit,
    onToggleScope: (ContentScope) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.block_rule_add_author)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = authorId,
                    onValueChange = onAuthorIdChange,
                    label = { Text(stringResource(Res.string.block_rule_author_id_label)) },
                    placeholder = { Text(stringResource(Res.string.block_rule_author_id_hint)) },
                    isError = error == InputError.EMPTY || error == InputError.ALREADY_EXISTS,
                    supportingText = {
                        if (error == InputError.EMPTY) {
                            Text(
                                text = stringResource(Res.string.block_rule_input_empty_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (error == InputError.ALREADY_EXISTS) {
                            Text(
                                text = stringResource(Res.string.block_rule_already_exists_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 适用范围选择器
                ScopeSelector(
                    selectedScopes = selectedScopes,
                    onToggleScope = onToggleScope,
                    error = error == InputError.EMPTY_SCOPE
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.common_confirm))
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
private fun AddTagDialog(
    tag: String,
    selectedScopes: Set<ContentScope>,
    selectedMatchMode: TagMatchMode,
    error: InputError?,
    onTagChange: (String) -> Unit,
    onToggleScope: (ContentScope) -> Unit,
    onToggleMatchMode: (TagMatchMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.block_rule_add_tag)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = onTagChange,
                    label = { Text(stringResource(Res.string.block_rule_tag_label)) },
                    placeholder = { Text(stringResource(Res.string.block_rule_tag_hint)) },
                    isError = error == InputError.EMPTY || error == InputError.ALREADY_EXISTS,
                    supportingText = {
                        if (error == InputError.EMPTY) {
                            Text(
                                text = stringResource(Res.string.block_rule_input_empty_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (error == InputError.ALREADY_EXISTS) {
                            Text(
                                text = stringResource(Res.string.block_rule_already_exists_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 匹配模式选择器
                MatchModeSelector(
                    selectedMode = selectedMatchMode,
                    onSelect = onToggleMatchMode
                )
                
                // 适用范围选择器
                ScopeSelector(
                    selectedScopes = selectedScopes,
                    onToggleScope = onToggleScope,
                    error = error == InputError.EMPTY_SCOPE
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 适用范围选择器组件（使用 FilterChip）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeSelector(
    selectedScopes: Set<ContentScope>,
    onToggleScope: (ContentScope) -> Unit,
    error: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.block_rule_scope_label),
            style = MaterialTheme.typography.labelMedium
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContentScope.entries.forEach { scope ->
                FilterChip(
                    selected = scope in selectedScopes,
                    onClick = { onToggleScope(scope) },
                    label = {
                        Text(
                            when (scope) {
                                ContentScope.ILLUST -> stringResource(Res.string.block_rule_scope_illust)
                                ContentScope.MANGA -> stringResource(Res.string.block_rule_scope_manga)
                                ContentScope.MANGA_SERIES -> stringResource(Res.string.block_rule_scope_manga_series)
                                ContentScope.UGOIRA -> stringResource(Res.string.block_rule_scope_ugoira)
                                ContentScope.NOVEL -> stringResource(Res.string.block_rule_scope_novel)
                                ContentScope.NOVEL_SERIES -> stringResource(Res.string.block_rule_scope_novel_series)
                            }
                        )
                    }
                )
            }
        }
        
        if (error) {
            Text(
                text = stringResource(Res.string.block_rule_scope_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 适用范围显示组件
 */
@Composable
private fun ScopeDisplay(scopes: Set<ContentScope>) {
    val scopeLabels = scopes.sorted().map { scope ->
        when (scope) {
            ContentScope.ILLUST -> stringResource(Res.string.block_rule_scope_illust)
            ContentScope.MANGA -> stringResource(Res.string.block_rule_scope_manga)
            ContentScope.MANGA_SERIES -> stringResource(Res.string.block_rule_scope_manga_series)
            ContentScope.UGOIRA -> stringResource(Res.string.block_rule_scope_ugoira)
            ContentScope.NOVEL -> stringResource(Res.string.block_rule_scope_novel)
            ContentScope.NOVEL_SERIES -> stringResource(Res.string.block_rule_scope_novel_series)
        }
    }.joinToString(", ")
    
    Text(
        text = scopeLabels,
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * Tag 匹配模式选择器
 */
@Composable
private fun MatchModeSelector(
    selectedMode: TagMatchMode,
    onSelect: (TagMatchMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.block_rule_match_mode_label),
            style = MaterialTheme.typography.labelMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TagMatchMode.entries.forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onSelect(mode) },
                    label = {
                        Text(
                            when (mode) {
                                TagMatchMode.EXACT -> stringResource(Res.string.block_rule_match_mode_exact)
                                TagMatchMode.REGEX -> stringResource(Res.string.block_rule_match_mode_regex)
                            }
                        )
                    }
                )
            }
        }
        
        // 提示文本
        Text(
            text = when (selectedMode) {
                TagMatchMode.EXACT -> stringResource(Res.string.block_rule_match_mode_exact_hint)
                TagMatchMode.REGEX -> stringResource(Res.string.block_rule_match_mode_regex_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 编辑规则对话框
 */
@Composable
private fun EditRuleDialog(
    rule: BlockRule,
    selectedScopes: Set<ContentScope>,
    selectedMatchMode: TagMatchMode,
    error: InputError?,
    onToggleScope: (ContentScope) -> Unit,
    onToggleMatchMode: (TagMatchMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when (rule.type) {
                    BlockRuleType.R18_CONTENT -> stringResource(Res.string.block_rule_r18_content)
                    BlockRuleType.AI_GENERATED -> stringResource(Res.string.block_rule_ai_generated)
                    BlockRuleType.AUTHOR_ID -> stringResource(Res.string.block_rule_author_id, rule.value)
                    BlockRuleType.TAG -> stringResource(Res.string.block_rule_tag, rule.value)
                }
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tag 规则显示匹配模式选择器
                if (rule.type == BlockRuleType.TAG) {
                    MatchModeSelector(
                        selectedMode = selectedMatchMode,
                        onSelect = onToggleMatchMode
                    )
                }
                
                // 适用范围选择器
                ScopeSelector(
                    selectedScopes = selectedScopes,
                    onToggleScope = onToggleScope,
                    error = error == InputError.EMPTY_SCOPE
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.common_confirm))
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
private fun DeleteConfirmDialog(
    rule: BlockRule,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.block_rule_delete_confirm_title)) },
        text = { Text(stringResource(Res.string.block_rule_delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(Res.string.common_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}
