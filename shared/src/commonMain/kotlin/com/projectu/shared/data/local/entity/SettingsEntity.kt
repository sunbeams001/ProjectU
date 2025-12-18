package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode

/**
 * 应用设置数据库实体
 * 用于持久化存储应用的各种设置
 */
@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1, // 固定ID，确保只有一条设置记录
    
    /**
     * 应用界面语言
     */
    val appLanguage: String,
    
    /**
     * Pixiv API 语言偏好
     * 支持：简体中文、繁体中文、英语、日语、韩语、泰语、马来语
     */
    val pixivLanguage: String,
    
    /**
     * 主题设置
     */
    val themeMode: String,
    
    /**
     * R18 Sanity Level 阈值
     * 范围：0-9，默认为 6
     */
    val r18SanityLevelThreshold: Int = 6,
    
    /**
     * 插画卡片首选图片质量
     */
    val preferredImageQuality: String = "SQUARE_MEDIUM",
    
    /**
     * 插画详情页首选图片质量
     */
    val detailImageQuality: String = "LARGE",
    
    /**
     * 小说下载首选图片质量
     */
    val novelDownloadImageQuality: String = "LARGE",
    
    /**
     * 图片磁盘缓存大小
     */
    val imageCacheSize: String = "MEDIUM",
    
    /**
     * 点击收藏按钮的行为
     */
    val clickBookmarkAction: String = "PUBLIC",
    
    /**
     * 长按收藏按钮的行为
     */
    val longPressBookmarkAction: String = "PRIVATE",
    
    /**
     * 下载基础路径
     */
    val baseDownloadPath: String = "",
    
    /**
     * 文件命名模式
     */
    val fileNameMode: String = "STANDARD",
    
    /**
     * 自定义文件命名模板
     */
    val customFileNameTemplate: String = "{id}_{p}_{title}",
    
    /**
     * 设置更新时间
     */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 将 AppSettings 转换为 SettingsEntity
 */
fun com.projectu.shared.data.local.AppSettings.toEntity(): SettingsEntity {
    return SettingsEntity(
        appLanguage = this.appLanguage.name,
        pixivLanguage = this.pixivLanguage.name,
        themeMode = this.themeMode.name,
        r18SanityLevelThreshold = this.r18SanityLevelThreshold,
        preferredImageQuality = this.preferredImageQuality.name,
        detailImageQuality = this.detailImageQuality.name,
        novelDownloadImageQuality = this.novelDownloadImageQuality.name,
        imageCacheSize = this.imageCacheSize.name,
        clickBookmarkAction = this.clickBookmarkAction.name,
        longPressBookmarkAction = this.longPressBookmarkAction.name,
        baseDownloadPath = this.downloadSettings.baseDownloadPath,
        fileNameMode = this.downloadSettings.fileNameMode.name,
        customFileNameTemplate = this.downloadSettings.customFileNameTemplate
    )
}

/**
 * 将 SettingsEntity 转换为 AppSettings
 */
fun SettingsEntity.toAppSettings(): com.projectu.shared.data.local.AppSettings {
    return com.projectu.shared.data.local.AppSettings(
        appLanguage = AppLanguage.valueOf(this.appLanguage),
        pixivLanguage = PixivLanguage.valueOf(this.pixivLanguage),
        themeMode = ThemeMode.valueOf(this.themeMode),
        r18SanityLevelThreshold = this.r18SanityLevelThreshold,
        preferredImageQuality = com.projectu.shared.domain.model.ImageQuality.fromName(this.preferredImageQuality),
        detailImageQuality = com.projectu.shared.domain.model.DetailImageQuality.fromName(this.detailImageQuality),
        novelDownloadImageQuality = com.projectu.shared.domain.model.NovelDownloadImageQuality.fromName(this.novelDownloadImageQuality),
        imageCacheSize = com.projectu.shared.domain.model.CacheSize.fromName(this.imageCacheSize),
        clickBookmarkAction = com.projectu.shared.domain.model.BookmarkAction.valueOf(this.clickBookmarkAction),
        longPressBookmarkAction = com.projectu.shared.domain.model.BookmarkAction.valueOf(this.longPressBookmarkAction),
        downloadSettings = com.projectu.shared.data.local.DownloadSettings(
            baseDownloadPath = this.baseDownloadPath.ifEmpty { com.projectu.shared.data.local.getDefaultDownloadPath() },
            fileNameMode = try {
                com.projectu.shared.data.local.FileNameMode.valueOf(this.fileNameMode)
            } catch (e: Exception) {
                com.projectu.shared.data.local.FileNameMode.STANDARD
            },
            customFileNameTemplate = this.customFileNameTemplate.ifEmpty { "{id}_{p}_{title}" }
        )
    )
}
