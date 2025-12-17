package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * 隐藏系统UI（状态栏和导航栏）
 * 平台特定实现
 */
@Composable
expect fun HideSystemUI()
