package com.projectu.shared.data.local

import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.ViewerImageQuality
import com.projectu.shared.domain.model.NovelDownloadImageQuality
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.BookmarkAction

/**
 * 应用设置数据模型
 * 存储应用的各项配置信息
 */
data class AppSettings(
    /**
     * 应用界面语言
     * 支持：zh-CN(简体中文), zh-TW(繁体中文), en(英文), ja(日文), ko(韩文)
     */
    val appLanguage: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    
    /**
     * Pixiv API 语言偏好
     * 用于从 Pixiv 获取数据时的语言设置
     */
    val pixivLanguage: PixivLanguage = PixivLanguage.SIMPLIFIED_CHINESE,
    
    /**
     * 主题设置
     * 支持：LIGHT(浅色), DARK(深色), SYSTEM(跟随系统)
     */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    
    /**
     * R18 Sanity Level 阈值
     * 当作品的 Sanity Level 达到或超过该值时，将被视为 R18 内容
     * 
     * 参考值：
     * - 2: 一般作品
     * - 4: 包含轻度裸露
     * - 6: R18 作品（默认）
     * 
     * 范围：0-9
     */
    val r18SanityLevelThreshold: Int = 6,
    
    /**
     * 插画卡片首选图片质量
     * 用于列表、瀑布流等场景的缩略图显示
     * 默认：SQUARE_MEDIUM (250x250)
     */
    val preferredImageQuality: ImageQuality = ImageQuality.SQUARE_MEDIUM,
    
    /**
     * 插画详情页首选图片质量
     * 用于作品详情页的图片显示
     * 默认：LARGE (540x540)
     */
    val detailImageQuality: DetailImageQuality = DetailImageQuality.LARGE,
    
    /**
     * 作品大图浏览页首选图片质量
     * 用于全屏查看作品时的图片显示
     * 默认：MASTER_1200 (1200px，平衡质量与速度)
     */
    val viewerImageQuality: ViewerImageQuality = ViewerImageQuality.MASTER_1200,
    
    /**
     * 小说下载首选图片质量
     * 用于下载小说为 EPUB 时，内嵌图片的质量
     * 默认：LARGE (1200px)
     */
    val novelDownloadImageQuality: NovelDownloadImageQuality = NovelDownloadImageQuality.LARGE,
    
    /**
     * 图片磁盘缓存大小
     * 用于控制 Coil 图片缓存的磁盘空间占用
     * 默认：MEDIUM (512MB)
     */
    val imageCacheSize: CacheSize = CacheSize.DEFAULT,
    
    /**
     * 点击收藏按钮的行为
     * 默认：PUBLIC (添加公开收藏)
     */
    val clickBookmarkAction: BookmarkAction = BookmarkAction.PUBLIC,
    
    /**
     * 长按收藏按钮的行为
     * 默认：PRIVATE (添加私人收藏)
     */
    val longPressBookmarkAction: BookmarkAction = BookmarkAction.PRIVATE,
    
    /**
     * 瀑布流列数
     * 用于作品列表、排行榜、搜索等瀑布流布局的列数
     * 范围：2-5，默认：3
     */
    val staggeredGridColumns: Int = 3,
    
    /**
     * 下载设置
     * 包含下载路径、文件命名等配置
     */
    val downloadSettings: DownloadSettings = DownloadSettings.DEFAULT,
    
    /**
     * 默认启动Tab页面
     * 用于控制App启动后默认打开的Tab页面
     * 默认：LAST_USED (上次退出的页面)
     */
    val defaultStartupTab: StartupTab = StartupTab.LAST_USED,
    
    /**
     * 最后使用的Tab页面
     * 用于记录上次退出App时所在的Tab
     * 默认：HOME (搜索页面)
     */
    val lastUsedTab: String = "HOME",
    
    /**
     * 小说阅读字号
     * 支持：SMALL(14sp), MEDIUM(16sp), LARGE(18sp), EXTRA_LARGE(20sp), HUGE(22sp)
     * 默认：MEDIUM
     */
    val novelFontSize: NovelFontSize = NovelFontSize.MEDIUM,
    
    /**
     * 小说阅读文字颜色（十六进制，如 "#000000"）
     * 默认：null（使用主题默认颜色）
     */
    val novelTextColor: String? = null,
    
    /**
     * 小说阅读背景色（十六进制，如 "#FFFFFF"）
     * 默认：null（使用主题默认颜色）
     */
    val novelBackgroundColor: String? = null,
    
    /**
     * 小说阅读背景色方案（预设方案）
     * 支持多种预设方案
     * 默认：THEME_DEFAULT（跟随主题）
     */
    val novelBackgroundScheme: NovelBackgroundScheme = NovelBackgroundScheme.THEME_DEFAULT,
    
    /**
     * 翻译引擎
     * 用于翻译作品简介、小说内容等
     * 默认：NONE（不使用翻译）
     */
    val translationEngine: com.projectu.shared.domain.model.TranslationEngine = com.projectu.shared.domain.model.TranslationEngine.NONE,
    
    /**
     * 翻译目标语言
     * 用于指定翻译的目标语言
     * 默认：简体中文
     */
    val translationTargetLanguage: com.projectu.shared.domain.model.TranslationLanguage = com.projectu.shared.domain.model.TranslationLanguage.SIMPLIFIED_CHINESE,
    
    /**
     * 排行榜导航偏好配置
     * 控制排行榜页面显示哪些导航项
     * 默认：全部启用
     */
    val rankingNavigationPreferences: com.projectu.shared.domain.model.RankingNavigationPreferences = com.projectu.shared.domain.model.RankingNavigationPreferences.DEFAULT,
    
    /**
     * 发现页导航偏好配置
     * 控制发现页面显示哪些导航项
     * 默认：全部启用
     */
    val discoveryNavigationPreferences: com.projectu.shared.domain.model.DiscoveryNavigationPreferences = com.projectu.shared.domain.model.DiscoveryNavigationPreferences.DEFAULT,
    
    /**
     * 动态页导航偏好配置
     * 控制动态页面显示哪些导航项
     * 默认：全部启用
     */
    val followLatestNavigationPreferences: com.projectu.shared.domain.model.FollowLatestNavigationPreferences = com.projectu.shared.domain.model.FollowLatestNavigationPreferences.DEFAULT
) {
    companion object {
        /**
         * 默认设置
         */
        val DEFAULT = AppSettings()
    }
}

/**
 * 应用界面语言枚举
 */
enum class AppLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어");
    
    companion object {
        /**
         * 从语言代码获取枚举
         */
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: SIMPLIFIED_CHINESE
        }
    }
}

/**
 * Pixiv API 语言枚举
 * 支持简体中文、繁体中文、英语、日语、韩语、泰语、马来语
 */
enum class PixivLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh", "简体中文"),
    TRADITIONAL_CHINESE("zh_tw", "繁體中文"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    THAI("th", "ภาษาไทย"),
    MALAY("ms", "Bahasa Melayu");
    
    companion object {
        /**
         * 从语言代码获取枚举
         */
        fun fromCode(code: String): PixivLanguage {
            return values().find { it.code == code } ?: SIMPLIFIED_CHINESE
        }
    }
}

/**
 * 主题模式枚举
 */
enum class ThemeMode {
    LIGHT,      // 浅色主题
    DARK,       // 深色主题
    SYSTEM      // 跟随系统
}

/**
 * 启动Tab页面枚举
 */
enum class StartupTab(val displayNameKey: String) {
    LAST_USED("startup_tab_last_used"),      // 上次退出的页面
    HOME("startup_tab_home"),                // 搜索页面
    DISCOVERY("startup_tab_discovery"),      // 发现页面
    FOLLOW_LATEST("startup_tab_follow_latest"), // 追更页面
    RANKING("startup_tab_ranking"),          // 排行榜页面
    PROFILE("startup_tab_profile");          // 个人页面
}

/**
 * 小说字号枚举
 */
enum class NovelFontSize(val sp: Int, val displayNameKey: String) {
    SMALL(14, "novel_font_size_small"),           // 小
    MEDIUM(16, "novel_font_size_medium"),         // 中（默认）
    LARGE(18, "novel_font_size_large"),           // 大
    EXTRA_LARGE(20, "novel_font_size_extra_large"), // 特大
    HUGE(22, "novel_font_size_huge");              // 超大
    
    companion object {
        fun fromName(name: String): NovelFontSize {
            return values().find { it.name == name } ?: MEDIUM
        }
    }
}

/**
 * 小说背景色方案枚举
 */
enum class NovelBackgroundScheme(
    val displayNameKey: String,
    val backgroundColor: String?,  // 十六进制颜色，null表示使用主题默认
    val textColor: String?         // 推荐的文字颜色，null表示使用主题默认
) {
    THEME_DEFAULT(
        "novel_bg_theme_default",
        null,
        null
    ),
    PAPER_WHITE(
        "novel_bg_paper_white",
        "#FFFFFF",
        "#000000"
    ),
    EYE_CARE_GREEN(
        "novel_bg_eye_care_green",
        "#CCE8CC",  // 护眼绿
        "#000000"
    ),
    WARM_YELLOW(
        "novel_bg_warm_yellow",
        "#FFF9E6",  // 暖黄色
        "#333333"
    ),
    CLASSIC_BEIGE(
        "novel_bg_classic_beige",
        "#F5E6D3",  // 经典米色
        "#333333"
    ),
    NIGHT_BLACK(
        "novel_bg_night_black",
        "#1A1A1A",  // 夜间模式
        "#CCCCCC"
    ),
    CUSTOM(
        "novel_bg_custom",
        null,  // 使用用户自定义的颜色
        null
    );
    
    companion object {
        fun fromName(name: String): NovelBackgroundScheme {
            return values().find { it.name == name } ?: THEME_DEFAULT
        }
    }
}
