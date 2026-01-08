package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.GoogleTranslateApi
import com.projectu.shared.domain.model.Translation
import com.projectu.shared.domain.model.TranslationEngine
import com.projectu.shared.domain.model.TranslationLanguage
import com.projectu.shared.domain.repository.TranslationRepository

/**
 * 翻译仓储实现
 */
class TranslationRepositoryImpl(
    private val googleTranslateApi: GoogleTranslateApi
) : TranslationRepository {
    
    override suspend fun translate(
        text: String,
        targetLanguage: TranslationLanguage,
        engine: TranslationEngine,
        sourceLanguage: String?
    ): Result<Translation> {
        return when (engine) {
            TranslationEngine.GOOGLE_FREE -> {
                translateWithGoogle(text, targetLanguage, sourceLanguage)
            }
            TranslationEngine.NONE -> {
                Result.failure(IllegalStateException("Translation engine is not enabled"))
            }
        }
    }
    
    /**
     * 使用 Google Translate 翻译
     */
    private suspend fun translateWithGoogle(
        text: String,
        targetLanguage: TranslationLanguage,
        sourceLanguage: String?
    ): Result<Translation> {
        return try {
            val result = googleTranslateApi.translate(
                text = text,
                targetLang = targetLanguage.code,
                sourceLang = sourceLanguage ?: "auto"
            )
            
            result.map { translationResult ->
                Translation(
                    originalText = text,
                    translatedText = translationResult.translatedText,
                    sourceLanguage = translationResult.sourceLanguage,
                    targetLanguage = translationResult.targetLanguage,
                    engine = TranslationEngine.GOOGLE_FREE
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
