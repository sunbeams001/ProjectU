package com.projectu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 颜色选择器对话框
 * 
 * @param initialColor 初始颜色（十六进制格式，如 "#FFFFFF"），null表示使用主题默认
 * @param onColorSelected 颜色选择回调，参数为十六进制颜色字符串，null表示使用主题默认
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun ColorPickerDialog(
    initialColor: String?,
    onColorSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var colorHex by remember { mutableStateOf(initialColor ?: "") }
    var useThemeDefault by remember { mutableStateOf(initialColor == null) }
    
    // 预设颜色列表
    val presetColors = listOf(
        "#000000" to "Black",
        "#FFFFFF" to "White",
        "#333333" to "Dark Gray",
        "#666666" to "Gray",
        "#999999" to "Light Gray",
        "#CCCCCC" to "Very Light Gray",
        "#FF0000" to "Red",
        "#00FF00" to "Green",
        "#0000FF" to "Blue",
        "#FFFF00" to "Yellow",
        "#FF00FF" to "Magenta",
        "#00FFFF" to "Cyan",
        "#FFA500" to "Orange",
        "#800080" to "Purple",
        "#008000" to "Dark Green",
        "#CCE8CC" to "Eye Care Green",
        "#FFF9E6" to "Warm Yellow",
        "#F5E6D3" to "Classic Beige"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(min = 280.dp, max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(
                    text = stringResource(Res.string.color_picker_title),
                    style = MaterialTheme.typography.titleLarge
                )
                
                // 使用主题默认选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { useThemeDefault = !useThemeDefault }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useThemeDefault,
                        onCheckedChange = { useThemeDefault = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.color_picker_use_theme_default),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (!useThemeDefault) {
                    // 颜色输入框
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^#?[0-9A-Fa-f]{0,6}$"))) {
                                colorHex = if (it.startsWith("#")) it else "#$it"
                            }
                        },
                        label = { Text(stringResource(Res.string.color_picker_hex_input)) },
                        placeholder = { Text("#FFFFFF") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            // 颜色预览
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(colorHex) ?: Color.Transparent)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                    )
                    
                    // 预设颜色网格
                    Text(
                        text = stringResource(Res.string.color_picker_presets),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 颜色网格（3列）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.chunked(6).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowColors.forEach { (hex, _) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(parseColor(hex) ?: Color.Gray)
                                            .border(
                                                width = if (colorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                                color = if (colorHex.equals(hex, ignoreCase = true)) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                            .clickable { colorHex = hex }
                                    )
                                }
                                // 填充空位
                                repeat(6 - rowColors.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (useThemeDefault) {
                                onColorSelected(null)
                            } else {
                                val finalColor = if (colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                                    colorHex
                                } else {
                                    null
                                }
                                onColorSelected(finalColor)
                            }
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(Res.string.action_confirm))
                    }
                }
            }
        }
    }
}

/**
 * 解析十六进制颜色字符串为Color对象
 */
private fun parseColor(hex: String): Color? {
    return try {
        if (hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            val colorInt = hex.substring(1).toLong(16).toInt()
            Color(0xFF000000 or colorInt.toLong())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
