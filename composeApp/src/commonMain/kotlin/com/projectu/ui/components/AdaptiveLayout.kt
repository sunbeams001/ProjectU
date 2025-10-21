package com.projectu.ui.components

import androidx.compose.runtime.Composable
import com.projectu.ui.util.WindowSize
import com.projectu.ui.util.isPhoneLayout
import com.projectu.ui.util.isTabletLayout
import com.projectu.util.rememberWindowSize

/**
 * 自适应布局组件
 * 根据窗口尺寸自动选择合适的布局
 */
@Composable
fun AdaptiveLayout(
    phoneContent: @Composable (WindowSize) -> Unit,
    tabletContent: @Composable (WindowSize) -> Unit,
    desktopContent: (@Composable (WindowSize) -> Unit)? = null
) {
    val windowSize = rememberWindowSize()
    
    when {
        windowSize.isPhoneLayout() -> phoneContent(windowSize)
        windowSize.isTabletLayout() -> tabletContent(windowSize)
        else -> (desktopContent ?: tabletContent)(windowSize)
    }
}

/**
 * 简化版自适应布局
 * 平板和桌面使用相同布局
 */
@Composable
fun SimpleAdaptiveLayout(
    phoneContent: @Composable (WindowSize) -> Unit,
    tabletContent: @Composable (WindowSize) -> Unit
) {
    val windowSize = rememberWindowSize()
    
    if (windowSize.isPhoneLayout()) {
        phoneContent(windowSize)
    } else {
        tabletContent(windowSize)
    }
}

