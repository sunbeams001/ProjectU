package com.projectu.util

import androidx.compose.runtime.Composable
import com.projectu.ui.util.WindowSize

/**
 * 跨平台的窗口尺寸获取
 * 使用 expect/actual 机制实现平台特定逻辑
 */
@Composable
expect fun rememberWindowSize(): WindowSize

