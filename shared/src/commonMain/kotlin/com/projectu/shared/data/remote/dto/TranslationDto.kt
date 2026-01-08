package com.projectu.shared.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Google Translate 响应 DTO
 * 
 * 旧版免费接口返回的是一个复杂的嵌套数组结构
 * 格式示例：
 * [
 *   [
 *     ["翻译结果", "原文", null, null, 10],
 *     ...
 *   ],
 *   null,
 *   "ja",  // 源语言
 *   ...
 * ]
 */
@Serializable
data class GoogleTranslateResponse(
    val translations: List<List<String>>? = null,
    val sourceLanguage: String? = null
)

/**
 * 翻译结果 DTO（内部使用）
 */
data class TranslationResult(
    val translatedText: String,
    val sourceLanguage: String?,
    val targetLanguage: String
)
