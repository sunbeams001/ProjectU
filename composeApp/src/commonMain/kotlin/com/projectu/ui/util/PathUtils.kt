package com.projectu.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 格式化路径用于显示
 * 保留最后几级目录，前面用...代替
 */
fun formatPathForDisplay(path: String?, maxSegments: Int = 3): String {
    if (path.isNullOrEmpty()) return ""
    
    val segments = path.split("/").filter { it.isNotEmpty() }
    return if (segments.size > maxSegments) {
        ".../" + segments.takeLast(maxSegments).joinToString("/")
    } else {
        path
    }
}

/**
 * 路径显示组件
 * 支持智能截断、点击展开、复制路径
 */
@Composable
fun PathDisplay(
    path: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    allowCopy: Boolean = true,
    allowExpand: Boolean = true,
    showIcon: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val copyToClipboard = rememberCopyToClipboard()
    val displayPath = if (expanded) path else formatPathForDisplay(path, 3)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = displayPath,
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (allowExpand) {
                        Modifier.clickable { expanded = !expanded }
                    } else Modifier
                )
        )
        
        if (allowExpand) {
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (allowCopy) {
            IconButton(
                onClick = { 
                    copyToClipboard(path)
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制路径",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
