package com.projectu.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Pixiv 配置存储
 * 使用 DataStore 进行持久化存储
 */
class PixivConfigStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_PHP_SESSION_ID = stringPreferencesKey("php_session_id")
        private val KEY_CSRF_TOKEN = stringPreferencesKey("csrf_token")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
    }
    
    /**
     * 配置流
     */
    val config: Flow<PixivConfig> = dataStore.data.map { preferences ->
        PixivConfig(
            phpSessionId = preferences[KEY_PHP_SESSION_ID] ?: "",
            csrfToken = preferences[KEY_CSRF_TOKEN],
            language = preferences[KEY_LANGUAGE] ?: "zh"
        )
    }
    
    /**
     * 获取当前配置
     */
    suspend fun getCurrentConfig(): PixivConfig {
        return config.first()
    }
    
    /**
     * 更新配置
     */
    suspend fun updateConfig(config: PixivConfig) {
        dataStore.edit { preferences ->
            preferences[KEY_PHP_SESSION_ID] = config.phpSessionId
            config.csrfToken?.let { preferences[KEY_CSRF_TOKEN] = it }
            preferences[KEY_LANGUAGE] = config.language
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
     * 设置语言
     * @param language Pixiv 语言代码
     */
    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }
    
    /**
     * 从应用设置同步 Pixiv 语言
     */
    suspend fun syncLanguageFromSettings(pixivLanguage: PixivLanguage) {
        setLanguage(pixivLanguage.code)
    }
    
    /**
     * 清除配置（登出）
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PHP_SESSION_ID)
            preferences.remove(KEY_CSRF_TOKEN)
            preferences[KEY_LANGUAGE] = "zh"  // 保留语言设置
        }
    }
    
    /**
     * 检查是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentConfig().isValid()
    }
}

