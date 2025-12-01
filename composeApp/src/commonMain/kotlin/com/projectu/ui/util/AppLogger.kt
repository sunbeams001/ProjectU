package com.projectu.ui.util

/**
 * 跨平台日志工具
 * 在 Android 上使用 Log，在其他平台使用 println
 */
expect object AppLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
