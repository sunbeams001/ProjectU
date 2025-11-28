package com.projectu.shared.data.local

import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.CacheSize

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
     * 图片磁盘缓存大小
     * 用于控制 Coil 图片缓存的磁盘空间占用
     * 默认：MEDIUM (512MB)
     */
    val imageCacheSize: CacheSize = CacheSize.DEFAULT
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

