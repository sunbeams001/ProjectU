package com.projectu.shared.domain.model

/**
 * 翻译语言
 * 支持的翻译目标语言列表
 */
enum class TranslationLanguage(
    val code: String,
    val displayNameKey: String
) {
    /**
     * 简体中文
     */
    SIMPLIFIED_CHINESE("zh-CN", "translation_lang_zh_cn"),
    
    /**
     * 繁体中文
     */
    TRADITIONAL_CHINESE("zh-TW", "translation_lang_zh_tw"),
    
    /**
     * 英语
     */
    ENGLISH("en", "translation_lang_en"),
    
    /**
     * 日语
     */
    JAPANESE("ja", "translation_lang_ja"),
    
    /**
     * 韩语
     */
    KOREAN("ko", "translation_lang_ko"),
    
    /**
     * 法语
     */
    FRENCH("fr", "translation_lang_fr"),
    
    /**
     * 德语
     */
    GERMAN("de", "translation_lang_de"),
    
    /**
     * 西班牙语
     */
    SPANISH("es", "translation_lang_es"),
    
    /**
     * 意大利语
     */
    ITALIAN("it", "translation_lang_it"),
    
    /**
     * 俄语
     */
    RUSSIAN("ru", "translation_lang_ru"),
    
    /**
     * 葡萄牙语
     */
    PORTUGUESE("pt", "translation_lang_pt"),
    
    /**
     * 泰语
     */
    THAI("th", "translation_lang_th"),
    
    /**
     * 越南语
     */
    VIETNAMESE("vi", "translation_lang_vi"),
    
    /**
     * 印度尼西亚语
     */
    INDONESIAN("id", "translation_lang_id"),
    
    /**
     * 马来语
     */
    MALAY("ms", "translation_lang_ms");
    
    companion object {
        /**
         * 默认目标语言
         * 根据Pixiv用户群体，默认为简体中文
         */
        val DEFAULT = SIMPLIFIED_CHINESE
        
        /**
         * 从代码获取语言
         */
        fun fromCode(code: String): TranslationLanguage {
            return entries.find { it.code == code } ?: DEFAULT
        }
    }
}
