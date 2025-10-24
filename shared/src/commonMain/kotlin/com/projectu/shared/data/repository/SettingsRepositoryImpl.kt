package com.projectu.shared.data.repository

import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * 设置仓储实现
 * 使用 SettingsStore 进行数据存储
 */
class SettingsRepositoryImpl(
    private val settingsStore: SettingsStore
) : SettingsRepository {
    
    override fun getSettings(): Flow<AppSettings> {
        return settingsStore.settings
    }
    
    override suspend fun getCurrentSettings(): AppSettings {
        return settingsStore.getCurrentSettings()
    }
    
    override suspend fun updateAppLanguage(language: AppLanguage) {
        settingsStore.setAppLanguage(language)
    }
    
    override suspend fun updatePixivLanguage(language: PixivLanguage) {
        settingsStore.setPixivLanguage(language)
    }
    
    override suspend fun updateThemeMode(mode: ThemeMode) {
        settingsStore.setThemeMode(mode)
    }
    
    override suspend fun updateSettings(settings: AppSettings) {
        settingsStore.updateSettings(settings)
    }
    
    override suspend fun resetSettings() {
        settingsStore.reset()
    }
}

