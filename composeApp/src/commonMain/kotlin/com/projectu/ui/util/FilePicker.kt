package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * 跨平台文件选择器
 */
@Composable
expect fun rememberFilePicker(
    mimeTypes: Array<String> = arrayOf("*/*"),
    onFileSelected: (String?) -> Unit
): () -> Unit
