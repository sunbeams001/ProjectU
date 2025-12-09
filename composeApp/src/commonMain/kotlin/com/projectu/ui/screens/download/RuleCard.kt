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
                    text = "规则 #${rule.order + 1}",
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
                RuleConditionText("资源类型", getResourceTypesLabel(rule.resourceTypes))
                RuleConditionText("R-18", getFilterTypeLabel(rule.r18Filter))
                RuleConditionText("AI 生成", getFilterTypeLabel(rule.aiFilter))
                RuleConditionText("作者分组", getAuthorGroupingLabel(rule.authorGrouping))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 目标路径
            Text(
                text = "保存路径",
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
                    Text("删除")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除规则") },
            text = { Text("确定要删除这条规则吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
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
 * 获取资源类型标签（多选）
 */
private fun getResourceTypesLabel(types: Set<ResourceType>): String {
    if (types.isEmpty()) return "任意"
    
    return types.joinToString(", ") { type ->
        when (type) {
            ResourceType.ILLUSTRATION -> "插画"
            ResourceType.MANGA -> "漫画"
            ResourceType.UGOIRA -> "动图"
            ResourceType.NOVEL -> "小说"
            ResourceType.NOVEL_SERIES -> "小说系列"
        }
    }
}

/**
 * 获取过滤器类型标签
 */
private fun getFilterTypeLabel(filter: FilterType): String {
    return when (filter) {
        FilterType.MUST_BE -> "必须是"
        FilterType.MUST_NOT_BE -> "必须不是"
        FilterType.ANY -> "任意"
    }
}

/**
 * 获取作者分组标签
 */
private fun getAuthorGroupingLabel(grouping: AuthorGrouping): String {
    return when (grouping) {
        AuthorGrouping.BY_ID -> "按作者ID"
        AuthorGrouping.BY_NAME -> "按作者名"
        AuthorGrouping.NONE -> "不分组"
    }
}
