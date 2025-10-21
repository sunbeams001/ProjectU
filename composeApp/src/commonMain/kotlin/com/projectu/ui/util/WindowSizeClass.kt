package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口尺寸分类
 * 基于 Material Design 3 的自适应布局指南
 */
enum class WindowSizeClass {
    /** 紧凑型：手机竖屏（< 600dp）*/
    COMPACT,
    
    /** 中等型：手机横屏、小平板（600dp - 840dp）*/
    MEDIUM,
    
    /** 扩展型：大平板、折叠屏（> 840dp）*/
    EXPANDED
}

/**
 * 设备类型
 */
enum class DeviceType {
    PHONE,      // 手机
    TABLET,     // 平板
    DESKTOP     // 桌面
}

/**
 * 窗口尺寸信息
 */
data class WindowSize(
    val width: Dp,
    val height: Dp,
    val widthSizeClass: WindowSizeClass,
    val heightSizeClass: WindowSizeClass,
    val deviceType: DeviceType
)

/**
 * 根据宽度计算尺寸分类
 */
fun calculateWindowSizeClass(width: Dp): WindowSizeClass {
    return when {
        width < 600.dp -> WindowSizeClass.COMPACT
        width < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * 判断是否为手机布局
 */
fun WindowSize.isPhoneLayout(): Boolean {
    return deviceType == DeviceType.PHONE || 
           (widthSizeClass == WindowSizeClass.COMPACT && heightSizeClass != WindowSizeClass.COMPACT)
}

/**
 * 判断是否为平板布局
 */
fun WindowSize.isTabletLayout(): Boolean {
    return deviceType == DeviceType.TABLET || 
           widthSizeClass == WindowSizeClass.MEDIUM || 
           widthSizeClass == WindowSizeClass.EXPANDED
}

/**
 * 判断是否为桌面布局
 */
fun WindowSize.isDesktopLayout(): Boolean {
    return deviceType == DeviceType.DESKTOP
}

/**
 * 获取自适应的列数（用于网格布局）
 */
fun WindowSize.getGridColumns(): Int {
    return when (widthSizeClass) {
        WindowSizeClass.COMPACT -> 2   // 手机：2列
        WindowSizeClass.MEDIUM -> 3    // 小平板：3列
        WindowSizeClass.EXPANDED -> 4  // 大平板/桌面：4列
    }
}

/**
 * 获取自适应的内边距
 */
fun WindowSize.getScreenPadding(): Dp {
    return when (widthSizeClass) {
        WindowSizeClass.COMPACT -> 16.dp   // 手机：16dp
        WindowSizeClass.MEDIUM -> 24.dp    // 小平板：24dp
        WindowSizeClass.EXPANDED -> 32.dp  // 大平板/桌面：32dp
    }
}

