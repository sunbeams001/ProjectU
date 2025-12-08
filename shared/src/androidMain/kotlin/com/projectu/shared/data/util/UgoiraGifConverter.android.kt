package com.projectu.shared.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android 平台的图片解码实现
 * 使用 BitmapFactory 解码图片
 */
actual suspend fun decodeImageToArgb(imageBytes: ByteArray, delay: Int): UgoiraGifConverter.FrameData = 
    withContext(Dispatchers.IO) {
        // 使用 BitmapFactory 解码图片
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("Failed to decode image")
        
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // 创建 ARGB 数组
            val argb = IntArray(width * height)
            
            // 从 Bitmap 读取像素到 ARGB 数组
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            
            UgoiraGifConverter.FrameData(
                argb = argb,
                width = width,
                height = height,
                delay = delay
            )
        } finally {
            // 释放 Bitmap 内存
            bitmap.recycle()
        }
    }
