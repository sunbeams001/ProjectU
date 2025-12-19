package com.projectu.ui.util

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/**
 * Android 平台的路径选择器 Composable 实现
 * 
 * 兼容性说明：
 * - Android 5.0+ (API 21+): 使用 ACTION_OPEN_DOCUMENT_TREE (推荐)
 * - Android 4.4+ (API 19+): 使用 ACTION_OPEN_DOCUMENT (降级方案)
 * 
 * 本项目 minSdk=24，完全支持 ACTION_OPEN_DOCUMENT_TREE
 */
@Composable
fun rememberAndroidPathPicker(): PathPicker {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentCallback by remember { mutableStateOf<((String?) -> Unit)?>(null) }
    
    /**
     * 目录选择器启动器
     * 使用 rememberLauncherForActivityResult 避免在 RESUMED 状态注册
     */
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // 持久化URI权限
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // 某些情况下可能无法获取持久化权限，继续处理
            }
            
            // 返回URI字符串
            currentCallback?.invoke(uri.toString())
        } else {
            currentCallback?.invoke(null)
        }
        currentCallback = null
    }
    
    return remember {
        object : PathPicker {
            override fun pickDirectory(
                title: String,
                initialPath: String?,
                onPathSelected: (String?) -> Unit
            ) {
                currentCallback = onPathSelected
                
                // 如果有初始路径，尝试转换为URI
                val initialUri = initialPath?.let { path ->
                    try {
                        if (path.startsWith("content://")) {
                            Uri.parse(path)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                // Android的目录选择器不支持自定义标题
                directoryPickerLauncher.launch(initialUri)
            }
        }
    }
}

