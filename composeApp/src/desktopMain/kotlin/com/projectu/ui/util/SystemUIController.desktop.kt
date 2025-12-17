package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * 隐藏系统UI（状态栏和导航栏）
 * Desktop实现（无操作）
 */
@Composable
actual fun HideSystemUI() {
    // Desktop平台不需要隐藏系统UI
}
