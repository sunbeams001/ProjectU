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
 * 使用 DataStore 进行认证信息持久化存储
 * 负责管理 PHPSESSID 和 CSRF Token 等认证信息
 */
class PixivConfigStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_PHP_SESSION_ID = stringPreferencesKey("php_session_id")
        private val KEY_CSRF_TOKEN = stringPreferencesKey("csrf_token")
    }
    
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
     * 清除配置（登出）
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_PHP_SESSION_ID)
            preferences.remove(KEY_CSRF_TOKEN)
        }
    }
    
    /**
     * 检查是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentConfig().isValid()
    }
}

