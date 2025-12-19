package com.projectu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.projectu.shared.data.local.FileNameMode
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 文件命名模板设置组件
 * 
 * @param currentMode 当前文件命名模式
 * @param customTemplate 自定义模板
 * @param previewExamples 预览示例列表
 * @param validationError 验证错误信息
 * @param validationWarning 验证警告信息
 * @param onModeChange 模式改变回调
 * @param onCustomTemplateChange 自定义模板改变回调
 * @param onShowVariableHelp 显示变量帮助回调
 */
@Composable
fun FileNameTemplateSection(
    currentMode: FileNameMode,
    customTemplate: String,
    previewExamples: List<FileNamePreviewExample>,
    validationError: String?,
    validationWarning: String?,
    onModeChange: (FileNameMode) -> Unit,
    onCustomTemplateChange: (String) -> Unit,
    onShowVariableHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.filename_template_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(Res.string.filename_template_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 标准模式
        FileNameModeOption(
            mode = FileNameMode.STANDARD,
            label = stringResource(Res.string.filename_mode_standard),
            example = "123456789_0_Landscape.jpg",
            selected = currentMode == FileNameMode.STANDARD,
            onSelect = { onModeChange(FileNameMode.STANDARD) },
            isRecommended = true
        )
        
        // 自定义模式
        CustomTemplateOption(
            selected = currentMode == FileNameMode.CUSTOM,
            template = customTemplate,
            validationError = validationError,
            validationWarning = validationWarning,
            onSelect = { onModeChange(FileNameMode.CUSTOM) },
            onTemplateChange = onCustomTemplateChange,
            onShowVariableHelp = onShowVariableHelp
        )
        
        // 实时预览
        if (previewExamples.isNotEmpty()) {
            PreviewSection(examples = previewExamples)
        }
    }
}

/**
 * 文件命名模式选项卡
 */
@Composable
private fun FileNameModeOption(
    mode: FileNameMode,
    label: String,
    example: String,
    selected: Boolean,
    onSelect: () -> Unit,
    isRecommended: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    
                    if (isRecommended) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(stringResource(Res.string.recommended), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * 自定义模板选项
 */
@Composable
private fun CustomTemplateOption(
    selected: Boolean,
    template: String,
    validationError: String?,
    validationWarning: String?,
    onSelect: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onShowVariableHelp: () -> Unit
) {
    // 使用 TextFieldValue 跟踪光标位置
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = template, selection = TextRange(template.length)))
    }
    
    // 当外部 template 变化时，只有在文本内容不同时才更新（避免丢失光标位置）
    LaunchedEffect(template) {
        if (textFieldValue.text != template) {
            textFieldValue = TextFieldValue(
                text = template,
                selection = TextRange(template.length)
            )
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = stringResource(Res.string.filename_mode_custom),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // 展开输入框
            AnimatedVisibility(visible = selected) {
                Column(
                    modifier = Modifier.padding(start = 48.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onTemplateChange(newValue.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.filename_template_label)) },
                        placeholder = { Text("{id}_{p}_{title}") },
                        trailingIcon = {
                            IconButton(onClick = onShowVariableHelp) {
                                Icon(Icons.Default.Info, stringResource(Res.string.filename_variable_help))
                            }
                        },
                        isError = validationError != null,
                        supportingText = {
                            when {
                                validationError != null -> Text(
                                    text = validationError,
                                    color = MaterialTheme.colorScheme.error
                                )
                                validationWarning != null -> Text(
                                    text = validationWarning,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        singleLine = true
                    )
                    
                    // 快捷插入变量按钮
                    Text(
                        text = stringResource(Res.string.filename_quick_insert),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    @Composable
                    fun VariableChip(variable: String) {
                        SuggestionChip(
                            onClick = {
                                val currentText = textFieldValue.text
                                val cursorPosition = textFieldValue.selection.start
                                
                                // 在光标位置插入变量
                                val newText = currentText.substring(0, cursorPosition) + 
                                              variable + 
                                              currentText.substring(cursorPosition)
                                
                                // 更新光标位置到插入内容之后
                                val newCursorPosition = cursorPosition + variable.length
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursorPosition)
                                )
                                onTemplateChange(newText)
                            },
                            label = { Text(variable, fontFamily = FontFamily.Monospace) }
                        )
                    }
                    
                    // 第一行：基础变量
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariableChip("{id}")
                        VariableChip("{p}")
                        VariableChip("{title}")
                    }
                    
                    // 第二行：作者相关变量
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariableChip("{author_id}")
                        VariableChip("{author_name}")
                    }
                    
                    // 第三行：标签和日期
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariableChip("{ai}")
                        VariableChip("{r18}")
                        VariableChip("{tags}")
                    }
                    
                    // 第四行：分隔符
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariableChip("_")
                        VariableChip("-")
                        VariableChip(" ")
                    }
                }
            }
        }
    }
}

/**
 * 预览区域
 */
@Composable
private fun PreviewSection(examples: List<FileNamePreviewExample>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(Res.string.filename_preview_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            examples.forEach { example ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${example.type}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = example.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 预览示例数据类
 */
data class FileNamePreviewExample(
    val type: String,
    val fileName: String
)

/**
 * 变量帮助对话框
 */
@Composable
fun VariableHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.filename_variables_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.filename_variables_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                VariableHelpItem("{id}", stringResource(Res.string.filename_var_id), "123456789")
                VariableHelpItem("{p}", stringResource(Res.string.filename_var_page), "0, 1, 2...")
                VariableHelpItem("{title}", stringResource(Res.string.filename_var_title), stringResource(Res.string.filename_var_title_example))
                VariableHelpItem("{author_id}", stringResource(Res.string.filename_var_author_id), "987654321")
                VariableHelpItem("{author_name}", stringResource(Res.string.filename_var_author_name), stringResource(Res.string.filename_var_author_name_example))
                VariableHelpItem("{publish_date}", stringResource(Res.string.filename_var_publish_date), "2025-01-15")
                VariableHelpItem("{download_date}", stringResource(Res.string.filename_var_download_date), "2025-12-18")
                VariableHelpItem("{ai}", stringResource(Res.string.filename_var_ai), stringResource(Res.string.filename_var_ai_example))
                VariableHelpItem("{r18}", stringResource(Res.string.filename_var_r18), stringResource(Res.string.filename_var_r18_example))
                VariableHelpItem("{tags}", stringResource(Res.string.filename_var_tags), stringResource(Res.string.filename_var_tags_example))
                
                Divider()
                
                Text(
                    text = stringResource(Res.string.filename_notes_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = stringResource(Res.string.filename_notes_content),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}

@Composable
private fun VariableHelpItem(
    variable: String,
    description: String,
    example: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = variable,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(100.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(Res.string.filename_example_prefix, example),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
