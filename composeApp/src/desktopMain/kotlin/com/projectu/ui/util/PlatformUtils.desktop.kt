package com.projectu.ui.util

/**
 * Desktop 实现：不需要 SAF 授权
 * Desktop 平台没有 Scoped Storage 限制
 */
actual fun needsSafAuthorization(): Boolean {
    return false
}
