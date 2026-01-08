package com.projectu.shared.domain.model

/**
 * 翻译结果领域模型
 */
data class Translation(
    /**
     * 原始文本
     */
    val originalText: String,
    
    /**
     * 翻译后的文本
     */
    val translatedText: String,
    
    /**
     * 源语言（自动检测）
     */
    val sourceLanguage: String? = null,
    
    /**
     * 目标语言
     */
    val targetLanguage: String,
    
    /**
     * 使用的翻译引擎
     */
    val engine: TranslationEngine
)
