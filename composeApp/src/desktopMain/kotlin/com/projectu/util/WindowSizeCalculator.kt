package com.projectu.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.projectu.ui.util.*
import java.awt.Toolkit

/**
 * Desktop 平台的窗口尺寸计算器
 */
@Composable
actual fun rememberWindowSize(): WindowSize {
    val screenSize = remember {
        val toolkit = Toolkit.getDefaultToolkit()
        val screenSize = toolkit.screenSize
        Pair(screenSize.width, screenSize.height)
    }
    
    return remember(screenSize) {
        val width = screenSize.first.dp
        val height = screenSize.second.dp
        
        WindowSize(
            width = width,
            height = height,
            widthSizeClass = calculateWindowSizeClass(width),
            heightSizeClass = calculateWindowSizeClass(height),
            deviceType = DeviceType.DESKTOP
        )
    }
}

