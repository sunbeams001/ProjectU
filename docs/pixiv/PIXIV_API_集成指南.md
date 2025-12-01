# Pixiv API 集成指南

> 📅 最后更新: 2025-11-17  
> 🌐 API 集成状态: [docs/shared/API_STATUS.md](../shared/API_STATUS.md)

## 📖 概述

ProjectU 已完整集成 Pixiv Web API，基于 Kotlin Multiplatform + Ktor，覆盖作品、用户、收藏、排行榜、评论、小说、标签等核心功能，支持 Android 和 Desktop 平台。

### 核心特性

- ✅ **跨平台**: Kotlin Multiplatform (Android + Desktop)
- ✅ **异步**: Kotlin Coroutines + Flow
- ✅ **网络**: Ktor 3.3.1 HTTP 客户端
- ✅ **认证**: PHPSESSID Cookie + CSRF Token 自动刷新
- ✅ **完整性**: 49+ API 方法，8 个主要模块，40+ DTO

### API 模块完成度

> 📊 详细 API 状态: [docs/shared/API_STATUS.md](../shared/API_STATUS.md)

| 模块 | 完成度 | 方法数 |
|------|--------|--------|
| IllustApi | 100% | 9 |
| UserApi | 100% | 7 |
| BookmarkApi | 100% | 8 |
| RankingApi | 100% | 2 |
| CommentApi ✨ | 100% | 7 |
| NovelApi ✨ | 100% | 6 |
| NovelSeriesApi ✨ | 100% | 3 |
| TagApi ✨ | 100% | 7 |
| **总计** | **100%** | **49** |

---

## 🚀 快速开始

### 1. 获取 PHPSESSID

从浏览器登录后获取认证信息：

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具 (F12)
3. Application/Storage → Cookies → `https://www.pixiv.net`
4. 复制 `PHPSESSID` 的值 (格式: `12345678_xxxxxxxxxxxx`)

⚠️ **安全提示**: 
- 不要将 PHPSESSID 硬编码到代码中
- 不要提交到版本控制系统
- Android 使用 EncryptedSharedPreferences
- Desktop 使用环境变量或加密配置

### 2. 配置 Pixiv API

#### 方式 1: 通过 DataStore (推荐)

```kotlin
// 存储 PHPSESSID
val pixivConfigStore = PixivConfigStore(context)
pixivConfigStore.updatePhpSessionId("你的PHPSESSID")

// 读取配置
val config = pixivConfigStore.config.first()
```

#### 方式 2: 直接创建 API 实例

```kotlin
val pixivApi = PixivApi.create(
    httpClient = httpClient,
    phpSessionId = "你的PHPSESSID",
    token = null,  // 可选，会自动获取
    host = "https://www.pixiv.net",
    lang = "zh"  // zh, zh_tw, en, ja, ko
)
```

### 3. 调用 API

#### 在 Repository 中使用

```kotlin
class ArtworkRepositoryImpl(
    private val pixivApi: PixivApi
) : ArtworkRepository {
    override suspend fun getRecommendedArtworks(): Result<List<Artwork>> {
        return try {
            val response = pixivApi.illustApi.getRecommendInit(
                pid = 123456,
                limit = 20
            )
            Result.success(response.body.illusts.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### 在 ViewModel 中使用

```kotlin
class HomeViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeScreenState())
    val state = _state.asStateFlow()
    
    fun loadArtworks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            artworkRepository.getRecommendedArtworks()
                .onSuccess { artworks ->
                    _state.update {
                        it.copy(artworks = artworks, isLoading = false)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = error.message, isLoading = false)
                    }
                }
        }
    }
}
```

### 4. 加载图片 (重要！)

Pixiv 图片需要带上 `Referer` 请求头：

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .setHeader("Referer", "https://www.pixiv.net/")
        .build(),
    contentDescription = null,
    modifier = Modifier.fillMaxWidth()
)
```

**Coil 配置 (推荐)**:

```kotlin
// 在应用启动时配置
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(KtorNetworkFetcherFactory(httpClient = ktorClient))
    }
    .okHttpClient {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Referer", "https://www.pixiv.net/")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    .build()
```

---

## 📁 代码结构

### 主要文件位置

```
shared/src/commonMain/kotlin/com/projectu/shared/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   ├── PixivApi.kt              # API 统一门面
│   │   │   ├── PixivApiClient.kt        # HTTP 客户端封装
│   │   │   ├── IllustApi.kt             # 插画 API
│   │   │   ├── UserApi.kt               # 用户 API
│   │   │   ├── BookmarkApi.kt           # 收藏 API
│   │   │   └── RankingApi.kt            # 排行榜 API
│   │   ├── dto/
│   │   │   ├── pixiv/
│   │   │   │   ├── PixivResponse.kt     # 通用响应
│   │   │   │   ├── IllustDto.kt         # 作品 DTO
│   │   │   │   ├── UserDto.kt           # 用户 DTO
│   │   │   │   └── RankingDto.kt        # 排行榜 DTO
│   │   │   └── ArtworkDto.kt
│   │   └── mapper/
│   │       └── PixivArtworkMapper.kt    # DTO → Domain 映射
│   ├── repository/
│   │   ├── ArtworkRepositoryImpl.kt     # 作品仓储实现
│   │   └── UserRepositoryImpl.kt        # 用户仓储实现
│   └── local/
│       ├── PixivConfig.kt               # Pixiv 配置模型
│       └── PixivConfigStore.kt          # 配置存储
├── domain/
│   ├── model/
│   │   └── Artwork.kt                   # 作品领域模型
│   └── repository/
│       ├── ArtworkRepository.kt         # 作品仓储接口
│       └── UserRepository.kt            # 用户仓储接口
└── examples/
    └── PixivApiUsageExample.kt          # 使用示例
```

### 依赖注入配置

`shared/src/commonMain/kotlin/com/projectu/shared/di/SharedModule.kt`

```kotlin
fun pixivApiModule(phpSessionId: String, token: String? = null) = module {
    single {
        PixivApiClient(
            httpClient = get(),
            phpSessionId = phpSessionId,
            token = token
        )
    }
    
    single {
        PixivApi(get())
    }
}
```

---

## 🎯 API 功能概览

> 📋 完整 API 列表和使用示例: [docs/shared/API_STATUS.md](../shared/API_STATUS.md)

### IllustApi - 插画相关

| 方法 | 功能 | 参数 |
|-----|------|------|
| `getDetail()` | 获取作品详情 | pid |
| `search()` | 搜索作品 | keyword, order, mode |
| `getRecommendInit()` | 推荐作品 | pid, limit |
| `getDiscovery()` | 发现作品 | mode, limit |
| `getUgoiraMeta()` | Ugoira 元数据 | pid |
| `getFollowLatest()` | 关注作者最新 | mode, page |
| `postLike()` | 点赞作品 | pid |

### UserApi - 用户相关

| 方法 | 功能 | 参数 |
|-----|------|------|
| `getUserInfo()` | 用户信息 | uid |
| `getProfileAll()` | 用户作品概况 | uid |
| `getProfileIllusts()` | 用户作品列表 | uid, ids |
| `getRecommendUsers()` | 推荐用户 | uid, userNum |
| `followUser()` | 关注用户 | userId |
| `unfollowUser()` | 取关用户 | userId |

### BookmarkApi - 收藏相关

| 方法 | 功能 | 参数 |
|-----|------|------|
| `getUserBookmarkIllusts()` | 查询用户收藏的插画·漫画 | uid, tag, offset, limit |
| `getUserBookmarkNovels()` | 查询用户收藏的小说 | uid, tag, offset, limit |
| `addIllust()` | 收藏作品 | illustId, restrict, tags |
| `deleteIllust()` | 删除收藏 | bookmarkId |
| `deleteIllusts()` | 批量删除 | bookmarkIds |

### RankingApi - 排行榜

| 方法 | 功能 | 参数 |
|-----|------|------|
| `getDailyRanking()` | 日榜 | page, content, date |
| `getWeeklyRanking()` | 周榜 | page, content, date |
| `getMonthlyRanking()` | 月榜 | page, content, date |
| `getRookieRanking()` | 新人榜 | page, content, date |

**支持的排行榜模式**:
- `daily` - 日榜
- `weekly` - 周榜
- `monthly` - 月榜
- `rookie` - 新人榜
- `original` - 原创榜
- `male` / `female` - 性别分类
- `*_r18` - R-18 系列

---

## ⚠️ 常见问题

### 1. 图片加载失败

**问题**: 图片显示 403 或显示占位符

**解决**: 确保添加 `Referer` 请求头
```kotlin
.setHeader("Referer", "https://www.pixiv.net/")
```

### 2. PHPSESSID 失效

**问题**: API 返回 401 或需要重新登录

**解决**: 
- PHPSESSID 有效期通常为 30 天
- 从浏览器重新获取最新的 PHPSESSID
- 实现自动刷新机制（监听 401 响应）

### 3. 频率限制

**问题**: 请求过快导致被限流

**解决**:
- 实现请求节流（Throttle）
- 添加本地缓存减少请求
- 使用分页加载避免一次性请求过多数据

### 4. Ugoira 动图播放

**问题**: 不知道如何播放 Ugoira 格式

**解决**:
```kotlin
// 1. 获取元数据
val meta = pixivApi.illustApi.getUgoiraMeta(pid)

// 2. 下载 ZIP 文件（meta.body.src）

// 3. 解压并按帧率播放
meta.body.frames.forEach { frame ->
    // delay(frame.delay)
    // showImage(frame.file)
}
```

---

## 🔗 相关文档

- 📊 [API 状态和详细方法签名](../shared/API_STATUS.md)
- 🏗️ [项目架构说明](../project/项目架构参考文档.md)
- ⚙️ [开发状态和路线图](../shared/DEVELOPMENT_STATUS.md)
- 🔧 [技术栈详情](../shared/TECH_STACK.md)
- 💡 [示例代码](../../shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt)

---

## 📝 更新日志

- **2025-10-30**: 简化文档，引用共享 API_STATUS.md
- **2025-06**: 完成 4 个主要 API 模块集成
- **2025-05**: 初始集成 pixiv-utils
