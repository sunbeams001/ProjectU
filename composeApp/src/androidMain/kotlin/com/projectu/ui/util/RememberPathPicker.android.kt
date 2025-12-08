package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * Android 平台实现
 * 使用 rememberLauncherForActivityResult 避免生命周期问题
 */
@Composable
actual fun rememberPathPicker(): PathPicker {
    return rememberAndroidPathPicker()
}
