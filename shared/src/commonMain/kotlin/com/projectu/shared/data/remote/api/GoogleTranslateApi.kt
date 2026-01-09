package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.TranslationResult
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val REFERER = "https://translate.google.com/"
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
            // 使用POST + Form方式提交，更不容易被拦截
            val response = httpClient.submitForm(
                url = BASE_URL,
                formParameters = parameters {
                    append("client", "gtx")
                    append("sl", sourceLang)
                    append("tl", targetLang)
                    append("dt", "t")
                    append("ie", "UTF-8")
                    append("oe", "UTF-8")
                    append("q", text)
                }
            ) {
                header("User-Agent", USER_AGENT)
                header("Referer", REFERER)
                header("Accept", "*/*")
                header("Accept-Language", "zh-CN,zh;q=0.9")
                header("Content-Type", "application/x-www-form-urlencoded")
                header("Origin", "https://translate.google.com")
            }
            
            val responseText = response.bodyAsText()
            
            // 检查是否返回了HTML错误页面
            if (responseText.trim().startsWith("<!DOCTYPE") || responseText.trim().startsWith("<html")) {
                throw IllegalStateException("Google Translate API returned HTML error page. Possible rate limiting or blocking.")
            }
            
            val result = parseResponse(responseText, targetLang)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解析 Google Translate 响应
     * 
     * 标准响应格式（不使用dj=1）：
     * [
     *   [
     *     ["翻译结果1", "原文1", null, null, 10],
     *     ["翻译结果2", "原文2", null, null, 10]
     *   ],
     *   null,
     *   "ja"
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
