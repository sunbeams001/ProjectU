package com.projectu.ui.util

/**
 * 检查当前平台是否需要 SAF (Storage Access Framework) 授权
 * Android 10 (API 29) 及以上需要
 */
expect fun needsSafAuthorization(): Boolean
