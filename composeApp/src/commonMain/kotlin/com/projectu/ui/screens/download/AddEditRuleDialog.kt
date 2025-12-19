package com.projectu.ui.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.AuthorGrouping
import com.projectu.shared.domain.model.DownloadRule
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.ResourceType
import com.projectu.ui.util.rememberPathPicker
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 添加/编辑规则对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    existingRule: DownloadRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        resourceTypes: Set<ResourceType>,
        r18Filter: FilterType,
        aiFilter: FilterType,
        authorGrouping: AuthorGrouping,
        targetPath: String
    ) -> Unit
) {
    var resourceTypes by remember {
        mutableStateOf(existingRule?.resourceTypes ?: emptySet())
    }
    var r18Filter by remember {
        mutableStateOf(existingRule?.r18Filter ?: FilterType.ANY)
    }
    var aiFilter by remember {
        mutableStateOf(existingRule?.aiFilter ?: FilterType.ANY)
    }
    var authorGrouping by remember {
        mutableStateOf(existingRule?.authorGrouping ?: AuthorGrouping.NONE)
    }
    var targetPath by remember {
        mutableStateOf(existingRule?.targetPath ?: "")
    }
    
    val pathPicker = rememberPathPicker()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRule == null) stringResource(Res.string.add_rule_title) else stringResource(Res.string.edit_rule_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 资源类型选择（多选）
                Text(
                    text = stringResource(Res.string.rule_resource_types_hint),
                    style = MaterialTheme.typography.labelMedium
                )
                ResourceTypeSelector(
                    selectedTypes = resourceTypes,
                    onSelectionChange = { resourceTypes = it }
                )
                
                // R-18 过滤
                Text(
                    text = stringResource(Res.string.rule_r18_filter),
                    style = MaterialTheme.typography.labelMedium
                )
                FilterTypeSelector(
                    selected = r18Filter,
                    onSelect = { r18Filter = it }
                )
                
                // AI 生成过滤
                Text(
                    text = stringResource(Res.string.rule_ai_filter),
                    style = MaterialTheme.typography.labelMedium
                )
                FilterTypeSelector(
                    selected = aiFilter,
                    onSelect = { aiFilter = it }
                )
                
                // 作者分组
                Text(
                    text = stringResource(Res.string.rule_author_grouping),
                    style = MaterialTheme.typography.labelMedium
                )
                AuthorGroupingSelector(
                    selected = authorGrouping,
                    onSelect = { authorGrouping = it }
                )
                
                // 目标路径
                Text(
                    text = stringResource(Res.string.rule_save_path),
                    style = MaterialTheme.typography.labelMedium
                )
                val selectDirTitle = stringResource(Res.string.select_download_directory)
                OutlinedButton(
                    onClick = {
                        pathPicker.pickDirectory(
                            title = selectDirTitle,
                            initialPath = targetPath.ifEmpty { null }
                        ) { selectedPath ->
                            selectedPath?.let { targetPath = it }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = targetPath.ifEmpty { stringResource(Res.string.rule_select_path) },
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (targetPath.isNotBlank()) {
                        onConfirm(resourceTypes, r18Filter, aiFilter, authorGrouping, targetPath)
                    }
                },
                enabled = targetPath.isNotBlank()
            ) {
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
 * 资源类型选择器（多选）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceTypeSelector(
    selectedTypes: Set<ResourceType>,
    onSelectionChange: (Set<ResourceType>) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResourceType.entries.forEach { type ->
            FilterChip(
                selected = type in selectedTypes,
                onClick = {
                    val newSelection = if (type in selectedTypes) {
                        selectedTypes - type
                    } else {
                        selectedTypes + type
                    }
                    onSelectionChange(newSelection)
                },
                label = {
                    Text(
                        when (type) {
                            ResourceType.ILLUSTRATION -> stringResource(Res.string.resource_type_illustration)
                            ResourceType.MANGA -> stringResource(Res.string.resource_type_manga)
                            ResourceType.UGOIRA -> stringResource(Res.string.resource_type_ugoira)
                            ResourceType.NOVEL -> stringResource(Res.string.resource_type_novel)
                            ResourceType.NOVEL_SERIES -> stringResource(Res.string.resource_type_novel_series)
                        }
                    )
                }
            )
        }
    }
}

/**
 * 过滤器类型选择器
 */
@Composable
private fun FilterTypeSelector(
    selected: FilterType,
    onSelect: (FilterType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        when (type) {
                            FilterType.MUST_BE -> stringResource(Res.string.rule_filter_must_be)
                            FilterType.MUST_NOT_BE -> stringResource(Res.string.rule_filter_must_not_be)
                            FilterType.ANY -> stringResource(Res.string.rule_filter_any)
                        }
                    )
                }
            )
        }
    }
}

/**
 * 作者分组选择器
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthorGroupingSelector(
    selected: AuthorGrouping,
    onSelect: (AuthorGrouping) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuthorGrouping.entries.forEach { grouping ->
            FilterChip(
                selected = selected == grouping,
                onClick = { onSelect(grouping) },
                label = {
                    Text(
                        when (grouping) {
                            AuthorGrouping.BY_ID -> stringResource(Res.string.rule_grouping_by_id)
                            AuthorGrouping.BY_NAME -> stringResource(Res.string.rule_grouping_by_name)
                            AuthorGrouping.NONE -> stringResource(Res.string.rule_grouping_none)
                        }
                    )
                }
            )
        }
    }
}
