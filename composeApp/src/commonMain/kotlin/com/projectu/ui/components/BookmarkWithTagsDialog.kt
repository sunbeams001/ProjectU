package com.projectu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 按标签收藏对话框
 * Material Design 3 风格
 * 
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认收藏回调，参数为 (标签列表, 是否私人收藏)
 * @param suggestedTags 建议的标签列表（可选）
 * @param initialPrivate 初始是否为私人收藏，默认 false
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkWithTagsDialog(
    onDismiss: () -> Unit,
    onConfirm: (tags: List<String>, isPrivate: Boolean) -> Unit,
    suggestedTags: List<String> = emptyList(),
    initialPrivate: Boolean = false
) {
    var tags by remember { mutableStateOf(emptyList<String>()) }
    var currentTagInput by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(initialPrivate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // 标题
                Text(
                    text = stringResource(Res.string.bookmark_with_tags_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 标签输入框
                OutlinedTextField(
                    value = currentTagInput,
                    onValueChange = { currentTagInput = it },
                    label = { Text(stringResource(Res.string.bookmark_tag_input_label)) },
                    placeholder = { Text(stringResource(Res.string.bookmark_tag_input_placeholder)) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val trimmedTag = currentTagInput.trim()
                                if (trimmedTag.isNotEmpty() && !tags.contains(trimmedTag) && tags.size < 10) {
                                    tags = tags + trimmedTag
                                    currentTagInput = ""
                                }
                            },
                            enabled = currentTagInput.trim().isNotEmpty() && tags.size < 10
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(Res.string.bookmark_add_tag)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmedTag = currentTagInput.trim()
                            if (trimmedTag.isNotEmpty() && !tags.contains(trimmedTag) && tags.size < 10) {
                                tags = tags + trimmedTag
                                currentTagInput = ""
                            }
                        }
                    ),
                    singleLine = true,
                    enabled = tags.size < 10,
                    supportingText = if (tags.size >= 10) {
                        { Text("最多只能添加10个标签", color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 已添加的标签列表
                if (tags.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.bookmark_added_tags),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    tags = tags.filter { it != tag }
                                },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.bookmark_remove_tag),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 建议标签（如果有）
                if (suggestedTags.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.bookmark_suggested_tags),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestedTags
                            .filter { !tags.contains(it) } // 过滤已添加的标签
                            .forEach { tag ->
                                SuggestionChip(
                                    onClick = {
                                        if (!tags.contains(tag) && tags.size < 10) {
                                            tags = tags + tag
                                        }
                                    },
                                    label = { Text(tag) },
                                    enabled = tags.size < 10,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 公开/私人切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.bookmark_set_private),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(tags, isPrivate)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(Res.string.common_confirm))
                    }
                }
            }
        }
    }
}
