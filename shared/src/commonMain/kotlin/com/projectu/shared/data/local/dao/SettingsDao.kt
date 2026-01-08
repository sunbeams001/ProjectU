package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * 应用设置数据访问对象
 * 定义设置数据的数据库操作
 */
@Dao
interface SettingsDao {
    
    /**
     * 获取设置流
     * 返回Flow以便观察设置变化
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>
    
    /**
     * 获取当前设置
     */
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getCurrentSettings(): SettingsEntity?
    
    /**
     * 插入或更新设置
     * 使用 UPSERT 操作确保只有一条记录
     */
    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)
    
    /**
     * 更新应用语言
     */
    @Query("UPDATE app_settings SET appLanguage = :language, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateAppLanguage(language: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新 Pixiv API 语言偏好
     * 支持：简体中文(zh)、繁体中文(zh_tw)、英语(en)、日语(ja)、韩语(ko)、泰语(th)、马来语(ms)
     */
    @Query("UPDATE app_settings SET pixivLanguage = :language, updatedAt = :timestamp WHERE id = 1")
    suspend fun updatePixivLanguage(language: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新主题模式
     */
    @Query("UPDATE app_settings SET themeMode = :mode, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateThemeMode(mode: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新 R18 Sanity Level 阈值
     * @param threshold 阈值范围 0-9，默认为 6
     */
    @Query("UPDATE app_settings SET r18SanityLevelThreshold = :threshold, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateR18SanityLevelThreshold(threshold: Int, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新插画卡片首选图片质量
     */
    @Query("UPDATE app_settings SET preferredImageQuality = :quality, updatedAt = :timestamp WHERE id = 1")
    suspend fun updatePreferredImageQuality(quality: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新插画详情页首选图片质量
     */
    @Query("UPDATE app_settings SET detailImageQuality = :quality, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateDetailImageQuality(quality: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新作品大图浏览页首选图片质量
     */
    @Query("UPDATE app_settings SET viewerImageQuality = :quality, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateViewerImageQuality(quality: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新小说下载首选图片质量
     */
    @Query("UPDATE app_settings SET novelDownloadImageQuality = :quality, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNovelDownloadImageQuality(quality: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新图片磁盘缓存大小
     */
    @Query("UPDATE app_settings SET imageCacheSize = :size, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateImageCacheSize(size: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新点击收藏按钮的行为
     */
    @Query("UPDATE app_settings SET clickBookmarkAction = :action, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateClickBookmarkAction(action: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新长按收藏按钮的行为
     */
    @Query("UPDATE app_settings SET longPressBookmarkAction = :action, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateLongPressBookmarkAction(action: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新瀑布流列数
     */
    @Query("UPDATE app_settings SET staggeredGridColumns = :columns, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateStaggeredGridColumns(columns: Int, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新下载基础路径
     */
    @Query("UPDATE app_settings SET baseDownloadPath = :path, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateBaseDownloadPath(path: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新文件命名模式
     */
    @Query("UPDATE app_settings SET fileNameMode = :mode, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateFileNameMode(mode: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新自定义文件命名模板
     */
    @Query("UPDATE app_settings SET customFileNameTemplate = :template, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateCustomFileNameTemplate(template: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新默认启动Tab
     */
    @Query("UPDATE app_settings SET defaultStartupTab = :tab, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateDefaultStartupTab(tab: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新最后使用的Tab
     */
    @Query("UPDATE app_settings SET lastUsedTab = :tab, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateLastUsedTab(tab: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新小说阅读字号
     */
    @Query("UPDATE app_settings SET novelFontSize = :fontSize, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNovelFontSize(fontSize: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新小说阅读文字颜色
     */
    @Query("UPDATE app_settings SET novelTextColor = :color, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNovelTextColor(color: String?, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新小说阅读背景色
     */
    @Query("UPDATE app_settings SET novelBackgroundColor = :color, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNovelBackgroundColor(color: String?, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新小说阅读背景色方案
     */
    @Query("UPDATE app_settings SET novelBackgroundScheme = :scheme, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNovelBackgroundScheme(scheme: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新翻译引擎
     */
    @Query("UPDATE app_settings SET translationEngine = :engine, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateTranslationEngine(engine: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新翻译目标语言
     */
    @Query("UPDATE app_settings SET translationTargetLanguage = :language, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateTranslationTargetLanguage(language: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 删除所有设置（重置功能）
     */
    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
}
