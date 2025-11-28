package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * 跨平台的返回键处理
 * 在 Android 上拦截系统返回键/手势
 * 在 Desktop 上此功能不适用（由窗口管理器处理）
 * 
 * @param enabled 是否启用返回拦截
 * @param onBack 返回时的回调
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
