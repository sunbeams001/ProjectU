# Pixiv API 快速开始指南

## 5分钟快速集成

### 步骤 1: 获取 PHPSESSID

1. 在浏览器中访问 https://www.pixiv.net/ 并登录
2. 按 F12 打开开发者工具
3. 切换到 Application/Storage → Cookies
4. 找到并复制 `PHPSESSID` 的值（格式：`用户ID_随机字符串`）

### 步骤 2: 配置应用

#### Android

编辑 `composeApp/src/androidMain/kotlin/.../PixivApplication.kt`:

```kotlin
class PixivApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // TODO: 从安全存储中读取 PHPSESSID
        val phpSessionId = "你的PHPSESSID"  // 实际应用中不要硬编码！
        
        startKoin {
            androidContext(this@PixivApplication)
            modules(
                networkModule(createHttpClient()),
                pixivApiModule(phpSessionId),
                repositoryModule,
                useCaseModule
            )
        }
    }
    
    private fun createHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}
```

#### Desktop

编辑 `composeApp/src/desktopMain/kotlin/.../main.kt`:

```kotlin
fun main() = application {
    // TODO: 从配置文件或环境变量读取
    val phpSessionId = "你的PHPSESSID"
    
    // 初始化 Koin
    startKoin {
        modules(
            networkModule(createHttpClient()),
            pixivApiModule(phpSessionId),
            repositoryModule,
            useCaseModule
        )
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "ProjectU"
    ) {
        App()
    }
}
```

### 步骤 3: 使用 API

#### 在 ViewModel 中使用

```kotlin
class HomeViewModel : ViewModel(), KoinComponent {
    private val artworkRepository: ArtworkRepository by inject()
    
    private val _artworks = MutableStateFlow<List<Artwork>>(emptyList())
    val artworks = _artworks.asStateFlow()
    
    init {
        loadRecommendedArtworks()
    }
    
    private fun loadRecommendedArtworks() {
        viewModelScope.launch {
            artworkRepository.getRecommendedArtworks(page = 1, limit = 20)
                .onSuccess { _artworks.value = it }
                .onFailure { /* 处理错误 */ }
        }
    }
}
```

#### 在 Compose UI 中显示

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val artworks by viewModel.artworks.collectAsState()
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp)
    ) {
        items(artworks) { artwork ->
            ArtworkCard(artwork)
        }
    }
}

@Composable
fun ArtworkCard(artwork: Artwork) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artwork.imageUrls.first())
                    .addHeader("Referer", "https://www.pixiv.net/")
                    .build(),
                contentDescription = artwork.title
            )
            Text(artwork.title, maxLines = 1)
            Text("by ${artwork.userName}", style = MaterialTheme.typography.bodySmall)
            Row {
                Text("❤️ ${artwork.likeCount}")
                Spacer(Modifier.width(8.dp))
                Text("💾 ${artwork.bookmarkCount}")
            }
        }
    }
}
```

## 常用功能示例

### 1. 搜索作品

```kotlin
suspend fun searchArtworks(keyword: String) {
    artworkRepository.searchArtworks(
        keyword = keyword,
        page = 1,
        searchMode = "s_tag",
        order = "date_d"
    ).onSuccess { artworks ->
        // 显示搜索结果
    }
}
```

### 2. 查看排行榜

```kotlin
suspend fun loadRanking() {
    artworkRepository.getRankingArtworks(
        mode = "daily",  // daily, weekly, monthly
        page = 1
    ).onSuccess { artworks ->
        // 显示排行榜
    }
}
```

### 3. 收藏作品

```kotlin
suspend fun bookmarkArtwork(artworkId: String) {
    artworkRepository.addBookmark(
        artworkId = artworkId,
        isPrivate = false,
        tags = listOf("收藏")
    ).onSuccess {
        // 收藏成功
    }
}
```

### 4. 播放 Ugoira 动图

```kotlin
suspend fun loadUgoira(artworkId: String) {
    artworkRepository.getUgoiraMetadata(artworkId)
        .onSuccess { metadata ->
            // 使用 UgoiraPlayer 播放
            // metadata.zipUrl - ZIP文件地址
            // metadata.frames - 帧列表及延迟时间
        }
}
```

## 安全建议

### ⚠️ 重要：不要硬编码 PHPSESSID

**错误做法：**
```kotlin
// ❌ 不要这样做！
val phpSessionId = "12345678_abcdefghijklmnop"
```

**正确做法：**

#### Android - 使用 EncryptedSharedPreferences

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "pixiv_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun savePhpSessionId(sessionId: String) {
        prefs.edit().putString("php_session_id", sessionId).apply()
    }
    
    fun getPhpSessionId(): String? {
        return prefs.getString("php_session_id", null)
    }
}
```

#### Desktop - 使用环境变量或配置文件

```kotlin
// 从环境变量读取
val phpSessionId = System.getenv("PIXIV_SESSION_ID") 
    ?: throw IllegalStateException("请设置 PIXIV_SESSION_ID 环境变量")

// 或从加密的配置文件读取
val config = File("config/pixiv.encrypted").readText()
val phpSessionId = decryptConfig(config)
```

## 故障排除

### 问题 1: 401 Unauthorized

**原因：** PHPSESSID 无效或过期

**解决：**
1. 重新登录 Pixiv 网站
2. 获取新的 PHPSESSID
3. 更新应用配置

### 问题 2: 图片加载失败

**原因：** 缺少 Referer header

**解决：**
```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(imageUrl)
        .addHeader("Referer", "https://www.pixiv.net/")
        .build(),
    contentDescription = null
)
```

### 问题 3: Token not found

**原因：** 无法自动获取 CSRF Token

**解决：**
```kotlin
// 手动提供 Token
pixivApiModule(
    phpSessionId = "你的PHPSESSID",
    token = "手动获取的Token"  // 从浏览器 console 执行 pixiv.context.token
)
```

### 问题 4: 请求频率过高

**原因：** Pixiv 有 API 调用频率限制

**解决：**
```kotlin
// 添加请求延迟
suspend fun searchWithDelay(keyword: String) {
    delay(1000)  // 延迟1秒
    artworkRepository.searchArtworks(keyword, 1)
}

// 使用缓存
val cachedResult = cache.get(cacheKey)
if (cachedResult != null) return cachedResult
```

## 下一步

- 阅读完整文档：[PIXIV_API_INTEGRATION.md](PIXIV_API_INTEGRATION.md)
- 查看示例代码：`shared/src/commonMain/kotlin/.../examples/PixivApiUsageExample.kt`
- 了解数据模型：查看 `domain/model/` 目录
- 自定义配置：修改 `PixivConfig.kt`

## 注意事项

1. **遵守 Pixiv 使用条款** - 仅用于个人学习研究
2. **保护用户隐私** - 不要泄露 PHPSESSID
3. **合理使用 API** - 避免过于频繁的请求
4. **处理异常** - 网络请求可能失败，做好错误处理
5. **更新及时** - PHPSESSID 会过期，需要重新登录

## 技术支持

遇到问题？

1. 查看日志输出
2. 检查网络连接
3. 验证 PHPSESSID 是否有效
4. 阅读完整文档

祝使用愉快！🎨

