package com.projectu.shared.domain.repository

import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.FileNameMode
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.ViewerImageQuality
import com.projectu.shared.domain.model.NovelDownloadImageQuality
import com.projectu.shared.domain.model.CacheSize
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
     * 更新作品大图浏览页首选图片质量
     */
    suspend fun updateViewerImageQuality(quality: ViewerImageQuality)
    
    /**
     * 更新小说下载首选图片质量
     */
    suspend fun updateNovelDownloadImageQuality(quality: NovelDownloadImageQuality)
    
    /**
     * 更新图片缓存大小
     * @param size 缓存大小设置
     */
    suspend fun updateImageCacheSize(size: CacheSize)
    
    /**
     * 更新点击收藏按钮的行为
     * @param action 收藏行为
     */
    suspend fun updateClickBookmarkAction(action: com.projectu.shared.domain.model.BookmarkAction)
    
    /**
     * 更新长按收藏按钮的行为
     * @param action 收藏行为
     */
    suspend fun updateLongPressBookmarkAction(action: com.projectu.shared.domain.model.BookmarkAction)
    
    /**
     * 更新瀑布流列数
     * @param columns 列数，范围 2-5
     */
    suspend fun updateStaggeredGridColumns(columns: Int)
    
    /**
     * 更新下载基础路径
     * @param path 下载基础路径
     */
    suspend fun updateBaseDownloadPath(path: String)
    
    /**
     * 更新文件命名模式
     * @param mode 文件命名模式
     */
    suspend fun updateFileNameMode(mode: FileNameMode)
    
    /**
     * 更新自定义文件命名模板
     * @param template 自定义模板
     */
    suspend fun updateCustomFileNameTemplate(template: String)
    
    /**
     * 更新默认启动Tab
     * @param tab 启动Tab设置
     */
    suspend fun updateDefaultStartupTab(tab: com.projectu.shared.data.local.StartupTab)
    
    /**
     * 更新最后使用的Tab
     * @param tab Tab标识字符串
     */
    suspend fun updateLastUsedTab(tab: String)
    
    /**
     * 更新小说阅读字号
     */
    suspend fun updateNovelFontSize(fontSize: com.projectu.shared.data.local.NovelFontSize)
    
    /**
     * 更新小说阅读文字颜色
     */
    suspend fun updateNovelTextColor(color: String?)
    
    /**
     * 更新小说阅读背景色
     */
    suspend fun updateNovelBackgroundColor(color: String?)
    
    /**
     * 更新小说阅读背景色方案
     */
    suspend fun updateNovelBackgroundScheme(scheme: com.projectu.shared.data.local.NovelBackgroundScheme)
    
    /**
     * 更新翻译引擎
     */
    suspend fun updateTranslationEngine(engine: com.projectu.shared.domain.model.TranslationEngine)
    
    /**
     * 更新翻译目标语言
     */
    suspend fun updateTranslationTargetLanguage(language: com.projectu.shared.domain.model.TranslationLanguage)

    /**
     * 更新完整设置
     */
    suspend fun updateSettings(settings: AppSettings)
    
    /**
     * 更新排行榜导航偏好
     */
    suspend fun updateRankingNavigationPreferences(preferences: com.projectu.shared.domain.model.RankingNavigationPreferences)
    
    /**
     * 更新发现页导航偏好
     */
    suspend fun updateDiscoveryNavigationPreferences(preferences: com.projectu.shared.domain.model.DiscoveryNavigationPreferences)
    
    /**
     * 更新动态页导航偏好
     */
    suspend fun updateFollowLatestNavigationPreferences(preferences: com.projectu.shared.domain.model.FollowLatestNavigationPreferences)
    
    /**
     * 重置为默认设置
     */
    suspend fun resetSettings()
}

