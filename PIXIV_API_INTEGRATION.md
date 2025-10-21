# Pixiv API 集成文档

## 概述

本项目已成功集成开源的 Pixiv Web API，使用 Kotlin Multiplatform 重写，完全基于 Ktor 实现网络请求，无需依赖原始的 Java 库。

## 架构设计

### 分层结构

```
┌─────────────────────────────────────┐
│    Presentation Layer (UI)          │
│    - ViewModels                      │
│    - Screens                         │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│    Domain Layer                      │
│    - Repository Interfaces           │
│    - Use Cases                       │
│    - Domain Models                   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│    Data Layer                        │
│    - Repository Implementations      │
│    - Pixiv API Client                │
│    - DTO & Mappers                   │
└─────────────────────────────────────┘
```

### 核心组件

1. **PixivApiClient** - 核心客户端，处理认证和请求
2. **PixivApi** - API 门面，提供统一访问入口
3. **各API模块** - IllustApi, UserApi, BookmarkApi, RankingApi
4. **Repository实现** - ArtworkRepositoryImpl, UserRepositoryImpl
5. **DTO & Mappers** - 数据传输对象和转换器

## 认证方式

Pixiv Web API 使用 **Cookie 认证**，需要 `PHPSESSID`。

### 获取 PHPSESSID

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具 (F12)
3. 进入 Application/Storage -> Cookies
4. 找到 `PHPSESSID` 值
5. 格式为：`用户ID_随机字符串`，例如：`12345678_abcdefghijklmnop`

### 配置认证信息

```kotlin
// 方法1: 使用配置类
val config = PixivConfig(
    phpSessionId = "你的PHPSESSID",
    language = "zh"  // 可选：zh, en, ja, ko
)

// 方法2: 直接从字符串创建
val config = PixivConfig.fromPhpSessionId("你的PHPSESSID")
```

## 快速开始

### 1. 初始化 Koin 模块

在应用启动时配置依赖注入：

```kotlin
import com.projectu.shared.di.*
import org.koin.core.context.startKoin

fun initKoin(phpSessionId: String) {
    startKoin {
        modules(
            networkModule(createHttpClient()),
            pixivApiModule(phpSessionId),
            repositoryModule,
            useCaseModule
        )
    }
}

// 创建 HttpClient
fun createHttpClient(): HttpClient {
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
```

### 2. 使用 Repository

```kotlin
import com.projectu.shared.domain.repository.ArtworkRepository
import org.koin.core.component.inject

class MyViewModel : ViewModel(), KoinComponent {
    private val artworkRepository: ArtworkRepository by inject()
    
    suspend fun loadArtwork(artworkId: String) {
        artworkRepository.getArtworkDetail(artworkId)
            .onSuccess { artwork ->
                println("作品标题: ${artwork.title}")
                println("作者: ${artwork.userName}")
            }
            .onFailure { error ->
                println("加载失败: ${error.message}")
            }
    }
}
```

### 3. 直接使用 API

```kotlin
import com.projectu.shared.data.remote.api.PixivApi
import org.koin.core.component.inject

class MyService : KoinComponent {
    private val pixivApi: PixivApi by inject()
    
    // 获取作品详情
    suspend fun getIllustDetail(pid: Long) {
        val response = pixivApi.illustApi.getDetail(pid)
        if (!response.error) {
            val illust = response.body
            println("标题: ${illust?.title}")
        }
    }
    
    // 搜索作品
    suspend fun searchIllusts(keyword: String) {
        val response = pixivApi.illustApi.search(
            keyword = keyword,
            searchMode = "s_tag",
            order = "date_d",
            page = 1
        )
        response.body?.illustManga?.data?.forEach { illust ->
            println("${illust.id}: ${illust.title}")
        }
    }
    
    // 获取排行榜
    suspend fun getRanking() {
        val response = pixivApi.rankingApi.getDailyRanking(
            page = 1,
            content = "all"
        )
        response.contents.forEach { content ->
            println("排名 ${content.rank}: ${content.title}")
        }
    }
}
```

## API 功能列表

### 插画 API (IllustApi)

| 方法 | 功能 | 说明 |
|------|------|------|
| `getDetail(pid)` | 获取作品详情 | 返回完整的作品信息 |
| `getBookmarkData(pid)` | 获取收藏状态 | 检查是否已收藏 |
| `getUgoiraMeta(pid)` | 获取动图元数据 | 用于播放 Ugoira |
| `search(...)` | 搜索作品 | 支持多种搜索模式 |
| `getDiscovery(...)` | 发现作品 | 推荐新作品 |
| `getFollowLatest(...)` | 关注用户最新作品 | 获取关注列表更新 |
| `getRecommendInit(...)` | 获取推荐作品 | 基于某个作品推荐 |
| `getRecommendIllusts(...)` | 批量推荐 | 基于多个作品推荐 |
| `postLike(pid)` | 点赞作品 | 给作品点赞 |

### 用户 API (UserApi)

| 方法 | 功能 | 说明 |
|------|------|------|
| `getUserInfo(uid)` | 获取用户信息 | 返回用户详细信息 |
| `getProfileAll(uid)` | 获取用户作品概况 | 所有作品ID列表 |
| `getProfileIllusts(...)` | 获取用户插画 | 指定作品的详细信息 |
| `getUserBookmarkIllusts(...)` | 获取用户收藏 | 支持标签筛选 |
| `getRecommendUsers(...)` | 推荐用户 | 基于某个用户推荐 |
| `followUser(...)` | 关注用户 | 添加关注 |
| `unfollowUser(uid)` | 取消关注 | 移除关注 |

### 收藏 API (BookmarkApi)

| 方法 | 功能 | 说明 |
|------|------|------|
| `addIllust(...)` | 收藏插画 | 支持私密收藏和标签 |
| `deleteIllust(bookmarkId)` | 删除收藏 | 取消收藏 |
| `deleteIllusts(ids)` | 批量删除收藏 | 批量操作 |
| `addNovel(...)` | 收藏小说 | 同插画 |
| `deleteNovel(bookId)` | 删除小说收藏 | - |
| `deleteNovels(ids)` | 批量删除小说收藏 | - |

### 排行榜 API (RankingApi)

| 方法 | 功能 | 说明 |
|------|------|------|
| `getIllustRanking(...)` | 通用排行榜 | 支持多种模式 |
| `getDailyRanking(...)` | 日榜 | 每日排行 |
| `getWeeklyRanking(...)` | 周榜 | 每周排行 |
| `getMonthlyRanking(...)` | 月榜 | 每月排行 |
| `getRookieRanking(...)` | 新人榜 | 新人作品 |
| `getOriginalRanking(...)` | 原创榜 | 原创作品 |
| `getMaleRanking(...)` | 男性向榜 | - |
| `getFemaleRanking(...)` | 女性向榜 | - |

## 数据模型

### Artwork (作品)

```kotlin
data class Artwork(
    val id: String,              // 作品ID
    val title: String,           // 标题
    val description: String,     // 描述
    val type: ArtworkType,       // 类型：插画/漫画/动图
    val imageUrls: List<String>, // 图片URL列表
    val width: Int,              // 宽度
    val height: Int,             // 高度
    val pageCount: Int,          // 页数
    val userId: String,          // 作者ID
    val userName: String,        // 作者名
    val tags: List<String>,      // 标签
    val viewCount: Int,          // 浏览数
    val likeCount: Int,          // 点赞数
    val bookmarkCount: Int,      // 收藏数
    val commentCount: Int,       // 评论数
    val createdTime: String,     // 创建时间
    val isBookmarked: Boolean,   // 是否已收藏
    val ageLimit: AgeLimit,      // 年龄限制
    val ugoiraMetadata: UgoiraMetadata? // 动图元数据
)
```

### UgoiraMetadata (动图元数据)

```kotlin
data class UgoiraMetadata(
    val zipUrl: String,          // ZIP文件URL
    val frames: List<UgoiraFrame> // 帧列表
)

data class UgoiraFrame(
    val file: String,            // 文件名
    val delay: Int               // 延迟(毫秒)
)
```

## 使用示例

### 示例 1: 加载并显示作品

```kotlin
class ArtworkViewModel : ViewModel(), KoinComponent {
    private val repository: ArtworkRepository by inject()
    private val _artwork = MutableStateFlow<Artwork?>(null)
    val artwork = _artwork.asStateFlow()
    
    fun loadArtwork(artworkId: String) {
        viewModelScope.launch {
            repository.getArtworkDetail(artworkId)
                .onSuccess { _artwork.value = it }
                .onFailure { /* 处理错误 */ }
        }
    }
}

@Composable
fun ArtworkScreen(artworkId: String) {
    val viewModel: ArtworkViewModel = koinViewModel()
    val artwork by viewModel.artwork.collectAsState()
    
    LaunchedEffect(artworkId) {
        viewModel.loadArtwork(artworkId)
    }
    
    artwork?.let { art ->
        Column {
            AsyncImage(
                model = art.imageUrls.first(),
                contentDescription = art.title
            )
            Text(art.title, style = MaterialTheme.typography.headlineMedium)
            Text("by ${art.userName}")
            Text("❤️ ${art.likeCount}  💾 ${art.bookmarkCount}")
        }
    }
}
```

### 示例 2: 搜索作品

```kotlin
class SearchViewModel : ViewModel(), KoinComponent {
    private val repository: ArtworkRepository by inject()
    private val _results = MutableStateFlow<List<Artwork>>(emptyList())
    val results = _results.asStateFlow()
    
    fun search(keyword: String) {
        viewModelScope.launch {
            repository.searchArtworks(
                keyword = keyword,
                page = 1,
                searchMode = "s_tag",
                order = "date_d"
            ).onSuccess { _results.value = it }
        }
    }
}
```

### 示例 3: 获取排行榜

```kotlin
class RankingViewModel : ViewModel(), KoinComponent {
    private val repository: ArtworkRepository by inject()
    private val _ranking = MutableStateFlow<List<Artwork>>(emptyList())
    val ranking = _ranking.asStateFlow()
    
    fun loadDailyRanking() {
        viewModelScope.launch {
            repository.getRankingArtworks(
                mode = "daily",
                page = 1
            ).onSuccess { _ranking.value = it }
        }
    }
}
```

### 示例 4: 收藏作品

```kotlin
class BookmarkViewModel : ViewModel(), KoinComponent {
    private val repository: ArtworkRepository by inject()
    
    fun bookmarkArtwork(artworkId: String, isPrivate: Boolean = false) {
        viewModelScope.launch {
            repository.addBookmark(
                artworkId = artworkId,
                isPrivate = isPrivate,
                tags = listOf("收藏")
            ).onSuccess {
                println("收藏成功")
            }.onFailure { error ->
                println("收藏失败: ${error.message}")
            }
        }
    }
    
    fun removeBookmark(artworkId: String) {
        viewModelScope.launch {
            repository.removeBookmark(artworkId)
                .onSuccess { println("已取消收藏") }
        }
    }
}
```

### 示例 5: 播放 Ugoira 动图

```kotlin
class UgoiraViewModel : ViewModel(), KoinComponent {
    private val getUgoiraUseCase: GetUgoiraUseCase by inject()
    private val _frames = MutableStateFlow<List<String>>(emptyList())
    val frames = _frames.asStateFlow()
    
    fun loadUgoira(artworkId: String) {
        viewModelScope.launch {
            getUgoiraUseCase(artworkId)
                .onSuccess { (metadata, frameFiles) ->
                    _frames.value = frameFiles
                }
        }
    }
}
```

## 注意事项

### 1. 认证安全

- **不要**在代码中硬编码 PHPSESSID
- 使用加密存储保存认证信息
- 定期检查 Session 是否过期

### 2. 速率限制

- Pixiv 有 API 调用频率限制
- 建议添加请求间隔
- 使用缓存减少重复请求

### 3. 错误处理

```kotlin
repository.getArtworkDetail(artworkId)
    .onSuccess { artwork ->
        // 成功处理
    }
    .onFailure { error ->
        when (error) {
            is IOException -> {
                // 网络错误
            }
            is IllegalStateException -> {
                // API 返回错误
                println("API Error: ${error.message}")
            }
            else -> {
                // 其他错误
            }
        }
    }
```

### 4. 图片加载

Pixiv 的图片需要设置 Referer 才能访问：

```kotlin
// 使用 Coil 加载图片
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .addHeader("Referer", "https://www.pixiv.net/")
        .build(),
    contentDescription = "作品图片"
)
```

## 与原 Java 库的对比

| 特性 | Java (pixiv-utils) | Kotlin (本项目) |
|------|-------------------|----------------|
| 语言 | Java 8 | Kotlin |
| 网络库 | OkHttp | Ktor |
| 序列化 | Jackson | kotlinx.serialization |
| 平台支持 | JVM only | KMP (Android + Desktop) |
| 协程支持 | ❌ | ✅ |
| 类型安全 | ⚠️ | ✅ |
| 代码量 | 较多 | 较少 |
| 维护性 | 一般 | 优秀 |

## 文件结构

```
shared/src/commonMain/kotlin/
├── data/
│   ├── local/
│   │   ├── PixivConfig.kt              # 配置类
│   │   └── PixivConfigStore.kt         # 配置存储
│   ├── remote/
│   │   ├── api/
│   │   │   ├── PixivApiClient.kt       # 核心客户端
│   │   │   ├── PixivApi.kt             # API 门面
│   │   │   ├── IllustApi.kt            # 插画 API
│   │   │   ├── UserApi.kt              # 用户 API
│   │   │   ├── BookmarkApi.kt          # 收藏 API
│   │   │   └── RankingApi.kt           # 排行榜 API
│   │   ├── dto/pixiv/
│   │   │   ├── PixivResponse.kt        # 响应基类
│   │   │   ├── IllustDto.kt            # 插画 DTO
│   │   │   ├── UserDto.kt              # 用户 DTO
│   │   │   └── RankingDto.kt           # 排行榜 DTO
│   │   └── mapper/
│   │       └── PixivArtworkMapper.kt   # DTO 映射器
│   └── repository/
│       ├── ArtworkRepositoryImpl.kt    # 作品仓储实现
│       └── UserRepositoryImpl.kt       # 用户仓储实现
└── di/
    └── SharedModule.kt                 # Koin 配置
```

## 下一步开发建议

1. **持久化存储** - 使用 DataStore 保存配置
2. **缓存策略** - 实现本地缓存减少网络请求
3. **离线支持** - 缓存作品数据支持离线浏览
4. **用户体验** - 添加加载状态、错误重试等
5. **性能优化** - 图片预加载、分页加载优化
6. **单元测试** - 为 API 和 Repository 添加测试

## 技术支持

如有问题，请参考：

1. [Pixiv Web API 文档](https://github.com/AgMonk/pixiv-utils)
2. [Ktor 官方文档](https://ktor.io/)
3. [Kotlin Multiplatform 文档](https://kotlinlang.org/docs/multiplatform.html)

## 许可证

本集成基于开源项目 [pixiv-utils](https://github.com/AgMonk/pixiv-utils) 实现，仅供学习和研究使用。

