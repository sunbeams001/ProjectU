package com.projectu.ui.util

/**
 * 跨平台WebView Cookie提取工具
 * 用于从WebView中提取PHPSESSID等Cookie信息
 */

/**
 * 从原生WebView对象中提取Cookie
 * @param nativeWebView 原生WebView对象（Android: android.webkit.WebView, Desktop: 待实现）
 * @param domain Cookie所属域名
 * @return Cookie字符串映射表
 */
expect suspend fun extractCookiesFromWebView(
    nativeWebView: Any,
    domain: String = "https://www.pixiv.net"
): Map<String, String>

/**
 * 清除WebView的所有Cookie
 * 用于退出登录时清理缓存，确保下次登录时不会自动使用上次的账号
 */
expect suspend fun clearWebViewCookies()

/**
 * 从Cookie映射表中提取PHPSESSID
 * @param cookies Cookie映射表
 * @return PHPSESSID值，如果不存在或格式不正确则返回null
 */
fun extractPhpSessionId(cookies: Map<String, String>): String? {
    val phpsessid = cookies["PHPSESSID"] ?: return null
    
    // 验证格式: 数字_字母数字组合
    return if (phpsessid.matches(Regex("\\d+_[a-zA-Z0-9]+"))) {
        phpsessid
    } else {
        null
    }
}

/**
 * 从Cookie字符串中解析为映射表
 * @param cookieString Cookie字符串，格式: "key1=value1; key2=value2; ..."
 * @return Cookie映射表
 */
fun parseCookieString(cookieString: String): Map<String, String> {
    return cookieString.split(";")
        .mapNotNull { cookie ->
            val parts = cookie.trim().split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                null
            }
        }
        .toMap()
}
