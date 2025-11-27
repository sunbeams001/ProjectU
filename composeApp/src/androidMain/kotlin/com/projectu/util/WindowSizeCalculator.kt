package com.projectu.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.projectu.ui.util.*

/**
 * Android 平台的窗口尺寸计算器
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // 使用 LocalWindowInfo.current.containerSize（Compose UI 1.8.0+ 推荐方式）
    val containerSize = windowInfo.containerSize
    
    val width = with(density) { containerSize.width.toDp() }
    val height = with(density) { containerSize.height.toDp() }
    
    // 判断设备类型
    val deviceType = if (configuration.smallestScreenWidthDp >= 600) {
        DeviceType.TABLET
    } else {
        DeviceType.PHONE
    }
    
    return WindowSize(
        width = width,
        height = height,
        widthSizeClass = calculateWindowSizeClass(width),
        heightSizeClass = calculateWindowSizeClass(height),
        deviceType = deviceType
    )
}

