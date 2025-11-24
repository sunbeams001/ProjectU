package com.projectu.shared.util

import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Tag

/**
 * 标签翻译工具类
 * 
 * 用于从 tagTranslation 结构中获取翻译后的标签
 * 
 * tagTranslation 数据结构示例：
 * ```
 * {
 *   "寝取られ": {
 *     "en": "cuckolding",
 *     "ko": "네토라레",
 *     "zh": "NTR",
 *     "zh_tw": "NTR",
 *     "romaji": "netorare"
 *   }
 * }
 * ```
 * 
 * @property settingsCache 设置缓存，用于获取当前 Pixiv 语言偏好
 */
class TagTranslationUtil(
    private val settingsCache: SettingsCache
) {
    
    /**
     * 支持的语言代码
     */
    private object SupportedLang {
        const val ZH = "zh"          // 简体中文
        const val ZH_TW = "zh_tw"    // 繁体中文
        const val EN = "en"          // 英语
        const val JA = "ja"          // 日语
        const val KO = "ko"          // 韩语
        const val ROMAJI = "romaji"  // 罗马音
    }
    
    /**
     * 翻译单个标签
     * 
     * @param originalTag 原始标签（通常是日文）
     * @param tagTranslation 标签翻译映射表
     * @return Tag 对象，包含原始标签和翻译
     * 
     * 注意：
     * - 自动从配置中获取目标语言
     * - 如果目标语言是泰语(th)或马来语(ms)等新语言，会自动降级到英语(en)
     * - 如果翻译为空字符串或不存在，translatedName 为 null
     */
    fun translateTag(
        originalTag: String,
        tagTranslation: Map<String, Map<String, String>>?
    ): Tag {
        val translated = getTranslationText(originalTag, tagTranslation)
        return Tag(
            name = originalTag,
            translatedName = translated
        )
    }
    
    /**
     * 批量翻译标签
     * 
     * @param originalTags 原始标签列表（通常是日文）
     * @param tagTranslation 标签翻译映射表
     * @return Tag 对象列表，与输入列表顺序一致
     */
    fun translateTags(
        originalTags: List<String>,
        tagTranslation: Map<String, Map<String, String>>?
    ): List<Tag> {
        if (originalTags.isEmpty()) {
            return emptyList()
        }
        
        return originalTags.map { tag ->
            translateTag(tag, tagTranslation)
        }
    }
    
    /**
     * 获取翻译文本（内部方法）
     * 
     * @param originalTag 原始标签
     * @param tagTranslation 标签翻译映射表
     * @return 翻译文本，如果无翻译则返回 null
     */
    private fun getTranslationText(
        originalTag: String,
        tagTranslation: Map<String, Map<String, String>>?
    ): String? {
        // tagTranslation 为 null 或空，直接返回 null
        if (tagTranslation.isNullOrEmpty()) {
            return null
        }
        
        // 获取该标签的翻译映射
        val translations = tagTranslation[originalTag] ?: return null
        
        // 从 SettingsCache 获取当前 Pixiv 语言偏好
        val targetLang = settingsCache.getPixivLanguageCode()
        
        // 标准化目标语言代码
        val normalizedLang = normalizeLangCode(targetLang)
        
        // 尝试获取目标语言的翻译
        val translation = translations[normalizedLang]
        
        // 如果翻译存在且不为空，返回翻译
        if (!translation.isNullOrBlank()) {
            return translation
        }
        
        // 如果目标语言不是英语，且没有翻译，尝试降级到英语
        if (normalizedLang != SupportedLang.EN) {
            val enTranslation = translations[SupportedLang.EN]
            if (!enTranslation.isNullOrBlank()) {
                return enTranslation
            }
        }
        
        // 所有尝试都失败，返回 null
        return null
    }
    
    /**
     * 标准化语言代码
     * 
     * 将不支持的新语言（如泰语、马来语）映射到英语
     */
    private fun normalizeLangCode(langCode: String): String {
        return when (langCode.lowercase()) {
            SupportedLang.ZH, "zh-cn", "zh_cn" -> SupportedLang.ZH
            SupportedLang.ZH_TW, "zh-tw", "zh_hk" -> SupportedLang.ZH_TW
            SupportedLang.EN, "en-us", "en_us" -> SupportedLang.EN
            SupportedLang.JA, "ja-jp", "ja_jp" -> SupportedLang.JA
            SupportedLang.KO, "ko-kr", "ko_kr" -> SupportedLang.KO
            SupportedLang.ROMAJI -> SupportedLang.ROMAJI
            // 新语言（泰语、马来语等）降级到英语
            "th", "th-th", "ms", "ms-my", "id", "id-id", "vi", "vi-vn" -> SupportedLang.EN
            else -> SupportedLang.EN  // 默认使用英语
        }
    }
}
