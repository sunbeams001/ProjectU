package com.projectu.shared.data.local

import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.entity.SettingsEntity
import com.projectu.shared.data.local.entity.toAppSettings
import com.projectu.shared.data.local.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * 应用设置存储
 * 使用 Room 数据库进行持久化存储
 * 支持跨平台数据持久化
 */
class SettingsStore(
    private val settingsDao: SettingsDao
) {
    
    /**
     * 获取设置流
     * 从数据库读取设置并转换为AppSettings
     */
    val settings: Flow<AppSettings> = settingsDao.getSettings()
        .map { entity ->
            entity?.toAppSettings() ?: AppSettings.DEFAULT
        }
        .onStart {
            // 如果数据库中没有设置，则初始化默认设置
            val currentSettings = settingsDao.getCurrentSettings()
            if (currentSettings == null) {
                settingsDao.upsertSettings(AppSettings.DEFAULT.toEntity())
            }
        }
    
    /**
     * 获取当前设置
     */
    suspend fun getCurrentSettings(): AppSettings {
        return settingsDao.getCurrentSettings()?.toAppSettings() ?: AppSettings.DEFAULT
    }
    
    /**
     * 更新设置
     */
    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.upsertSettings(settings.toEntity())
    }
    
    /**
     * 设置应用语言
     */
    suspend fun setAppLanguage(language: AppLanguage) {
        settingsDao.updateAppLanguage(language.name)
    }
    
    /**
     * 设置 Pixiv API 语言偏好
     * 支持：简体中文、繁体中文、英语、日语、韩语、泰语、马来语
     */
    suspend fun setPixivLanguage(language: PixivLanguage) {
        settingsDao.updatePixivLanguage(language.name)
    }
    
    /**
     * 设置主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        settingsDao.updateThemeMode(mode.name)
    }
    
    /**
     * 设置 R18 Sanity Level 阈值
     * @param threshold 阈值范围 0-9，默认为 6
     */
    suspend fun setR18SanityLevelThreshold(threshold: Int) {
        settingsDao.updateR18SanityLevelThreshold(threshold.coerceIn(0, 9))
    }
    
    /**
     * 设置插画卡片首选图片质量
     */
    suspend fun setPreferredImageQuality(quality: com.projectu.shared.domain.model.ImageQuality) {
        settingsDao.updatePreferredImageQuality(quality.name)
    }
    
    /**
     * 设置插画详情页首选图片质量
     */
    suspend fun setDetailImageQuality(quality: com.projectu.shared.domain.model.DetailImageQuality) {
        settingsDao.updateDetailImageQuality(quality.name)
    }
    
    /**
     * 设置小说下载首选图片质量
     */
    suspend fun setNovelDownloadImageQuality(quality: com.projectu.shared.domain.model.NovelDownloadImageQuality) {
        settingsDao.updateNovelDownloadImageQuality(quality.name)
    }
    
    /**
     * 设置图片磁盘缓存大小
     */
    suspend fun setImageCacheSize(size: com.projectu.shared.domain.model.CacheSize) {
        settingsDao.updateImageCacheSize(size.name)
    }
    
    /**
     * 设置下载基础路径
     */
    suspend fun setBaseDownloadPath(path: String) {
        settingsDao.updateBaseDownloadPath(path)
    }
    
    /**
     * 重置为默认设置
     */
    suspend fun reset() {
        settingsDao.deleteAllSettings()
        settingsDao.upsertSettings(AppSettings.DEFAULT.toEntity())
    }
}

