package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * Desktop 平台实现
 */
@Composable
actual fun rememberPathPicker(): PathPicker {
    return rememberDesktopPathPicker()
}
