package com.projectu.ui.util

import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android平台WebView Cookie提取实现
 */
actual suspend fun extractCookiesFromWebView(
    nativeWebView: Any,
    domain: String
): Map<String, String> = withContext(Dispatchers.Main) {
    try {
        // 确保是Android WebView
        if (nativeWebView !is WebView) {
            return@withContext emptyMap()
        }
        
        // 获取CookieManager
        val cookieManager = CookieManager.getInstance()
        
        // 获取指定域名的Cookie字符串
        val cookieString = cookieManager.getCookie(domain) ?: return@withContext emptyMap()
        
        // 解析Cookie字符串
        parseCookieString(cookieString)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyMap()
    }
}
