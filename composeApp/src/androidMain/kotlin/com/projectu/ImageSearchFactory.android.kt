package com.projectu

import cafe.adriel.voyager.core.screen.Screen
import com.projectu.ui.screens.imagesearch.ImageSearchScreen

/**
 * Android 平台的图片搜索 Screen 创建器
 */
actual fun createImageSearchScreen(imageUri: String): Screen? {
    return ImageSearchScreen(imageUri)
}
