package com.projectu.util

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Android 平台的图片加载器实现
 */
actual object PlatformImageLoader {
    
    /**
     * 从字节数组加载 ImageBitmap
     */
    actual fun loadImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap?.asImageBitmap()
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
            
            val bitmap = BitmapFactory.decodeFile(filePath)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
