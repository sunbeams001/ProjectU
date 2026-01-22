package com.projectu.ui.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

/**
 * Android平台的文件选择器
 * 用于选择单个文件
 * 支持设置初始目录（Android 8+）
 */
@Composable
fun rememberAndroidFilePicker(
    mimeTypes: Array<String> = arrayOf("*/*"),
    onFileSelected: (String?) -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // 获取持久化权限
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // 某些情况下无法获取持久化权限
            }
            onFileSelected(uri.toString())
        } else {
            onFileSelected(null)
        }
    }
    
    return remember {
        {
            // Android 8+ 支持设置初始目录
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    // 构建备份目录的Uri
                    val downloadsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download")
                    val backupDirUri = DocumentsContract.buildDocumentUriUsingTree(
                        downloadsUri,
                        DocumentsContract.getTreeDocumentId(downloadsUri) + "/ProjectU/Backups"
                    )
                    
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        // 尝试设置初始目录（可能不被所有文件管理器支持）
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, backupDirUri)
                    }
                    launcher.launch(mimeTypes)
                } catch (e: Exception) {
                    // 如果构建Uri失败，使用默认方式
                    launcher.launch(mimeTypes)
                }
            } else {
                launcher.launch(mimeTypes)
            }
        }
    }
}
