package com.projectu.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File

/**
 * Desktop 平台的图片加载器实现
 */
actual object PlatformImageLoader {
    
    /**
     * 从字节数组加载 ImageBitmap
     */
    actual fun loadImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从文件路径加载 ImageBitmap
     */
    actual fun loadImageBitmapFromFile(filePath: String): ImageBitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            val bytes = file.readBytes()
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
