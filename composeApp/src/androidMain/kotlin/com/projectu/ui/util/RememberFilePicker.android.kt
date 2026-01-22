package com.projectu.ui.util

import androidx.compose.runtime.Composable

/**
 * Android平台实现
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: Array<String>,
    onFileSelected: (String?) -> Unit
): () -> Unit {
    return rememberAndroidFilePicker(mimeTypes, onFileSelected)
}
