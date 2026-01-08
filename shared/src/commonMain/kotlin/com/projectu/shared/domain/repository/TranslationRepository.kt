package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Translation
import com.projectu.shared.domain.model.TranslationEngine
import com.projectu.shared.domain.model.TranslationLanguage

/**
 * 翻译仓储接口
 */
interface TranslationRepository {
    /**
     * 翻译文本
     * 
     * @param text 待翻译的文本
     * @param targetLanguage 目标语言
     * @param engine 翻译引擎
     * @param sourceLanguage 源语言（可选，默认自动检测）
     * @return 翻译结果，失败时返回 null
     */
    suspend fun translate(
        text: String,
        targetLanguage: TranslationLanguage,
        engine: TranslationEngine,
        sourceLanguage: String? = null
    ): Result<Translation>
}
