package com.projectu.shared.data.repository

import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.FileNameMode
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.ImageQuality
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
    
    override suspend fun updateR18SanityLevelThreshold(threshold: Int) {
        settingsStore.setR18SanityLevelThreshold(threshold)
    }
    
    override suspend fun updatePreferredImageQuality(quality: ImageQuality) {
        settingsStore.setPreferredImageQuality(quality)
    }
    
    override suspend fun updateDetailImageQuality(quality: com.projectu.shared.domain.model.DetailImageQuality) {
        settingsStore.setDetailImageQuality(quality)
    }
    
    override suspend fun updateViewerImageQuality(quality: com.projectu.shared.domain.model.ViewerImageQuality) {
        settingsStore.setViewerImageQuality(quality)
    }
    
    override suspend fun updateNovelDownloadImageQuality(quality: com.projectu.shared.domain.model.NovelDownloadImageQuality) {
        settingsStore.setNovelDownloadImageQuality(quality)
    }
    
    override suspend fun updateImageCacheSize(size: CacheSize) {
        settingsStore.setImageCacheSize(size)
    }
    
    override suspend fun updateClickBookmarkAction(action: com.projectu.shared.domain.model.BookmarkAction) {
        settingsStore.setClickBookmarkAction(action)
    }
    
    override suspend fun updateLongPressBookmarkAction(action: com.projectu.shared.domain.model.BookmarkAction) {
        settingsStore.setLongPressBookmarkAction(action)
    }
    
    override suspend fun updateBaseDownloadPath(path: String) {
        settingsStore.setBaseDownloadPath(path)
    }
    
    override suspend fun updateFileNameMode(mode: FileNameMode) {
        settingsStore.setFileNameMode(mode)
    }
    
    override suspend fun updateCustomFileNameTemplate(template: String) {
        settingsStore.setCustomFileNameTemplate(template)
    }
    
    override suspend fun updateSettings(settings: AppSettings) {
        settingsStore.updateSettings(settings)
    }
    
    override suspend fun resetSettings() {
        settingsStore.reset()
    }
}

