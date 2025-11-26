package com.projectu.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.projectu.ui.util.*

/**
 * Android 平台的窗口尺寸计算器
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    
    // 使用 Configuration.screenWidthDp 和 screenHeightDp（推荐方式）
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    
    // 判断设备类型
    val deviceType = if (configuration.smallestScreenWidthDp >= 600) {
        DeviceType.TABLET
    } else {
        DeviceType.PHONE
    }
    
    return remember(width, height, deviceType) {
        WindowSize(
            width = width,
            height = height,
            widthSizeClass = calculateWindowSizeClass(width),
            heightSizeClass = calculateWindowSizeClass(height),
            deviceType = deviceType
        )
    }
}

