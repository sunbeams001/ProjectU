package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * Desktop 平台的返回键处理实现
 * Desktop 平台没有系统返回键的概念，此函数为空实现
 * 返回操作由用户点击 UI 中的返回按钮触发
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 平台不需要处理系统返回键
    // 用户通过点击 UI 中的返回按钮来返回
}
