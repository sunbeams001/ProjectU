package com.projectu.shared.domain.repository

import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import kotlinx.coroutines.flow.Flow

/**
 * 设置仓储接口
 * 定义设置数据访问的抽象接口
 */
interface SettingsRepository {
    /**
     * 获取设置流
     */
    fun getSettings(): Flow<AppSettings>
    
    /**
     * 获取当前设置
     */
    suspend fun getCurrentSettings(): AppSettings
    
    /**
     * 更新应用语言
     */
    suspend fun updateAppLanguage(language: AppLanguage)
    
    /**
     * 更新 Pixiv API 语言偏好
     * 支持：简体中文、繁体中文、英语、日语、韩语、泰语、马来语
     */
    suspend fun updatePixivLanguage(language: PixivLanguage)
    
    /**
     * 更新主题模式
     */
    suspend fun updateThemeMode(mode: ThemeMode)
    
    /**
     * 更新 R18 Sanity Level 阈值
     * @param threshold 阈值范围 0-9，默认为 6
     */
    suspend fun updateR18SanityLevelThreshold(threshold: Int)
    
    /**
     * 更新插画卡片首选图片质量
     */
    suspend fun updatePreferredImageQuality(quality: ImageQuality)
    
    /**
     * 更新插画详情页首选图片质量
     */
    suspend fun updateDetailImageQuality(quality: DetailImageQuality)
    
    /**
     * 更新完整设置
     */
    suspend fun updateSettings(settings: AppSettings)
    
    /**
     * 重置为默认设置
     */
    suspend fun resetSettings()
}

