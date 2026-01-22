package com.projectu.shared.data.backup.datasource

import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.entity.SettingsEntity
import kotlinx.serialization.Serializable

/**
 * 设置备份数据源
 */
class SettingsBackupDataSource(
    private val settingsDao: SettingsDao
) {
    
    /**
     * 导出设置数据
     */
    suspend fun exportData(): SettingsBackupData {
        val entity = settingsDao.getCurrentSettings() 
            ?: throw IllegalStateException("No settings found")
        return SettingsBackupData(
            appLanguage = entity.appLanguage,
            pixivLanguage = entity.pixivLanguage,
            themeMode = entity.themeMode,
            r18SanityLevelThreshold = entity.r18SanityLevelThreshold,
            preferredImageQuality = entity.preferredImageQuality,
            detailImageQuality = entity.detailImageQuality,
            viewerImageQuality = entity.viewerImageQuality,
            novelDownloadImageQuality = entity.novelDownloadImageQuality,
            imageCacheSize = entity.imageCacheSize,
            clickBookmarkAction = entity.clickBookmarkAction,
            longPressBookmarkAction = entity.longPressBookmarkAction,
            staggeredGridColumns = entity.staggeredGridColumns,
            baseDownloadPath = entity.baseDownloadPath,
            fileNameMode = entity.fileNameMode,
            customFileNameTemplate = entity.customFileNameTemplate,
            defaultStartupTab = entity.defaultStartupTab,
            lastUsedTab = entity.lastUsedTab,
            novelFontSize = entity.novelFontSize,
            novelTextColor = entity.novelTextColor,
            novelBackgroundColor = entity.novelBackgroundColor,
            novelBackgroundScheme = entity.novelBackgroundScheme,
            translationEngine = entity.translationEngine,
            translationTargetLanguage = entity.translationTargetLanguage,
            rankingNavigationConfig = entity.rankingNavigationConfig,
            discoveryNavigationConfig = entity.discoveryNavigationConfig,
            followLatestNavigationConfig = entity.followLatestNavigationConfig
        )
    }
    
    /**
     * 导入设置数据
     */
    suspend fun importData(data: SettingsBackupData) {
        val entity = SettingsEntity(
            id = 1,
            appLanguage = data.appLanguage,
            pixivLanguage = data.pixivLanguage,
            themeMode = data.themeMode,
            r18SanityLevelThreshold = data.r18SanityLevelThreshold,
            preferredImageQuality = data.preferredImageQuality,
            detailImageQuality = data.detailImageQuality,
            viewerImageQuality = data.viewerImageQuality,
            novelDownloadImageQuality = data.novelDownloadImageQuality,
            imageCacheSize = data.imageCacheSize,
            clickBookmarkAction = data.clickBookmarkAction,
            longPressBookmarkAction = data.longPressBookmarkAction,
            staggeredGridColumns = data.staggeredGridColumns,
            baseDownloadPath = data.baseDownloadPath,
            fileNameMode = data.fileNameMode,
            customFileNameTemplate = data.customFileNameTemplate,
            defaultStartupTab = data.defaultStartupTab,
            lastUsedTab = data.lastUsedTab,
            novelFontSize = data.novelFontSize,
            novelTextColor = data.novelTextColor,
            novelBackgroundColor = data.novelBackgroundColor,
            novelBackgroundScheme = data.novelBackgroundScheme,
            translationEngine = data.translationEngine,
            translationTargetLanguage = data.translationTargetLanguage,
            rankingNavigationConfig = data.rankingNavigationConfig,
            discoveryNavigationConfig = data.discoveryNavigationConfig,
            followLatestNavigationConfig = data.followLatestNavigationConfig
        )
        settingsDao.upsertSettings(entity)
    }
}

/**
 * 设置备份数据
 */
@Serializable
data class SettingsBackupData(
    val appLanguage: String,
    val pixivLanguage: String,
    val themeMode: String,
    val r18SanityLevelThreshold: Int,
    val preferredImageQuality: String,
    val detailImageQuality: String,
    val viewerImageQuality: String,
    val novelDownloadImageQuality: String,
    val imageCacheSize: String,
    val clickBookmarkAction: String,
    val longPressBookmarkAction: String,
    val staggeredGridColumns: Int,
    val baseDownloadPath: String,
    val fileNameMode: String,
    val customFileNameTemplate: String,
    val defaultStartupTab: String,
    val lastUsedTab: String,
    val novelFontSize: String,
    val novelTextColor: String?,
    val novelBackgroundColor: String?,
    val novelBackgroundScheme: String,
    val translationEngine: String,
    val translationTargetLanguage: String,
    val rankingNavigationConfig: String,
    val discoveryNavigationConfig: String,
    val followLatestNavigationConfig: String
)
