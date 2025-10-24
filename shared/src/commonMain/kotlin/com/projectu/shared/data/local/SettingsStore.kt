package com.projectu.shared.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用设置存储
 * 用于在运行时管理应用设置
 * 
 * 注意：这是一个简单的内存存储实现
 * 在实际应用中，应该使用 DataStore 等持久化存储
 */
class SettingsStore {
    private val _settings = MutableStateFlow(AppSettings.DEFAULT)
    val settings: Flow<AppSettings> = _settings.asStateFlow()
    
    /**
     * 获取当前设置
     */
    fun getCurrentSettings(): AppSettings {
        return _settings.value
    }
    
    /**
     * 更新设置
     */
    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
    }
    
    /**
     * 设置应用语言
     */
    fun setAppLanguage(language: AppLanguage) {
        _settings.value = _settings.value.copy(appLanguage = language)
    }
    
    /**
     * 设置 Pixiv 语言
     */
    fun setPixivLanguage(language: PixivLanguage) {
        _settings.value = _settings.value.copy(pixivLanguage = language)
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }
    
    /**
     * 重置为默认设置
     */
    fun reset() {
        _settings.value = AppSettings.DEFAULT
    }
}

