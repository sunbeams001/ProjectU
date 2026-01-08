package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.TranslationResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Google Translate API（旧版免费接口）
 * 
 * 通过模拟浏览器请求实现免费翻译
 * 
 * 端点：translate.googleapis.com/translate_a/single
 * 方式：HTTP GET 请求
 * 
 * 注意：
 * - 这是非官方接口，Google 可能随时改变
 * - 需要注意请求频率，避免被限制
 * - 建议添加适当的延迟和错误处理
 */
class GoogleTranslateApi(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "https://translate.googleapis.com/translate_a/single"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
    }
    
    /**
     * 翻译文本
     * 
     * @param text 待翻译的文本
     * @param targetLang 目标语言代码（如：zh-CN, en, ja）
     * @param sourceLang 源语言代码（默认：auto，自动检测）
     * @return 翻译结果
     */
    suspend fun translate(
        text: String,
        targetLang: String,
        sourceLang: String = "auto"
    ): Result<TranslationResult> {
        return try {
            val response = httpClient.get(BASE_URL) {
                url {
                    parameters.append("client", "gtx")
                    parameters.append("sl", sourceLang)
                    parameters.append("tl", targetLang)
                    parameters.append("dt", "t")  // t = translation
                    parameters.append("q", text)
                }
                header("User-Agent", USER_AGENT)
                header("Accept", "*/*")
                header("Accept-Language", "en-US,en;q=0.9")
            }
            
            val responseText = response.bodyAsText()
            val result = parseResponse(responseText, targetLang)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解析 Google Translate 响应
     * 
     * 响应格式：
     * [
     *   [
     *     ["翻译结果1", "原文1", null, null, 10],
     *     ["翻译结果2", "原文2", null, null, 10],
     *     ...
     *   ],
     *   null,
     *   "ja",  // 源语言
     *   ...
     * ]
     */
    private fun parseResponse(responseText: String, targetLang: String): TranslationResult {
        val json = Json { ignoreUnknownKeys = true }
        val jsonArray = json.parseToJsonElement(responseText).jsonArray
        
        // 第一个元素是翻译结果数组
        val translationsArray = jsonArray.getOrNull(0)?.jsonArray
            ?: throw IllegalStateException("Invalid response format: missing translations array")
        
        // 拼接所有翻译片段
        val translatedText = translationsArray
            .mapNotNull { it.jsonArray.getOrNull(0)?.jsonPrimitive?.content }
            .joinToString("")
        
        // 第三个元素是源语言（索引为2）
        val sourceLanguage = jsonArray.getOrNull(2)?.jsonPrimitive?.content
        
        return TranslationResult(
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLang
        )
    }
}
