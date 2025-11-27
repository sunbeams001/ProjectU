package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.common.PixivResponseWithRaw
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
 * @property langProvider 语言提供者函数，动态获取当前语言设置
 * @property onTokenUpdated CSRF token更新回调，用于持久化保存
 */
class PixivApiClient(
    @PublishedApi
    internal val httpClient: HttpClient,
    private val phpSessionId: String,
    private val token: String? = null,
    @PublishedApi
    internal val host: String = DEFAULT_HOST,
    @PublishedApi
    internal val langProvider: () -> String = { DEFAULT_LANG },
    private val onTokenUpdated: (suspend (String) -> Unit)? = null
) {
    companion object {
        const val DEFAULT_HOST = "https://www.pixiv.net"
        const val DEFAULT_LANG = "zh"
        const val HEADER_REFERER = "Referer"
        const val HEADER_COOKIE = "Cookie"
        const val HEADER_CSRF_TOKEN = "x-csrf-token"
        
        /**
         * 用于解析原始JSON的Json实例（需要忽略未知字段）
         * @suppress - Internal use only for inline functions
         */
        @PublishedApi
        internal val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    /**
     * 当前用户ID（从phpSessionId中解析）
     */
    val userId: Long = phpSessionId.split("_")[0].toLong()

    @PublishedApi
    internal val cookie: String = "PHPSESSID=$phpSessionId"
    
    @PublishedApi
    internal var csrfToken: String? = token

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
            parameter("lang", langProvider())
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
     * 执行GET请求（带原始JSON）- 用于API测试
     */
    suspend inline fun <reified T> getWithRaw(
        url: String,
        queryParams: Map<String, Any?>? = null
    ): PixivResponseWithRaw<T> {
        val httpResponse = httpClient.get("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            parameter("lang", langProvider())
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
        }
        val rawJson = httpResponse.bodyAsText()
        val response = jsonParser.decodeFromString<PixivResponse<T>>(rawJson)
        return PixivResponseWithRaw(response, rawJson)
    }

    /**
     * 执行GET请求（直接返回解析后的对象，不包装在PixivResponse中）
     * 用于不遵循标准响应格式的API（如 /rpc/cps.php）
     */
    suspend inline fun <reified T> getRaw(
        url: String,
        queryParams: Map<String, Any?>? = null
    ): T {
        val httpResponse = httpClient.get("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            // 添加完整的浏览器请求头以绕过 Cloudflare 验证（精确匹配 Chrome 142 真实请求）
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            header("Accept", "*/*")
            header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7,zh-TW;q=0.6,ja;q=0.5,ru;q=0.4")
            header("Priority", "u=1, i")
            header("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"")
            header("Sec-Ch-Ua-Mobile", "?0")
            header("Sec-Ch-Ua-Platform", "\"Windows\"")
            header("Sec-Fetch-Dest", "empty")
            header("Sec-Fetch-Mode", "cors")
            header("Sec-Fetch-Site", "same-origin")
            parameter("lang", langProvider())
            queryParams?.forEach { (key, value) ->
                when (value) {
                    is Collection<*> -> {
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
        }
        
        return httpResponse.body()
    }

    /**
     * 执行GET请求（返回HTML）- 用于旧的PHP页面
     */
    suspend fun getHtml(
        url: String,
        params: Map<String, Any?>? = null
    ): String {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        val httpResponse = httpClient.get("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
            parameter("lang", langProvider())
            params?.forEach { (key, value) ->
                value?.let { parameter(key, it) }
            }
        }
        
        return httpResponse.bodyAsText()
    }

    /**
     * 执行POST请求（JSON body）
     * 自动处理 CSRF token 过期问题：如果遇到 403 错误，会刷新 token 并重试一次
     */
    suspend inline fun <reified T, reified B> postJson(
        url: String,
        body: B
    ): PixivResponse<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        return try {
            httpClient.post("$host$url") {
                header(HEADER_REFERER, DEFAULT_HOST)
                header(HEADER_COOKIE, cookie)
                header(HEADER_CSRF_TOKEN, csrfToken)
                parameter("lang", langProvider())
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: Exception) {
            // 如果是403错误（可能是token过期），刷新token并重试
            if (e.message?.contains("403") == true) {
                println("⚠️ POST请求遇到403错误，刷新CSRF token并重试...")
                csrfToken = fetchToken()
                httpClient.post("$host$url") {
                    header(HEADER_REFERER, DEFAULT_HOST)
                    header(HEADER_COOKIE, cookie)
                    header(HEADER_CSRF_TOKEN, csrfToken)
                    parameter("lang", langProvider())
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()
            } else {
                throw e
            }
        }
    }
    
    /**
     * 执行POST请求（JSON body，无body参数）
     * 自动处理 CSRF token 过期问题：如果遇到 403 错误，会刷新 token 并重试一次
     */
    suspend inline fun <reified T> postJson(
        url: String
    ): PixivResponse<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        return try {
            httpClient.post("$host$url") {
                header(HEADER_REFERER, DEFAULT_HOST)
                header(HEADER_COOKIE, cookie)
                header(HEADER_CSRF_TOKEN, csrfToken)
                parameter("lang", langProvider())
                contentType(ContentType.Application.Json)
            }.body()
        } catch (e: Exception) {
            // 如果是403错误（可能是token过期），刷新token并重试
            if (e.message?.contains("403") == true) {
                println("⚠️ POST请求遇到403错误，刷新CSRF token并重试...")
                csrfToken = fetchToken()
                httpClient.post("$host$url") {
                    header(HEADER_REFERER, DEFAULT_HOST)
                    header(HEADER_COOKIE, cookie)
                    header(HEADER_CSRF_TOKEN, csrfToken)
                    parameter("lang", langProvider())
                    contentType(ContentType.Application.Json)
                }.body()
            } else {
                throw e
            }
        }
    }

    /**
     * 执行POST请求（JSON body，带原始JSON）- 用于API测试
     */
    suspend inline fun <reified T, reified B> postJsonWithRaw(
        url: String,
        body: B
    ): PixivResponseWithRaw<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        val httpResponse = httpClient.post("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
            parameter("lang", langProvider())
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val rawJson = httpResponse.bodyAsText()
        val response = jsonParser.decodeFromString<PixivResponse<T>>(rawJson)
        return PixivResponseWithRaw(response, rawJson)
    }
    
    /**
     * 执行POST请求（JSON body，带原始JSON，无body）- 用于API测试
     */
    suspend inline fun <reified T> postJsonWithRaw(
        url: String
    ): PixivResponseWithRaw<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        val httpResponse = httpClient.post("$host$url") {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
            parameter("lang", langProvider())
            contentType(ContentType.Application.Json)
        }
        val rawJson = httpResponse.bodyAsText()
        val response = jsonParser.decodeFromString<PixivResponse<T>>(rawJson)
        return PixivResponseWithRaw(response, rawJson)
    }

    /**
     * 执行POST请求（Form body）
     * 自动处理 CSRF token 过期问题：如果遇到 403 错误，会刷新 token 并重试一次
     */
    suspend inline fun <reified T> postForm(
        url: String,
        formParams: Map<String, String>? = null
    ): PixivResponse<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        return try {
            httpClient.submitForm(
                url = "$host$url",
                formParameters = Parameters.build {
                    append("lang", langProvider())
                    formParams?.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            ) {
                header(HEADER_REFERER, DEFAULT_HOST)
                header(HEADER_COOKIE, cookie)
                header(HEADER_CSRF_TOKEN, csrfToken)
            }.body()
        } catch (e: Exception) {
            // 如果是403错误（可能是token过期），刷新token并重试
            if (e.message?.contains("403") == true) {
                println("⚠️ POST请求遇到403错误，刷新CSRF token并重试...")
                csrfToken = fetchToken()
                httpClient.submitForm(
                    url = "$host$url",
                    formParameters = Parameters.build {
                        append("lang", langProvider())
                        formParams?.forEach { (key, value) ->
                            append(key, value)
                        }
                    }
                ) {
                    header(HEADER_REFERER, DEFAULT_HOST)
                    header(HEADER_COOKIE, cookie)
                    header(HEADER_CSRF_TOKEN, csrfToken)
                }.body()
            } else {
                throw e
            }
        }
    }

    /**
     * 执行POST请求（Form body，带原始JSON）- 用于API测试
     */
    suspend inline fun <reified T> postFormWithRaw(
        url: String,
        formParams: Map<String, String>? = null
    ): PixivResponseWithRaw<T> {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        val httpResponse = httpClient.submitForm(
            url = "$host$url",
            formParameters = Parameters.build {
                append("lang", langProvider())
                formParams?.forEach { (key, value) ->
                    append(key, value)
                }
            }
        ) {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
        }
        val rawJson = httpResponse.bodyAsText()
        val response = jsonParser.decodeFromString<PixivResponse<T>>(rawJson)
        return PixivResponseWithRaw(response, rawJson)
    }

    /**
     * 执行POST请求（Form body）- 用于旧的API端点，直接返回原始响应
     * 这些端点不返回标准的 PixivResponse 格式，而是直接返回数据或空数组
     */
    suspend inline fun <reified T> postFormRaw(
        url: String,
        formParams: Map<String, String>? = null
    ): T {
        // 确保有token
        if (csrfToken == null) {
            csrfToken = fetchToken()
        }

        val httpResponse = httpClient.submitForm(
            url = "$host$url",
            formParameters = Parameters.build {
                append("lang", langProvider())
                formParams?.forEach { (key, value) ->
                    append(key, value)
                }
            }
        ) {
            header(HEADER_REFERER, DEFAULT_HOST)
            header(HEADER_COOKIE, cookie)
            header(HEADER_CSRF_TOKEN, csrfToken)
        }
        
        val rawJson = httpResponse.bodyAsText()
        return jsonParser.decodeFromString<T>(rawJson)
    }

    /**
     * 从服务器获取CSRF Token
     */
    @PublishedApi
    internal suspend fun fetchToken(): String {
        val url = "$host/settings/account"
        
        val response = httpClient.get(url) {
            header(HEADER_REFERER, host)
            header(HEADER_COOKIE, cookie)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
            header("Connection", "keep-alive")
            header("Upgrade-Insecure-Requests", "1")
            header("Sec-Fetch-Dest", "document")
            header("Sec-Fetch-Mode", "navigate")
            header("Sec-Fetch-Site", "same-origin")
        }
        
        val html = response.bodyAsText(Charsets.UTF_8)
        
        // 匹配 JSON 字符串中被转义的 token
        val regex = Regex("""api\\":\{\\"token\\":\\"([a-f0-9]+)\\"""")
        val matchResult = regex.find(html)
        
        val token = matchResult?.groupValues?.get(1)
            ?: throw IllegalStateException("无法获取CSRF Token")
        
        // 保存 token 到存储
        onTokenUpdated?.invoke(token)
        
        return token
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

