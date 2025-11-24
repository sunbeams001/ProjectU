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
     * 删除所有设置（重置功能）
     */
    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
}
