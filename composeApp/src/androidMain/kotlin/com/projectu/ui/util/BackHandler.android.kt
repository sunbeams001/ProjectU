package com.projectu.ui.util

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android 平台的返回键处理实现
 * 使用 androidx.activity.compose.BackHandler 拦截系统返回键/手势
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
