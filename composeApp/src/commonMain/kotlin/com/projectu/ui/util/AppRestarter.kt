package com.projectu.ui.util

/**
 * 平台特定的应用重启工具
 */
expect object AppRestarter {
    /**
     * 重启应用
     * @param delayMillis 延迟毫秒数，用于确保当前操作完成
     */
    fun restartApp(delayMillis: Long = 1000)
}
