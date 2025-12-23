package com.projectu

import cafe.adriel.voyager.core.screen.Screen

/**
 * Desktop 平台的图片搜索 Screen 创建器
 * 桌面端暂不支持图片搜索功能
 */
actual fun createImageSearchScreen(imageUri: String): Screen? {
    // 桌面端暂不支持
    return null
}

/**
 * Desktop 平台的 Ascii2d 搜索 Screen 创建器
 * 桌面端暂不支持 Ascii2d 搜索功能
 */
actual fun createAscii2dSearchScreen(imageUri: String): Screen? {
    // 桌面端暂不支持
    return null
}
