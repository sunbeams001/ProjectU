package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
     * 作品大图浏览页首选图片质量
     */
    val viewerImageQuality: String = "MASTER_1200",
    
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
     * 瀑布流列数
     */
    val staggeredGridColumns: Int = 3,
    
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
     * 默认启动Tab页面
     */
    val defaultStartupTab: String = "LAST_USED",
    
    /**
     * 最后使用的Tab页面
     */
    val lastUsedTab: String = "HOME",
    
    /**
     * 小说阅读字号
     */
    val novelFontSize: String = "MEDIUM",
    
    /**
     * 小说阅读文字颜色（十六进制）
     */
    val novelTextColor: String? = null,
    
    /**
     * 小说阅读背景色（十六进制）
     */
    val novelBackgroundColor: String? = null,
    
    /**
     * 小说阅读背景色方案
     */
    val novelBackgroundScheme: String = "THEME_DEFAULT",
    
    /**
     * 翻译引擎
     */
    val translationEngine: String = "NONE",
    
    /**
     * 翻译目标语言
     */
    val translationTargetLanguage: String = "SIMPLIFIED_CHINESE",
    
    /**
     * 排行榜导航配置（嵌套JSON字符串）
     * 存储格式：{
     *   "enabledContentTypes": ["ALL", "ILLUST", "MANGA"],
     *   "enabledModesPerContent": {"ALL":["DAILY","WEEKLY"], "ILLUST":["DAILY"]}
     * }
     */
    val rankingNavigationConfig: String = "{}",
    
    /**
     * 发现页导航配置（嵌套JSON字符串）
     * 存储格式：{
     *   "enabledContentTypes": ["USERS", "ILLUSTS", "NOVELS", "PIXIVISION"],
     *   "illustsModes": ["ALL", "SAFE", "R18"],
     *   "novelsModes": ["ALL", "SAFE", "R18"],
     *   "pixivisionCategories": ["ILLUSTRATION", "MANGA"]
     * }
     */
    val discoveryNavigationConfig: String = "{}",
    
    /**
     * 动态页导航配置（嵌套JSON字符串）
     * 存储格式：{
     *   "enabledContentTypes": ["ILLUSTS", "NOVELS", "WATCH_LIST", "GOOD_P_FRIENDS"],
     *   "watchListTypes": ["MANGA", "NOVELS"]
     * }
     */
    val followLatestNavigationConfig: String = "{}",
    
    /**
     * 设置更新时间
     */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * JSON序列化工具
 */
internal val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 排行榜导航配置JSON模型
 */
@kotlinx.serialization.Serializable
internal data class RankingNavigationConfigJson(
    val enabledContentTypes: List<String> = emptyList(),
    val enabledModesPerContent: Map<String, List<String>> = emptyMap()
)

/**
 * 发现页导航配置JSON模型
 */
@kotlinx.serialization.Serializable
internal data class DiscoveryNavigationConfigJson(
    val enabledContentTypes: List<String> = emptyList(),
    val illustsEnabledModes: List<String> = emptyList(),
    val novelsEnabledModes: List<String> = emptyList(),
    val pixivisionEnabledCategories: List<String> = emptyList()
)

/**
 * 动态页导航配置JSON模型
 */
@kotlinx.serialization.Serializable
internal data class FollowLatestNavigationConfigJson(
    val enabledContentTypes: List<String> = emptyList(),
    val illustsEnabledModes: List<String> = emptyList(),
    val novelsEnabledModes: List<String> = emptyList(),
    val watchListEnabledTypes: List<String> = emptyList()
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
        viewerImageQuality = this.viewerImageQuality.name,
        novelDownloadImageQuality = this.novelDownloadImageQuality.name,
        imageCacheSize = this.imageCacheSize.name,
        clickBookmarkAction = this.clickBookmarkAction.name,
        longPressBookmarkAction = this.longPressBookmarkAction.name,
        staggeredGridColumns = this.staggeredGridColumns,
        baseDownloadPath = this.downloadSettings.baseDownloadPath,
        fileNameMode = this.downloadSettings.fileNameMode.name,
        customFileNameTemplate = this.downloadSettings.customFileNameTemplate,
        defaultStartupTab = this.defaultStartupTab.name,
        lastUsedTab = this.lastUsedTab,
        novelFontSize = this.novelFontSize.name,
        novelTextColor = this.novelTextColor,
        novelBackgroundColor = this.novelBackgroundColor,
        novelBackgroundScheme = this.novelBackgroundScheme.name,
        translationEngine = this.translationEngine.name,
        translationTargetLanguage = this.translationTargetLanguage.name,
        rankingNavigationConfig = json.encodeToString(
            RankingNavigationConfigJson(
                enabledContentTypes = this.rankingNavigationPreferences.enabledContentTypes.toList(),
                enabledModesPerContent = this.rankingNavigationPreferences.enabledModesPerContent.mapValues { it.value.toList() }
            )
        ),
        discoveryNavigationConfig = json.encodeToString(
            DiscoveryNavigationConfigJson(
                enabledContentTypes = this.discoveryNavigationPreferences.enabledContentTypes.toList(),
                illustsEnabledModes = this.discoveryNavigationPreferences.illustsEnabledModes.toList(),
                novelsEnabledModes = this.discoveryNavigationPreferences.novelsEnabledModes.toList(),
                pixivisionEnabledCategories = this.discoveryNavigationPreferences.pixivisionEnabledCategories.toList()
            )
        ),
        followLatestNavigationConfig = json.encodeToString(
            FollowLatestNavigationConfigJson(
                enabledContentTypes = this.followLatestNavigationPreferences.enabledContentTypes.toList(),
                illustsEnabledModes = this.followLatestNavigationPreferences.illustsEnabledModes.toList(),
                novelsEnabledModes = this.followLatestNavigationPreferences.novelsEnabledModes.toList(),
                watchListEnabledTypes = this.followLatestNavigationPreferences.watchListEnabledTypes.toList()
            )
        )
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
        viewerImageQuality = com.projectu.shared.domain.model.ViewerImageQuality.fromName(this.viewerImageQuality),
        novelDownloadImageQuality = com.projectu.shared.domain.model.NovelDownloadImageQuality.fromName(this.novelDownloadImageQuality),
        imageCacheSize = com.projectu.shared.domain.model.CacheSize.fromName(this.imageCacheSize),
        clickBookmarkAction = com.projectu.shared.domain.model.BookmarkAction.valueOf(this.clickBookmarkAction),
        longPressBookmarkAction = com.projectu.shared.domain.model.BookmarkAction.valueOf(this.longPressBookmarkAction),
        staggeredGridColumns = this.staggeredGridColumns,
        downloadSettings = com.projectu.shared.data.local.DownloadSettings(
            baseDownloadPath = this.baseDownloadPath.ifEmpty { com.projectu.shared.data.local.getDefaultDownloadPath() },
            fileNameMode = try {
                com.projectu.shared.data.local.FileNameMode.valueOf(this.fileNameMode)
            } catch (e: Exception) {
                com.projectu.shared.data.local.FileNameMode.STANDARD
            },
            customFileNameTemplate = this.customFileNameTemplate.ifEmpty { "{id}_{p}_{title}" }
        ),
        defaultStartupTab = try {
            com.projectu.shared.data.local.StartupTab.valueOf(this.defaultStartupTab)
        } catch (e: Exception) {
            com.projectu.shared.data.local.StartupTab.LAST_USED
        },
        lastUsedTab = this.lastUsedTab.ifEmpty { "HOME" },
        novelFontSize = com.projectu.shared.data.local.NovelFontSize.fromName(this.novelFontSize),
        novelTextColor = this.novelTextColor,
        novelBackgroundColor = this.novelBackgroundColor,
        novelBackgroundScheme = com.projectu.shared.data.local.NovelBackgroundScheme.fromName(this.novelBackgroundScheme),
        translationEngine = try {
            com.projectu.shared.domain.model.TranslationEngine.valueOf(this.translationEngine)
        } catch (e: Exception) {
            com.projectu.shared.domain.model.TranslationEngine.NONE
        },
        translationTargetLanguage = try {
            com.projectu.shared.domain.model.TranslationLanguage.valueOf(this.translationTargetLanguage)
        } catch (e: Exception) {
            com.projectu.shared.domain.model.TranslationLanguage.SIMPLIFIED_CHINESE
        },
        rankingNavigationPreferences = run {
            try {
                val config = json.decodeFromString<RankingNavigationConfigJson>(this.rankingNavigationConfig)
                // 如果为空，使用默认配置
                if (config.enabledContentTypes.isEmpty() || config.enabledModesPerContent.isEmpty()) {
                    com.projectu.shared.domain.model.RankingNavigationPreferences.DEFAULT
                } else {
                    com.projectu.shared.domain.model.RankingNavigationPreferences(
                        enabledContentTypes = config.enabledContentTypes.toSet(),
                        enabledModesPerContent = config.enabledModesPerContent.mapValues { it.value.toSet() }
                    )
                }
            } catch (e: Exception) {
                com.projectu.shared.domain.model.RankingNavigationPreferences.DEFAULT
            }
        },
        discoveryNavigationPreferences = run {
            try {
                val config = json.decodeFromString<DiscoveryNavigationConfigJson>(this.discoveryNavigationConfig)
                // 如果为空，使用默认配置
                if (config.enabledContentTypes.isEmpty()) {
                    com.projectu.shared.domain.model.DiscoveryNavigationPreferences.DEFAULT
                } else {
                    com.projectu.shared.domain.model.DiscoveryNavigationPreferences(
                        enabledContentTypes = config.enabledContentTypes.toSet(),
                        illustsEnabledModes = config.illustsEnabledModes.toSet(),
                        novelsEnabledModes = config.novelsEnabledModes.toSet(),
                        pixivisionEnabledCategories = config.pixivisionEnabledCategories.toSet()
                    )
                }
            } catch (e: Exception) {
                com.projectu.shared.domain.model.DiscoveryNavigationPreferences.DEFAULT
            }
        },
        followLatestNavigationPreferences = run {
            try {
                val config = json.decodeFromString<FollowLatestNavigationConfigJson>(this.followLatestNavigationConfig)
                // 如果为空，使用默认配置
                if (config.enabledContentTypes.isEmpty()) {
                    com.projectu.shared.domain.model.FollowLatestNavigationPreferences.DEFAULT
                } else {
                    com.projectu.shared.domain.model.FollowLatestNavigationPreferences(
                        enabledContentTypes = config.enabledContentTypes.toSet(),
                        illustsEnabledModes = config.illustsEnabledModes.toSet().ifEmpty { setOf("ALL", "R18") },
                        novelsEnabledModes = config.novelsEnabledModes.toSet().ifEmpty { setOf("ALL", "R18") },
                        watchListEnabledTypes = config.watchListEnabledTypes.toSet()
                    )
                }
            } catch (e: Exception) {
                com.projectu.shared.domain.model.FollowLatestNavigationPreferences.DEFAULT
            }
        }
    )
}
