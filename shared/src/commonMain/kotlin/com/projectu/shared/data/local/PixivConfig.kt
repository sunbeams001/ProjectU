package com.projectu.shared.data.local

/**
 * Pixiv 配置
 * 用于存储和管理 Pixiv API 认证信息
 */
data class PixivConfig(
    /**
     * PHPSESSID Cookie 值
     * 可以从浏览器登录 Pixiv 后的 Cookie 中获取
     * 格式：PHPSESSID=xxxxxx_xxxxxxxxxxxx
     */
    val phpSessionId: String = "",
    
    /**
     * CSRF Token
     * 可选，如果不提供会自动从服务器获取
     */
    val csrfToken: String? = null,
    
    /**
     * API 主机地址
     * 默认为 https://www.pixiv.net
     */
    val host: String = "https://www.pixiv.net",
    
    /**
     * 语言设置
     * 支持：zh(中文), en(英文), ja(日文), ko(韩文)
     */
    val language: String = "zh"
) {
    /**
     * 检查配置是否有效
     */
    fun isValid(): Boolean {
        return phpSessionId.isNotBlank() && phpSessionId.contains("_")
    }
    
    /**
     * 获取用户ID（从 PHPSESSID 中解析）
     */
    fun getUserId(): Long? {
        return try {
            phpSessionId.split("_")[0].toLong()
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        /**
         * 默认配置（未登录状态）
         */
        val DEFAULT = PixivConfig()
        
        /**
         * 从 PHPSESSID 创建配置
         */
        fun fromPhpSessionId(phpSessionId: String): PixivConfig {
            return PixivConfig(phpSessionId = phpSessionId)
        }
    }
}

