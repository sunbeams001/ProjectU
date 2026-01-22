package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame

/**
 * Desktop平台实现
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: Array<String>,
    onFileSelected: (String?) -> Unit
): () -> Unit {
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "选择备份文件", FileDialog.LOAD)
            dialog.file = "*.pbu"
            dialog.isVisible = true
            
            val selectedFile = dialog.file
            val selectedDirectory = dialog.directory
            
            if (selectedFile != null && selectedDirectory != null) {
                val fullPath = java.io.File(selectedDirectory, selectedFile).absolutePath
                onFileSelected(fullPath)
            } else {
                onFileSelected(null)
            }
        }
    }
}
