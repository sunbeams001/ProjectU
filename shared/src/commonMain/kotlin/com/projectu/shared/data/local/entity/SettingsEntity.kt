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
     */
    val pixivLanguage: String,
    
    /**
     * 主题设置
     */
    val themeMode: String,
    
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
        themeMode = this.themeMode.name
    )
}

/**
 * 将 SettingsEntity 转换为 AppSettings
 */
fun SettingsEntity.toAppSettings(): com.projectu.shared.data.local.AppSettings {
    return com.projectu.shared.data.local.AppSettings(
        appLanguage = AppLanguage.valueOf(this.appLanguage),
        pixivLanguage = PixivLanguage.valueOf(this.pixivLanguage),
        themeMode = ThemeMode.valueOf(this.themeMode)
    )
}
