@file:Suppress("DEPRECATION")

package com.projectu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 剪贴板帮助类
 * 
 * 封装已弃用的 LocalClipboardManager API，便于以后统一升级到新 API
 * 
 * 使用示例：
 * ```kotlin
 * val copyToClipboard = rememberCopyToClipboard()
 * Button(onClick = { copyToClipboard("要复制的文本") }) {
 *     Text("复制")
 * }
 * ```
 */
@Composable
fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    return remember(clipboardManager, scope) {
        { text: String ->
            scope.launch {
                clipboardManager.setText(AnnotatedString(text))
            }
        }
    }
}

/**
 * 剪贴板帮助类（带协程作用域）
 * 
 * 当你需要在复制后执行其他挂起操作时使用此版本
 * 
 * 使用示例：
 * ```kotlin
 * val (copyToClipboard, scope) = rememberCopyToClipboardWithScope()
 * Button(onClick = { 
 *     scope.launch {
 *         copyToClipboard("文本")
 *         snackbarHostState.showSnackbar("已复制")
 *     }
 * }) {
 *     Text("复制")
 * }
 * ```
 */
@Composable
fun rememberCopyToClipboardWithScope(): Pair<suspend (String) -> Unit, CoroutineScope> {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    val copyFunction: suspend (String) -> Unit = remember(clipboardManager) {
        { text: String ->
            clipboardManager.setText(AnnotatedString(text))
        }
    }
    
    return Pair(copyFunction, scope)
}
