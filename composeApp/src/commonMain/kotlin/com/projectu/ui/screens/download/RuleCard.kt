package com.projectu.ui.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.AuthorGrouping
import com.projectu.shared.domain.model.DownloadRule
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.ResourceType
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 规则卡片组件
 */
@Composable
fun RuleCard(
    rule: DownloadRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行：优先级 + 启用开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.download_rules_rule_number, rule.order + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 规则条件
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.download_rules_resource_type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ResourceTypesLabel(rule.resourceTypes)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "R-18",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterTypeLabel(rule.r18Filter)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.download_rules_ai_generation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterTypeLabel(rule.aiFilter)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.download_rules_author_grouping),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AuthorGroupingLabel(rule.authorGrouping)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 目标路径
            Text(
                text = stringResource(Res.string.download_rules_save_path),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = rule.targetPath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.common_delete))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.common_edit))
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.download_rules_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.download_rules_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

/**
 * 规则条件显示组件
 */
@Composable
private fun RuleConditionText(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 资源类型标签组件（多选）
 */
@Composable
private fun ResourceTypesLabel(types: Set<ResourceType>) {
    if (types.isEmpty()) {
        Text(
            text = stringResource(Res.string.resource_type_any),
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        Text(
            text = types.joinToString(", ") { type ->
                when (type) {
                    ResourceType.ILLUSTRATION -> "Illustration"
                    ResourceType.MANGA -> "Manga"
                    ResourceType.UGOIRA -> "Ugoira"
                    ResourceType.NOVEL -> "Novel"
                    ResourceType.NOVEL_SERIES -> "Novel Series"
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 过滤器类型标签组件
 */
@Composable
private fun FilterTypeLabel(filter: FilterType) {
    Text(
        text = when (filter) {
            FilterType.MUST_BE -> stringResource(Res.string.rule_filter_must_be)
            FilterType.MUST_NOT_BE -> stringResource(Res.string.rule_filter_must_not_be)
            FilterType.ANY -> stringResource(Res.string.rule_filter_any)
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * 作者分组标签组件
 */
@Composable
private fun AuthorGroupingLabel(grouping: AuthorGrouping) {
    Text(
        text = when (grouping) {
            AuthorGrouping.BY_ID -> stringResource(Res.string.rule_grouping_by_id)
            AuthorGrouping.BY_NAME -> stringResource(Res.string.rule_grouping_by_name)
            AuthorGrouping.NONE -> stringResource(Res.string.rule_grouping_none)
        },
        style = MaterialTheme.typography.bodyMedium
    )
}
