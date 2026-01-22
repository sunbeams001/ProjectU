package com.projectu.ui.util

/**
 * Android 实现：检查是否需要 SAF 授权
 * Android 10 (API 29, Android Q) 及以上需要 Scoped Storage
 */
actual fun needsSafAuthorization(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
}
