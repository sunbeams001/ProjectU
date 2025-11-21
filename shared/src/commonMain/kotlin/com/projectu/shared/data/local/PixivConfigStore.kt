package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Pixiv 配置存储
 * 使用 DataStore 进行认证信息持久化存储
 * 语言设置仅在内存中维护，从数据库读取
 */
class PixivConfigStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_PHP_SESSION_ID = stringPreferencesKey("php_session_id")
        private val KEY_CSRF_TOKEN = stringPreferencesKey("csrf_token")
    }
    
    // 语言设置 - 仅内存存储，从 AppSettings 同步
    private val _language = MutableStateFlow("zh")
    val language: StateFlow<String> = _language.asStateFlow()
    
    /**
     * 配置流
     */
    val config: Flow<PixivConfig> = dataStore.data.map { preferences ->
        PixivConfig(
            phpSessionId = preferences[KEY_PHP_SESSION_ID] ?: "",
            csrfToken = preferences[KEY_CSRF_TOKEN]
        )
    }
    
    /**
     * 获取当前配置
     */
    suspend fun getCurrentConfig(): PixivConfig {
        return config.first()
    }
    
    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): String {
        return _language.value
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfig(config: PixivConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_PHP_SESSION_ID] = config.phpSessionId
            config.csrfToken?.let { preferences[KEY_CSRF_TOKEN] = it }
        }
    }
    
    /**
     * 设置 PHPSESSID
     */
    suspend fun setPhpSessionId(phpSessionId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_PHP_SESSION_ID] = phpSessionId
        }
    }
    
    /**
     * 设置 CSRF Token
     */
    suspend fun setCsrfToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CSRF_TOKEN] = token
        }
    }
    
    /**
     * 在内存中设置语言（不持久化）
     * 由 App.kt 的响应式监听调用，从数据库读取后更新内存状态
     * @param language Pixiv 语言代码（支持：zh, zh_tw, en, ja, ko, th, ms）
     */
    fun setLanguageInMemory(language: String) {
        _language.value = language
    }
    
    /**
     * 清除配置（登出）
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PHP_SESSION_ID)
            preferences.remove(KEY_CSRF_TOKEN)
        }
        // 语言保持不变，不需要重置
    }
    
    /**
     * 检查是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentConfig().isValid()
    }
}

