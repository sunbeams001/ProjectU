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
            // Google翻译免费API对文本长度有限制，需要分块翻译
            val maxChunkSize = 4500 // 保守估计，留出URL编码空间
            
            if (text.length <= maxChunkSize) {
                // 文本较短，直接翻译
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
            } else {
                // 文本较长，分块翻译
                
                // 按句子分块，尽量保持语义完整
                val chunks = splitTextIntoChunks(text, maxChunkSize)
                
                val translatedChunks = mutableListOf<String>()
                var detectedSourceLang: String? = null
                
                for ((index, chunk) in chunks.withIndex()) {
                    
                    val result = googleTranslateApi.translate(
                        text = chunk,
                        targetLang = targetLanguage.code,
                        sourceLang = sourceLanguage ?: "auto"
                    )
                    
                    result.fold(
                        onSuccess = { translationResult ->
                            translatedChunks.add(translationResult.translatedText)
                            if (detectedSourceLang == null) {
                                detectedSourceLang = translationResult.sourceLanguage
                            }
                            
                            // 🆕 每翻译完一块，立即返回进度回调
                            // 注意：这里只是累积，实际进度回调需要在UseCase层实现
                        },
                        onFailure = { error ->
                            throw error
                        }
                    )
                    
                    // 添加延迟避免请求过快被封禁
                    if (index < chunks.size - 1) {
                        kotlinx.coroutines.delay(500)  // 增加到500ms
                    }
                }
                
                val finalTranslation = translatedChunks.joinToString("")
                
                Result.success(
                    Translation(
                        originalText = text,
                        translatedText = finalTranslation,
                        sourceLanguage = detectedSourceLang ?: "unknown",
                        targetLanguage = targetLanguage.code,
                        engine = TranslationEngine.GOOGLE_FREE
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 将长文本分割成多个块
     * 尽量按句子边界分割，保持语义完整
     */
    private fun splitTextIntoChunks(text: String, maxChunkSize: Int): List<String> {
        return com.projectu.shared.util.TextSplitter.splitIntoChunks(text, maxChunkSize)
    }
}
