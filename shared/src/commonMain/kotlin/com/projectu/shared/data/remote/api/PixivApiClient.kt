package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Pixiv API 客户端
 * 基于 Pixiv Web API 实现
 * 
 * @property phpSessionId PHPSESSID cookie值，通过Web端登录后获取
 * @property token POST请求使用的令牌，可选，如果不提供会自动获取
 * @property host API主机地址，默认为 https://www.pixiv.net
 * @property lang 语言设置，默认为中文
 */
class PixivApiClient(
    private val httpClient: HttpClient,
    private val phpSessionId: String,
    private val token: String? = null,
    private val host: String = DEFAULT_HOST,
    private val lang: String = DEFAULT_LANG
) {
    companion object {
        const val DEFAULT_HOST = "https://www.pixiv.net"
        const val DEFAULT_LANG = "zh"
        private const val HEADER_REFERER = "Referer"
        private const val HEADER_COOKIE = "Cookie"
        private const val HEADER_CSRF_TOKEN = "x-csrf-token"
    }

    /**
     * 当前用户ID（从phpSessionId中解析）
     */
    val userId: Long = phpSessionId.split("_")[0].toLong()

    private val cookie: String = "PHPSESSID=$phpSessionId"
    private var csrfToken: String? = token

    /**
     * 执行GET请求
     */
    suspend inline fun <reified T> get(
        url: String,
        queryParams: Map<String, Any?>? = null
    ): PixivResponse<T> {
        return httpClient.get("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            parameter("lang", lang)
            queryParams?.forEach { (key, value) ->
                when (value) {
                    is Collection<*> -> {
                        // 处理数组参数
                        value.forEach { item ->
                            parameter(key, item.toString())
                        }
                    }
                    null -> {
                        // 忽略null值
                    }
                    else -> {
                        parameter(key, value.toString())
                    }
                }
            }
        }.body()
    }

    /**
     * 执行POST请求（JSON body）
     */
    suspend inline fun <reified T> postJson(
        url: String,
        body: Any? = null
    ): PixivResponse<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        return httpClient.post("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
            parameter("lang", lang)
            contentType(ContentType.Application.Json)
            body?.let {
                setBody(it)
            }
        }.body()
    }

    /**
     * 执行POST请求（Form body）
     */
    suspend inline fun <reified T> postForm(
        url: String,
        formParams: Map<String, String>? = null
    ): PixivResponse<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        return httpClient.submitForm(
            url = "$host$url",
            formParameters = Parameters.build {
                append("lang", lang)
                formParams?.forEach { (key, value) ->
                    append(key, value)
                }
            }
        ) {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
        }.body()
    }

    /**
     * 从服务器获取CSRF Token
     */
    private suspend fun fetchToken(): String {
        val html = httpClient.get("$host/setting_user.php") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
        }.body<String>()

        val regex = Regex("""pixiv\.context\.token = "(.+?)";""")
        val matchResult = regex.find(html)
        return matchResult?.groupValues?.get(1)
            ?: throw IllegalStateException("无法获取CSRF Token")
    }

    /**
     * 获取当前的CSRF Token（如果需要的话）
     */
    suspend fun getToken(): String {
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }
        return csrfToken!!
    }
}

