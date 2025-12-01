package com.projectu.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 平台特定的图片加载器
 * 用于将字节数组转换为 ImageBitmap
 */
expect object PlatformImageLoader {
    /**
     * 从字节数组加载 ImageBitmap
     * @param bytes 图片字节数组（支持 PNG、JPEG 等常见格式）
     * @return ImageBitmap 或 null（如果加载失败）
     */
    fun loadImageBitmap(bytes: ByteArray): ImageBitmap?
    
    /**
     * 从文件路径加载 ImageBitmap
     * @param filePath 文件绝对路径
     * @return ImageBitmap 或 null（如果加载失败）
     */
    fun loadImageBitmapFromFile(filePath: String): ImageBitmap?
}
