package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

/**
 * Desktop 平台的路径选择器实现
 * 使用 AWT FileDialog
 */
class DesktopPathPicker(
    private val parentFrame: Frame? = null
) : PathPicker {
    
    override fun pickDirectory(
        title: String,
        initialPath: String?,
        onPathSelected: (String?) -> Unit
    ) {
        // 在桌面平台，我们使用FileDialog选择目录
        val dialog = FileDialog(parentFrame, title, FileDialog.LOAD)
        
        // 设置为目录选择模式
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        
        // 如果有初始路径，设置为起始目录
        initialPath?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists() && file.isDirectory) {
                    dialog.directory = path
                } else {
                    dialog.directory = file.parent
                }
            } catch (e: Exception) {
                // 忽略无效路径
            }
        }
        
        dialog.isVisible = true
        
        val selectedDirectory = dialog.directory
        val selectedFile = dialog.file
        
        // 恢复默认设置
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
        
        if (selectedDirectory != null && selectedFile != null) {
            val fullPath = java.io.File(selectedDirectory, selectedFile).absolutePath
            onPathSelected(fullPath)
        } else if (selectedDirectory != null) {
            onPathSelected(selectedDirectory)
        } else {
            onPathSelected(null)
        }
    }
}

/**
 * Desktop 平台的 Compose 路径选择器
 */
@Composable
fun rememberDesktopPathPicker(): PathPicker {
    return remember {
        DesktopPathPicker()
    }
}
