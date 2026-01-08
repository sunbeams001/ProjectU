package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.Translation
import com.projectu.shared.domain.model.TranslationEngine
import com.projectu.shared.domain.model.TranslationLanguage
import com.projectu.shared.domain.repository.TranslationRepository

/**
 * 翻译文本用例
 * 
 * 用于翻译作品简介、小说内容等文本
 */
class TranslateTextUseCase(
    private val translationRepository: TranslationRepository
) {
    /**
     * 执行翻译
     * 
     * @param text 待翻译的文本
     * @param targetLanguage 目标语言
     * @param engine 翻译引擎
     * @param sourceLanguage 源语言（可选，默认自动检测）
     * @return 翻译结果
     */
    suspend operator fun invoke(
        text: String,
        targetLanguage: TranslationLanguage,
        engine: TranslationEngine,
        sourceLanguage: String? = null
    ): Result<Translation> {
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Text to translate cannot be empty"))
        }
        
        if (engine == TranslationEngine.NONE) {
            return Result.failure(IllegalStateException("Translation engine is not enabled"))
        }
        
        return translationRepository.translate(
            text = text,
            targetLanguage = targetLanguage,
            engine = engine,
            sourceLanguage = sourceLanguage
        )
    }
}
