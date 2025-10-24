package com.projectu.shared.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pixiv 配置存储
 * 用于在运行时管理 Pixiv 配置
 * 
 * 注意：这是一个简单的内存存储实现
 * 在实际应用中，应该使用 DataStore 等持久化存储
 */
class PixivConfigStore {
    private val _config = MutableStateFlow(PixivConfig.DEFAULT)
    val config: Flow<PixivConfig> = _config.asStateFlow()
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): PixivConfig {
        return _config.value
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(config: PixivConfig) {
        _config.value = config
    }
    
    /**
     * 设置 PHPSESSID
     */
    fun setPhpSessionId(phpSessionId: String) {
        _config.value = _config.value.copy(phpSessionId = phpSessionId)
    }
    
    /**
     * 设置 CSRF Token
     */
    fun setCsrfToken(token: String) {
        _config.value = _config.value.copy(csrfToken = token)
    }
    
    /**
     * 设置语言
     * @param language Pixiv 语言代码
     */
    fun setLanguage(language: String) {
        _config.value = _config.value.copy(language = language)
    }
    
    /**
     * 从应用设置同步 Pixiv 语言
     */
    fun syncLanguageFromSettings(pixivLanguage: PixivLanguage) {
        _config.value = _config.value.copy(language = pixivLanguage.code)
    }
    
    /**
     * 清除配置（登出）
     */
    fun clear() {
        _config.value = PixivConfig.DEFAULT
    }
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return _config.value.isValid()
    }
}

