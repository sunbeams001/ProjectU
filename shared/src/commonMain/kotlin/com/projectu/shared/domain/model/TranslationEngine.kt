package com.projectu.shared.domain.model

/**
 * 翻译引擎枚举
 * 支持的翻译服务提供商
 */
enum class TranslationEngine {
    /**
     * 不使用翻译功能
     */
    NONE,
    
    /**
     * Google Translate（免费接口）
     */
    GOOGLE_FREE;
    
    companion object {
        /**
         * 默认翻译引擎
         */
        val DEFAULT = NONE
    }
}
