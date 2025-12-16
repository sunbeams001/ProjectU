# HTTP 错误处理机制参考

> 📅 创建日期: 2025-12-16  
> 📦 适用版本: v0.1.0-alpha  
> 🔗 相关文件: [PixivApiClient.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/PixivApiClient.kt)

---

## 📖 概述

本文档详细说明 ProjectU 项目中针对 HTTP 错误（特别是 403 Forbidden）和 Cloudflare 验证的处理机制。所有处理逻辑集中在 `PixivApiClient` 类中。

---

## 🚨 一、403 错误处理机制

### 1.1 处理的 HTTP 方法

**✅ 包含 403 处理的方法（仅 POST 请求）：**

| 方法 | 位置 | 说明 |
|------|------|------|
| `postJson<T, B>(url, body)` | 第 247-286 行 | POST 请求（JSON body，有参数） |
| `postJson<T>(url)` | 第 288-327 行 | POST 请求（JSON body，无参数） |
| `postForm<T>(url, formParams)` | 第 374-421 行 | POST 请求（Form body） |

**❌ 不包含 403 处理的方法：**

| 方法 | 类型 | Cloudflare 处理 | 原因 |
|------|------|----------------|------|
| `get<T>()` | GET | ✅ **完整** | 所有 GET 请求都包含完整浏览器请求头 |
| `getWithRaw<T>()` | GET | ❌ | 不需要 CSRF token |
| `getRaw<T>()` | GET | ✅ **完整** | 专用于旧 PHP 端点 |
| `getRawWithJson<T>()` | GET | ✅ **完整** | API 测试专用 |
| `getHtml()` | GET | ❌ | 不需要 CSRF token |
| `postJsonWithRaw<T, B>()` | POST | ❌ | API 测试专用，故意不处理 |
| `postFormWithRaw<T>()` | POST | ❌ | API 测试专用，故意不处理 |
| `postFormRaw<T>()` | POST | ❌ | 旧 API，非标准响应 |

### 1.2 处理逻辑详解

#### 核心机制

```kotlin
try {
    // 1. 确保有 CSRF token
    if (csrfToken == null) {
        csrfToken = fetchToken()
    }
    
    // 2. 发送 POST 请求
    httpClient.post("$host$url") {
        header(HEADER_REFERER, DEFAULT_HOST)
        header(HEADER_COOKIE, cookie)
        header(HEADER_CSRF_TOKEN, csrfToken)  // 添加 CSRF token
        parameter("lang", langProvider())
        contentType(ContentType.Application.Json)
        setBody(body)
    }.body()
    
} catch (e: Exception) {
    // 3. 检测 403 错误
    if (e.message?.contains("403") == true) {
        println("⚠️ POST请求遇到403错误，刷新CSRF token并重试...")
        
        // 4. 刷新 CSRF token
        csrfToken = fetchToken()
        
        // 5. 使用新 token 自动重试一次
        httpClient.post("$host$url") {
            header(HEADER_CSRF_TOKEN, csrfToken)  // 使用新 token
            // ... 相同的请求配置
        }.body()
    } else {
        throw e  // 其他错误直接抛出
    }
}
```

#### 处理流程图

```
POST 请求
    ↓
检查 csrfToken
    ↓
[无] → fetchToken() → 获取新 token
    ↓
[有] → 发送请求
    ↓
成功? → [是] → 返回结果
    ↓
   [否]
    ↓
异常信息包含 "403"?
    ↓
[否] → 抛出原始异常
    ↓
[是] → 输出日志 "⚠️ POST请求遇到403错误，刷新CSRF token并重试..."
    ↓
调用 fetchToken()
    ↓
保存新 token (onTokenUpdated)
    ↓
使用新 token 重试请求
    ↓
返回结果（或抛出异常）
```

### 1.3 CSRF Token 获取机制

#### fetchToken() 方法

```kotlin
@PublishedApi
internal suspend fun fetchToken(): String {
    val url = "$host/settings/account"
    
    // 1. 访问设置页面获取 token
    val response = httpClient.get(url) {
        header(HEADER_REFERER, host)
        header(HEADER_COOKIE, cookie)
        header("User-Agent", "Mozilla/5.0 ... Chrome/142.0.0.0 Safari/537.36")
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
        header("Connection", "keep-alive")
        header("Upgrade-Insecure-Requests", "1")
        header("Sec-Fetch-Dest", "document")
        header("Sec-Fetch-Mode", "navigate")
        header("Sec-Fetch-Site", "same-origin")
    }
    
    val html = response.bodyAsText(Charsets.UTF_8)
    
    // 2. 从 HTML 中提取 token（正则匹配）
    val regex = Regex("""api\\":\{\\"token\\":\\"([a-f0-9]+)\\"""")
    val matchResult = regex.find(html)
    
    val token = matchResult?.groupValues?.get(1)
        ?: throw IllegalStateException("Unable to get CSRF Token")
    
    // 3. 持久化保存 token
    onTokenUpdated?.invoke(token)
    
    return token
}
```

#### Token 持久化

通过依赖注入配置的回调函数保存：

```kotlin
// SharedModule.kt
single {
    PixivApiClient(
        httpClient = get(),
        phpSessionId = config.phpSessionId,
        token = config.csrfToken,
        onTokenUpdated = { token ->
            // 保存到 DataStore
            pixivConfigStore.setCsrfToken(token)
        }
    )
}
```

### 1.4 错误检测机制

#### 当前实现

```kotlin
if (e.message?.contains("403") == true) {
    // 处理 403 错误
}
```

**特点：**
- ✅ 简单直接，无需额外依赖
- ⚠️ 通过字符串匹配检测，不够精确
- ⚠️ 无法区分 403 的具体原因

#### 潜在改进（未实现）

```kotlin
import io.ktor.client.plugins.*

try {
    // ...
} catch (e: ClientRequestException) {
    if (e.response.status.value == 403) {
        // 更精确的 403 检测
    }
}
```

### 1.5 使用示例

#### Repository 层调用

```kotlin
class ArtworkRepositoryImpl(private val pixivApi: PixivApi) {
    override suspend fun addBookmark(
        artworkId: Long,
        restrict: Int,
        tags: List<String>
    ): Result<Unit> = runCatching {
        // pixivApi.bookmarkApi 内部使用 postJson()
        // 如果遇到 403，会自动刷新 token 并重试
        pixivApi.bookmarkApi.addIllust(
            illustId = artworkId,
            restrict = restrict,
            tags = tags
        )
    }
}
```

#### ViewModel 层调用

```kotlin
class ArtworkDetailViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {
    fun addBookmark(artworkId: Long) {
        viewModelScope.launch {
            artworkRepository.addBookmark(artworkId, 0, emptyList())
                .onSuccess {
                    // 成功（包括自动重试后的成功）
                    _state.update { it.copy(isBookmarked = true) }
                }
                .onFailure { error ->
                    // 失败（重试后仍失败）
                    _state.update { it.copy(error = error.message) }
                }
        }
    }
}
```

---

## 🛡️ 二、Cloudflare 验证处理

### 2.1 需要特殊处理的方法

**✅ 完整 Cloudflare 处理（Chrome 142 完整请求头）：**

| 方法 | 位置 | 使用场景 |
|------|------|----------|
| `get<T>()` | 第 66-105 行 | 所有标准 Ajax API（`/ajax/*`） |
| `getRaw<T>()` | 第 136-173 行 | 非标准响应格式的 API（如 `/ranking.php`） |
| `getRawWithJson<T>()` | 第 179-220 行 | API 测试专用（带原始 JSON） |

**⚠️ 部分 Cloudflare 处理（Chrome 142 页面导航）：**

| 方法 | 位置 | 使用场景 |
|------|------|----------|
| `fetchToken()` | 第 488-517 行 | 获取 CSRF Token（访问 `/settings/account`） |

**❌ 无 Cloudflare 处理：**

所有其他方法（`get()`, `post()` 等）只发送基础请求头：
- `Referer: https://www.pixiv.net`
- `Cookie: PHPSESSID=...`
- `lang` 参数

### 2.2 完整浏览器请求头（getRaw 系列）

#### 请求头列表

```kotlin
suspend inline fun <reified T> getRaw(
    url: String,
    queryParams: Map<String, Any?>? = null
): T {
    val httpResponse = httpClient.get("$host$url") {
        // 基础请求头
        header("Referer", "https://www.pixiv.net")
        header("Cookie", "PHPSESSID=...")
        
        // 完整浏览器请求头 - 模拟 Chrome 142
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
        header("Accept", "*/*")
        header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7,zh-TW;q=0.6,ja;q=0.5,ru;q=0.4")
        header("Priority", "u=1, i")
        
        // Client Hints - 浏览器特征
        header("Sec-Ch-Ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"")
        header("Sec-Ch-Ua-Mobile", "?0")
        header("Sec-Ch-Ua-Platform", "\"Windows\"")
        
        // Fetch Metadata - 请求上下文
        header("Sec-Fetch-Dest", "empty")       // 目标类型：空
        header("Sec-Fetch-Mode", "cors")        // 请求模式：CORS
        header("Sec-Fetch-Site", "same-origin") // 请求来源：同源
        
        parameter("lang", langProvider())
        // ...
    }
    return httpResponse.body()
}
```

#### 请求头详解

| 请求头 | 值 | 作用 |
|--------|-----|------|
| **User-Agent** | Chrome 142 完整标识 | 告诉服务器这是 Chrome 142 浏览器 |
| **Accept** | `*/*` | 接受任何类型的响应 |
| **Accept-Language** | zh-CN,zh;q=0.9,... | 语言偏好列表（中文优先） |
| **Priority** | u=1, i | 请求优先级（用户优先级 1） |
| **Sec-Ch-Ua** | Chromium/Chrome 版本 | 客户端提示 - 浏览器版本 |
| **Sec-Ch-Ua-Mobile** | ?0 | 客户端提示 - 不是移动设备 |
| **Sec-Ch-Ua-Platform** | "Windows" | 客户端提示 - 操作系统 |
| **Sec-Fetch-Dest** | empty | 请求目标类型（XHR/Fetch） |
| **Sec-Fetch-Mode** | cors | CORS 跨域请求模式 |
| **Sec-Fetch-Site** | same-origin | 同源请求（不是跨域） |

#### 为什么需要这些请求头？

**Cloudflare 检测机制：**

1. **User-Agent 检测**：识别是否为真实浏览器
2. **Client Hints 验证**：检查浏览器特征是否一致
3. **Fetch Metadata 分析**：判断请求上下文是否合理
4. **请求特征指纹**：综合所有请求头进行指纹识别

**如果缺少这些请求头：**
- ❌ 可能被识别为爬虫/机器人
- ❌ 触发 JavaScript Challenge（需要执行 JS）
- ❌ 触发 CAPTCHA 验证码
- ❌ 直接返回 403 Forbidden

### 2.3 页面导航请求头（fetchToken）

#### 请求头列表

```kotlin
internal suspend fun fetchToken(): String {
    val response = httpClient.get("$host/settings/account") {
        header("Referer", host)
        header("Cookie", cookie)
        
        // 模拟浏览器页面导航 - Chrome 142
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
        header("Connection", "keep-alive")
        header("Upgrade-Insecure-Requests", "1")
        
        // Fetch Metadata - 页面导航
        header("Sec-Fetch-Dest", "document")    // 目标：文档（HTML页面）
        header("Sec-Fetch-Mode", "navigate")    // 模式：导航（地址栏输入/点击链接）
        header("Sec-Fetch-Site", "same-origin") // 来源：同源
    }
    // ...
}
```

#### 与 getRaw 的区别

| 特性 | getRaw() | fetchToken() |
|------|----------|--------------|
| **模拟版本** | Chrome 142 | Chrome 142 |
| **请求类型** | XHR/Fetch（AJAX） | 页面导航（HTML） |
| **Accept** | `*/*` | `text/html,...` |
| **Sec-Fetch-Dest** | empty | document |
| **Sec-Fetch-Mode** | cors | navigate |
| **Client Hints** | ✅ 包含 | ❌ 不包含 |
| **Upgrade-Insecure** | ❌ 不包含 | ✅ 包含 |

**原因：**
- `fetchToken()` 访问 HTML 设置页面，需要模拟**浏览器页面导航**
- `getRaw()` 调用 API 端点，需要模拟 **AJAX 请求**

### 2.4 使用场景分析

#### 需要完整 Cloudflare 处理的 API

**示例 1：排行榜 API**

```kotlin
// RankingApi.kt
suspend fun getIllustRanking(
    mode: RankingMode = RankingMode.DAILY,
    page: Int = 1,
    content: RankingContent = RankingContent.ALL,
    date: String? = null
): RankingResponse {
    // 使用 getRaw()，包含完整 Cloudflare 处理
    return client.getRaw<RankingResponse>("/ranking.php", params)
}
```

**为什么需要？**
- `/ranking.php` 是旧的 PHP 端点
- Cloudflare 对旧端点的保护更严格
- 不添加完整请求头可能返回 403 或 503

**示例 2：关注动态 API**

```kotlin
// FollowApi.kt
suspend fun getFollowLatestIllust(
    mode: String = "all",
    page: Int = 1
): PixivResponse<FollowLatestBody> {
    // 使用 getRaw()，包含完整 Cloudflare 处理
    return client.getRaw("/ajax/follow_latest/illust", mapOf(
        "mode" to mode,
        "p" to page
    ))
}

suspend fun getWatchListManga(page: Int = 1): PixivResponse<WatchListMangaBody> {
    // 追更列表也使用 getRaw() 处理
    return client.getRaw("/ajax/watch_list/manga", mapOf("p" to page, "new" to "1"))
}
```

**为什么需要？**
- `/ajax/follow_latest/*` 端点在高频访问时可能触发 Cloudflare 验证
- `/ajax/watch_list/*` 追更列表也容易触发保护机制
- 动态列表是用户频繁访问的功能
- 添加完整请求头可以避免 403 或 503 错误

**示例 3：特殊 RPC 端点**

```kotlin
// 某些特殊的 /rpc/*.php 端点
suspend fun callSpecialRpc(params: Map<String, Any?>): SpecialData {
    return client.getRaw<SpecialData>("/rpc/special.php", params)
}
```

#### 不需要特殊处理的 API

**标准 Ajax API（大部分 API）：**

```kotlin
// IllustApi.kt
suspend fun getDetail(pid: Long): PixivResponse<IllustDetailBody> {
    // 使用 get()，只需基础请求头
    return client.get<IllustDetailBody>("/ajax/illust/$pid")
}

// UserApi.kt
suspend fun getUserInfo(uid: Long): PixivResponse<UserInfoBody> {
    // 使用 get()，不需要特殊处理
    return client.get<UserInfoBody>("/ajax/user/$uid")
}
```

**为什么不需要？**
- `/ajax/*` 是新的 API 端点
- Pixiv 对自家 App/客户端的 API 保护较宽松
- 基础的 Cookie + Referer 就足够

### 2.5 调试和测试

#### 测试是否触发 Cloudflare

```kotlin
// 使用带原始 JSON 的方法测试
val result = client.getRawWithJson<RankingResponse>(
    url = "/ranking.php",
    queryParams = mapOf("mode" to "daily", "p" to 1, "format" to "json")
)

// 检查返回内容
if (result.rawJson.contains("cloudflare") || result.rawJson.contains("Just a moment")) {
    println("⚠️ 触发了 Cloudflare Challenge")
} else {
    println("✅ 成功绕过 Cloudflare")
}
```

#### 比较不同请求头的效果

```kotlin
// 测试 1：无特殊请求头
try {
    val response1 = httpClient.get("$host/ranking.php") {
        header("Referer", host)
        header("Cookie", cookie)
    }
    println("无特殊请求头：成功")
} catch (e: Exception) {
    println("无特殊请求头：失败 - ${e.message}")
}

// 测试 2：完整浏览器请求头
try {
    val response2 = client.getRaw<RankingResponse>("/ranking.php", params)
    println("完整请求头：成功")
} catch (e: Exception) {
    println("完整请求头：失败 - ${e.message}")
}
```

---

## 📊 三、综合对比表

### 3.1 所有方法的错误处理能力

| 方法 | GET/POST | 403 处理 | Cloudflare 处理 | 使用场景 |
|------|----------|----------|----------------|----------|
| `get<T>()` | GET | ❌ | ❌ | 标准 Ajax API（`/ajax/*`） |
| `getWithRaw<T>()` | GET | ❌ | ❌ | 带原始 JSON 的标准 API |
| `getRaw<T>()` | GET | ❌ | ✅ **完整** | 旧 PHP 端点（如 `/ranking.php`） |
| `getRawWithJson<T>()` | GET | ❌ | ✅ **完整** | API 测试 + 旧端点 |
| `getHtml()` | GET | ❌ | ❌ | 获取 HTML 页面 |
| `postJson<T, B>()` | POST | ✅ **自动重试** | ❌ | 标准 POST API（有 body） |
| `postJson<T>()` | POST | ✅ **自动重试** | ❌ | 标准 POST API（无 body） |
| `postJsonWithRaw<T, B>()` | POST | ❌ | ❌ | API 测试专用 |
| `postForm<T>()` | POST | ✅ **自动重试** | ❌ | Form 表单提交 |
| `postFormWithRaw<T>()` | POST | ❌ | ❌ | API 测试专用 |
| `postFormRaw<T>()` | POST | ❌ | ❌ | 旧 API（非标准响应） |
| `fetchToken()` | GET | ❌ | ⚠️ **部分** | 获取 CSRF Token |

### 3.2 错误处理覆盖范围

#### 403 错误处理

```
应用架构层次:
┌─────────────────────────────────────┐
│  ViewModel 层                       │
│  - 显示通用错误消息                  │
│  - 不区分错误类型                    │
└─────────────────────────────────────┘
                ↓ Result<T>
┌─────────────────────────────────────┐
│  Repository 层                      │
│  - runCatching 捕获所有异常          │
│  - 包装为 Result.failure             │
└─────────────────────────────────────┘
                ↓ throws Exception
┌─────────────────────────────────────┐
│  API 层 (IllustApi, UserApi, ...)  │
│  - 调用 PixivApiClient 方法          │
│  - 透传异常                         │
└─────────────────────────────────────┘
                ↓ throws Exception
┌─────────────────────────────────────┐
│  ✅ PixivApiClient (HTTP 客户端)    │
│  - POST 方法: 捕获 403 并自动重试    │  ← 唯一的 403 处理点
│  - GET 方法: 不处理，直接抛出异常     │
└─────────────────────────────────────┘
                ↓ HTTP Request
┌─────────────────────────────────────┐
│  Ktor HttpClient                    │
│  - 发送 HTTP 请求                    │
│  - 抛出网络异常                      │
└─────────────────────────────────────┘
```

#### Cloudflare 处理

```
API 端点类型:
┌─────────────────────────────────────┐
│  新 Ajax API (/ajax/*)              │
│  - 大部分不需要特殊处理               │
│  - 使用 get() / post() 方法          │
│  ✅ 占比: ~85% 的 API                │
│  示例: /ajax/illust/*, /ajax/user/* │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  高频 Ajax API (/ajax/follow_*)     │
│  - 需要完整浏览器请求头               │
│  - 使用 getRaw() 方法                │
│  ✅ 占比: ~5% 的 API                 │
│  示例: /ajax/follow_latest/*,       │
│        /ajax/watch_list/*           │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  旧 PHP 端点 (*.php)                │
│  - 需要完整浏览器请求头               │
│  - 使用 getRaw() 方法                │
│  ✅ 占比: ~10% 的 API                │
│  示例: /ranking.php, /rpc/*.php     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  HTML 页面                          │
│  - 需要页面导航请求头                │
│  - 使用 fetchToken() / getHtml()    │
│  ✅ 占比: 特殊场景                   │
│  示例: /settings/account            │
└─────────────────────────────────────┘
```

---

## 🔧 四、最佳实践

### 4.1 选择正确的方法

#### 决策树

```
需要调用 API
    ↓
是 POST 请求？
    ↓
[是] → 需要 CSRF token？
        ↓
      [是] → 使用 postJson() 或 postForm()  ← 自动处理 403
        ↓
      [否] → 使用 postFormRaw()
    ↓
[否] → 是旧 PHP 端点（*.php）？
        ↓
      [是] → 使用 getRaw()  ← 完整 Cloudflare 处理
        ↓
      [否] → 是标准 Ajax API（/ajax/*）？
              ↓
            [是] → 使用 get()  ← 无需特殊处理
              ↓
            [否] → 需要原始 JSON（测试）？
                    ↓
                  [是] → 使用 getWithRaw() 或 getRawWithJson()
                    ↓
                  [否] → 返回 HTML？
                          ↓
                        [是] → 使用 getHtml()
```

### 4.2 Repository 实现建议

#### ✅ 推荐写法

```kotlin
class ArtworkRepositoryImpl(
    private val pixivApi: PixivApi
) : ArtworkRepository {
    
    // GET 请求 - 使用标准方法
    override suspend fun getArtworkDetail(artworkId: Long): Result<Artwork> = runCatching {
        val response = pixivApi.illustApi.getDetail(artworkId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.toArtwork() ?: throw IllegalStateException("Empty response")
    }
    
    // POST 请求 - 自动处理 403
    override suspend fun addBookmark(
        artworkId: Long,
        restrict: Int,
        tags: List<String>
    ): Result<Unit> = runCatching {
        // postJson() 会自动处理 403 错误
        pixivApi.bookmarkApi.addIllust(artworkId, restrict, tags)
    }
}
```

#### ❌ 不推荐写法

```kotlin
// 不要在 Repository 中直接处理 403
override suspend fun addBookmark(...): Result<Unit> = runCatching {
    try {
        pixivApi.bookmarkApi.addIllust(...)
    } catch (e: Exception) {
        // ❌ 不需要，PixivApiClient 已经处理了
        if (e.message?.contains("403") == true) {
            // 重新获取 token...
            // 重试...
        }
        throw e
    }
}
```

### 4.3 错误处理建议

#### ViewModel 层

```kotlin
class ArtworkDetailViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {
    
    fun loadArtwork(artworkId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            artworkRepository.getArtworkDetail(artworkId)
                .onSuccess { artwork ->
                    _state.update {
                        it.copy(
                            artwork = artwork,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.localizedMessage ?: "Unknown error"
                        )
                    }
                }
        }
    }
}
```

#### UI 层

```kotlin
@Composable
fun ArtworkDetailScreen(
    viewModel: ArtworkDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    when {
        state.isLoading -> LoadingIndicator()
        state.error != null -> ErrorView(
            message = state.error,
            onRetry = { viewModel.loadArtwork(artworkId) }
        )
        state.artwork != null -> ArtworkContent(state.artwork)
    }
}
```

---

## 🐛 五、故障排查

### 5.1 常见问题

#### 问题 1: POST 请求一直返回 403

**可能原因：**
1. PHPSESSID 过期或无效
2. fetchToken() 无法获取新 token
3. 网络环境触发额外的 Cloudflare 验证

**排查步骤：**

```kotlin
// 1. 检查 PHPSESSID 是否有效
val config = pixivConfigStore.getCurrentConfig()
println("PHPSESSID: ${config.phpSessionId}")
println("CSRF Token: ${config.csrfToken}")

// 2. 手动测试 fetchToken()
try {
    val token = pixivApi.client.fetchToken()
    println("✅ Token 获取成功: $token")
} catch (e: Exception) {
    println("❌ Token 获取失败: ${e.message}")
}

// 3. 测试基础 GET 请求
try {
    val user = pixivApi.userApi.getUserInfo(pixivApi.client.userId)
    println("✅ GET 请求正常")
} catch (e: Exception) {
    println("❌ GET 请求失败: ${e.message}")
}
```

**解决方案：**
- 重新登录获取新的 PHPSESSID
- 检查网络代理设置
- 查看 Ktor 日志确认请求详情

#### 问题 2: 旧 PHP 端点返回 503 或 Cloudflare Challenge

**可能原因：**
1. 缺少完整的浏览器请求头
2. User-Agent 不够新（被识别为旧版本）
3. IP 被 Cloudflare 临时限制

**排查步骤：**

```kotlin
// 1. 确认使用了正确的方法
// ❌ 错误
val ranking = client.get<RankingResponse>("/ranking.php", params)

// ✅ 正确
val ranking = client.getRaw<RankingResponse>("/ranking.php", params)

// 2. 检查返回内容
val result = client.getRawWithJson<RankingResponse>("/ranking.php", params)
if (result.rawJson.contains("<!DOCTYPE html>")) {
    println("⚠️ 返回了 HTML 而不是 JSON，可能是 Cloudflare Challenge")
    println(result.rawJson.take(500)) // 查看前 500 字符
}
```

**解决方案：**
- 确保使用 `getRaw()` 而不是 `get()`
- 更新 User-Agent 版本号
- 等待一段时间后重试（IP 限制可能是临时的）

#### 问题 3: 重试后仍然失败

**可能原因：**
1. 错误不是 403（是其他类型的错误）
2. PHPSESSID 彻底失效，无法获取新 token
3. 账号被封禁或限制

**排查步骤：**

```kotlin
try {
    pixivApi.bookmarkApi.addIllust(artworkId, 0, emptyList())
} catch (e: Exception) {
    println("错误类型: ${e::class.simpleName}")
    println("错误消息: ${e.message}")
    println("堆栈跟踪: ${e.stackTraceToString()}")
    
    // 检查是否是 403
    if (e.message?.contains("403") == true) {
        println("⚠️ 确实是 403 错误，但重试失败")
    } else {
        println("⚠️ 不是 403 错误，是其他问题")
    }
}
```

**解决方案：**
- 重新登录获取新凭据
- 检查账号状态
- 查看完整的错误堆栈

### 5.2 日志输出

#### 启用详细日志

```kotlin
// NetworkClient.kt
install(Logging) {
    logger = Logger.DEFAULT
    level = LogLevel.ALL  // 改为 ALL 查看所有请求详情
    filter { request ->
        request.url.host.contains("pixiv")
    }
}
```

#### 关键日志点

```kotlin
// PixivApiClient.kt 中的日志
println("⚠️ POST请求遇到403错误，刷新CSRF token并重试...")  // 触发 403 处理
println("✅ Token 刷新成功: $newToken")  // Token 刷新成功

// 自定义日志
println("📤 发送请求: $url")
println("📥 响应状态: ${response.status}")
println("📝 响应内容: ${response.bodyAsText()}")
```

---

## 📚 六、参考资料

### 6.1 相关文件

| 文件 | 说明 |
|------|------|
| [PixivApiClient.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/PixivApiClient.kt) | HTTP 客户端核心实现 |
| [PixivApi.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/data/remote/api/PixivApi.kt) | API 统一门面 |
| [NetworkClient.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/util/NetworkClient.kt) | Ktor 客户端配置 |
| [SharedModule.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/di/SharedModule.kt) | 依赖注入配置 |

### 6.2 相关文档

| 文档 | 说明 |
|------|------|
| [PIXIV_API_集成指南.md](../pixiv/PIXIV_API_集成指南.md) | Pixiv API 使用指南 |
| [API_STATUS.md](../shared/API_STATUS.md) | API 状态和方法列表 |
| [项目架构参考文档.md](../project/项目架构参考文档.md) | 项目整体架构 |

### 6.3 外部参考

- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [Cloudflare Bot Detection](https://developers.cloudflare.com/bots/)
- [HTTP Client Hints](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#client_hints)
- [Fetch Metadata](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers#fetch_metadata_request_headers)

---

## 📝 更新日志

- **2025-12-16**: 
  - 初始版本，整合 403 错误处理和 Cloudflare 验证机制文档
  - 统一 Chrome 版本为 142
  - 为 FollowApi 的所有方法添加 Cloudflare 处理（`getRaw()`）
  - 更新 API 端点分类，新增"高频 Ajax API"分类
