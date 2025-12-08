package com.projectu.shared.data.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Desktop (JVM) 平台的图片解码实现
 * 使用 ImageIO 解码图片
 */
actual suspend fun decodeImageToArgb(imageBytes: ByteArray, delay: Int): UgoiraGifConverter.FrameData = 
    withContext(Dispatchers.IO) {
        // 使用 ImageIO 读取图片
        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw IllegalStateException("Failed to decode image")
        
        val width = image.width
        val height = image.height
        
        // 创建 ARGB 数组
        val argb = IntArray(width * height)
        
        // 从 BufferedImage 读取像素到 ARGB 数组
        image.getRGB(0, 0, width, height, argb, 0, width)
        
        UgoiraGifConverter.FrameData(
            argb = argb,
            width = width,
            height = height,
            delay = delay
        )
    }
