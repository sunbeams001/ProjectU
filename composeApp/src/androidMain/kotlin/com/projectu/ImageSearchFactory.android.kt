package com.projectu

import cafe.adriel.voyager.core.screen.Screen
import com.projectu.ui.screens.imagesearch.ImageSearchScreen
import com.projectu.ui.screens.imagesearch.Ascii2dSearchScreen

/**
 * Android 平台的图片搜索 Screen 创建器 (SauceNAO)
 */
actual fun createImageSearchScreen(imageUri: String): Screen? {
    return ImageSearchScreen(imageUri)
}

/**
 * Android 平台的 ascii2d 搜索 Screen 创建器
 */
actual fun createAscii2dSearchScreen(imageUri: String): Screen? {
    return Ascii2dSearchScreen(imageUri)
}
