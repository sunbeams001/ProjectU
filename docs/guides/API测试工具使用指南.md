# API 测试工具使用指南

> 📅 创建日期: 2025-10-30  
> 🔄 更新日期: 2026-01-10  
> 🎯 目的: 介绍如何使用内置的 API 测试工具进行 API 调试和验证

---

## 📖 概述

本项目内置了完整的 **API 测试工具**，可以在应用中直接测试所有已集成的 Pixiv API。

> 📊 **API 统计**: 详见 [API_STATUS.md](../shared/API_STATUS.md) - 包含所有 API 方法的详细信息和测试状态

### ✨ 主要特性

- 🎯 **完整覆盖**: 支持所有已实现的 API 方法，涵盖插画、小说、用户、收藏、排行榜等多个模块
- 📋 **分类浏览**: 按模块和优先级（P0/P1/P2/P3）分类展示
- ⚙️ **动态参数**: 根据 API 自动生成参数输入表单，支持文本输入和下拉选择
- 📊 **双标签结果**: 提供格式化摘要和原始 JSON 两种结果展示方式
- ⚡ **实时测试**: 点击执行立即调用真实 API
- 🛡️ **错误处理**: 完整的错误信息和堆栈跟踪，便于调试 DTO 问题
- 🔐 **登录检测**: 自动检测登录状态，未登录时提示配置 PHPSESSID

---

## 🚀 快速开始

### 步骤 1: 配置登录凭据

在使用 API 测试工具之前，需要先配置 PHPSESSID：

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具 (F12)
3. 进入 **Application/Storage** → **Cookies** → `https://www.pixiv.net`
4. 复制 `PHPSESSID` 的值（格式: `12345678_xxxxxxxxxxxx`）
5. 在应用中进入 **设置** → **账号管理**
6. 点击 **PHPSESSID** 配置项，输入复制的值并保存

### 步骤 2: 打开 API 测试工具

1. 在应用中进入 **设置** 页面
2. 在 **"Pixiv"** 分组下，找到 **API 测试工具 🛠️**
3. 点击进入测试界面

### 步骤 3: 选择要测试的 API

**左侧面板** - API 方法选择器：

1. **选择模块**（12 个模块可选）：
   - 插画 API (IllustApi) - 8 个方法
   - 漫画系列 API (IllustSeriesApi) - 3 个方法
   - 用户 API (UserApi) - 16 个方法
   - 收藏 API (BookmarkApi) - 10 个方法
   - 排行榜 API (RankingApi) - 2 个方法
   - 评论 API (CommentApi) - 8 个方法
   - 小说 API (NovelApi) - 5 个方法
   - 关注 API (FollowApi) - 4 个方法
   - 小说系列 API (NovelSeriesApi) - 5 个方法
   - 标签 API (TagApi) - 8 个方法
   - 阅读标记 API (MarkerApi) - 4 个方法
   - 搜索 API (SearchApi) - 3 个方法
   - Pixivision API (PixivisionApi) - 2 个方法

2. **选择具体方法**：
   - **P0**（红色）: 核心功能，高优先级
   - **P1**（蓝色）: 重要功能，中优先级
   - **P2**（紫色）: 辅助功能，低优先级
   - **P3**（橙色）: 修改操作，需谨慎使用

### 步骤 4: 输入测试参数

**右侧上半部分** - 参数输入区：

1. 查看已自动填充的默认参数值
2. 根据需要修改参数：
   - **必填参数**标记有红色 `*`
   - 有预定义选项的参数显示为**下拉菜单**
   - 其他参数为**文本输入框**

3. 点击 **执行测试** 按钮

### 步骤 5: 查看测试结果

**右侧下半部分** - 结果展示区：

#### 成功时显示两个标签页：

- **摘要** - 格式化的关键信息展示，易于阅读
- **JSON** - 完整的 API 响应数据，可用于调试 DTO

#### 失败时显示：

- 错误消息（中文）
- 详细堆栈跟踪（用于调试）

---

## � 代码集成指南

如果你需要在代码中使用 Pixiv API，而不仅是通过测试工具，请参考以下指南。

### 1. 创建 API 实例

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

### 2. 在 Repository 中使用

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

### 3. 在 ViewModel 中使用

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

**Coil 全局配置 (推荐)**:

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

### 5. Ugoira 动图处理

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

## 📁 项目代码结构

### API 相关文件位置

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
│   │   │   ├── RankingApi.kt            # 排行榜 API
│   │   │   ├── CommentApi.kt            # 评论 API
│   │   │   ├── NovelApi.kt              # 小说 API
│   │   │   └── ... (其他 API 模块)
│   │   ├── dto/
│   │   │   └── pixiv/
│   │   │       ├── PixivResponse.kt     # 通用响应
│   │   │       ├── IllustDto.kt         # 作品 DTO
│   │   │       ├── UserDto.kt           # 用户 DTO
│   │   │       └── ... (其他 DTO)
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

composeApp/src/commonMain/kotlin/com/projectu/ui/screens/
└── apitest/
    ├── ApiTestModels.kt                 # 测试模型定义 (64个API)
    ├── ApiTestContract.kt               # 测试状态管理
    ├── ApiTestViewModel.kt              # 测试逻辑实现
    └── ApiTestScreen.kt                 # 测试界面 UI
```

---

## �📋 模块清单

### 1. IllustApi - 插画 API (7 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getIllustDetail | P0 | 获取作品详情 | `102814610` |
| getIllustPages | P0 | 获取多页作品详情 | `137776727` |
| getRecommendInit | P1 | 推荐作品初始化 | `102814610` |
| getRecommendIllusts | P1 | 获取推荐作品 | 作品ID列表 |
| getDiscoveryIllust | P1 | 发现作品 | mode=all |
| getUgoiraMetadata | P2 | 动图元数据 | `44298467` |

### 2. IllustSeriesApi - 漫画系列 API (3 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getIllustSeriesDetail | P0 | 漫画系列详情 | `313864` |
| watch | P1 | 追更漫画系列 | `313864` |
| unwatch | P1 | 取消追更漫画系列 | `313864` |

⚠️ **注意**: watch/unwatch 需要发送空 JSON 对象 `{}`

### 3. UserApi - 用户 API (12 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getUserInfo | P0 | 获取用户信息 | `11` |
| getProfileAll | P0 | 用户作品概况 | `11` |
| getUserIllusts | P1 | 用户作品列表 | `11` |
| getProfileNovels | P1 | 用户小说作品 | `18662946` |
| getUserFollowDetail | P1 | 用户关注详情 | `58277` |
| getDiscoveryUsers | P1 | 发现用户(总体推荐) | limit=20 |
| getUserFollowing | P2 | 用户关注列表 | `11` |
| getUserFollowers | P2 | 用户粉丝列表 | `11` |
| getMyPixiv | P2 | 好P友列表 | `4966721` |
| getRecommendUsers | P2 | 推荐用户(针对特定用户) | `11` |
| followUser | P3 | 关注用户 ⚠️ | `11` |
| unfollowUser | P3 | 取消关注 ⚠️ | `11` |

### 4. BookmarkApi - 收藏 API (10 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| addBookmark | P0 | 添加插画收藏 ⚠️ | `102814610` |
| deleteBookmark | P0 | 删除插画收藏 ⚠️ | 收藏ID |
| getUserBookmarkIllusts | P1 | 查询用户收藏的插画 | `11` |
| getUserBookmarkNovels | P1 | 查询用户收藏的小说 | `11` |
| addNovelBookmark | P1 | 添加小说收藏 ⚠️ | 小说ID |
| deleteNovelBookmark | P1 | 删除小说收藏 ⚠️ | 收藏ID |
| deleteBookmarks | P1 | 批量删除插画收藏 ⚠️ | 收藏ID列表 |
| getIllustBookmarkTags | P2 | 插画收藏标签 | `11` |
| deleteNovelBookmarks | P2 | 批量删除小说收藏 ⚠️ | 收藏ID列表 |
| getNovelBookmarkTags | P2 | 小说收藏标签 | `11` |

### 5. RankingApi - 排行榜 API (2 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getIllustRanking | P0 | 获取插画排行榜 | mode=daily |
| getNovelRanking | P1 | 获取小说排行榜(JSON) | mode=daily |

**支持的排行榜模式**:
- 一般: daily, weekly, monthly, rookie, original, daily_ai, male, female
- R-18: daily_r18, weekly_r18, daily_r18_ai, male_r18, female_r18
- R-18G: r18g

### 6. CommentApi - 评论 API (8 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getIllustCommentRoots | P1 | 获取插画评论 | `102814610` |
| getNovelCommentRoots | P5 | 获取小说评论 | `15809265` |
| getCommentReplies | P2 | 获取评论回复 | 评论ID |
| getNovelCommentReplies | P6 | 获取小说评论回复 | `50155161` |
| postIllustComment | P3 | 发布插画评论 ⚠️ | `102814610` |
| deleteIllustComment | P4 | 删除插画评论 ⚠️ | `102814610` + 评论ID |
| postNovelComment | P7 | 发布小说评论 ⚠️ | `15809265` |
| deleteNovelComment | P8 | 删除小说评论 ⚠️ | `15809265` + 评论ID |

### 7. NovelApi - 小说 API (3 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getNovelDetail | P0 | 获取小说详情 | `15809265` |
| getNovelBookmarkData | P1 | 小说收藏状态 | `15809265` |
| getNovelDiscovery | P1 | 发现小说 | mode=all |

### 8. FollowApi - 关注 API (4 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getFollowLatestIllust | P0 | 关注作者最新插画 | mode=all |
| getFollowLatestNovel | P0 | 关注作者最新小说 | mode=all |
| getWatchListManga | P1 | 漫画追更列表 | page=1 |
| getWatchListNovel | P1 | 小说追更列表 | page=1 |

### 9. NovelSeriesApi - 小说系列 API (5 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getNovelSeriesDetail | P1 | 小说系列详情 | `8174474` |
| getNovelSeriesContents | P1 | 系列内容列表 | `8174474` |
| getNovelSeriesTitles | P2 | 系列标题列表 | `8174474` |
| watch | P3 | 追更小说系列 ⚠️ | `8174474` |
| unwatch | P3 | 取消追更 ⚠️ | `8174474` |

### 10. TagApi - 标签 API (4 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getTagSuggest | P1 | 标签搜索建议 (Ajax) | `RO635` |
| getSearchSuggestion | P1 | 搜索建议（点击搜索框） | mode=all |
| getTagSearchSuggest | P1 | 标签搜索建议 (RPC) | `RO635` |
| getTagInfo | P2 | 标签信息 | `初音ミク` |

### 11. MarkerApi - 阅读标记 API (3 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| addNovelMarker | P1 | 添加小说书签 ⚠️ | `15809265` |
| deleteNovelMarker | P1 | 删除小说书签 ⚠️ | `15809265` |
| getNovelMarkerList | P2 | 获取小说书签列表 | - |

### 12. SearchApi - 搜索 API (3 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| searchIllust | P0 | 搜索作品 | `初音ミク` |
| searchNovel | P0 | 搜索小说 | `初音ミク` |
| searchUser | P0 | 搜索用户 | `少女` |

### 13. PixivisionApi - Pixivision 特辑 API (2 个方法) ✅ 100%

| 方法 | 优先级 | 说明 | 默认测试参数 |
|-----|--------|------|------------|
| getArticleList | P0 | 获取文章列表 | category=illustration, lang=zh |
| getArticleDetail | P0 | 获取文章详情 | `11373` |

**特别说明**:
- **数据来源**: Pixivision (https://www.pixivision.net/) 是Pixiv的附属网站，提供各种主题的插画、漫画特辑
- **数据格式**: 返回HTML，工具会自动使用Ksoup解析
- **支持类别**: illustration(插画特辑)、manga(漫画特辑)
- **支持语言**: zh(简体中文), zh-tw(繁体中文), en(英语), ja(日语), ko(韩语)
- **分页支持**: 使用page参数进行翻页

---

## ⚠️ 注意事项

### 1. 修改类 API 需谨慎

以下 API 会修改真实数据，测试时请小心：

- ⚠️ **收藏操作**: addBookmark, deleteBookmark, addNovelBookmark, deleteNovelBookmark
- ⚠️ **关注操作**: followUser, unfollowUser
- ⚠️ **评论操作**: postIllustComment, deleteIllustComment, postNovelComment, deleteNovelComment
- ⚠️ **追更操作**: watch, unwatch（漫画系列、小说系列）
- ⚠️ **阅读标记**: addNovelMarker, deleteNovelMarker

**建议**: 使用测试账号，或者测试后立即撤销操作。

### 2. 登录状态

大部分 API 需要登录才能使用：
- 未登录时，工具会显示 **"未登录"** 提示
- 请确保已正确配置 PHPSESSID
- PHPSESSID 会过期，过期后需要重新获取

### 3. 参数格式要求

某些参数有特定的格式要求：
- **date** 参数: 格式必须是 `yyyyMMdd`（如 `20231201`）
- **ID 类参数**: 必须是有效的数字字符串
- **列表参数**: 使用逗号分隔（如 `123,456,789`）

### 4. POST 请求特殊处理

部分 POST 请求需要特殊处理：
- **watch/unwatch**: 需要发送空 JSON 对象 `{}`
- **评论操作**: 需要提供 userId 参数

---

## 🛠️ 调试技巧

### 技巧 1: 使用默认参数快速测试

工具为每个 API 方法都配置了合理的默认参数，可以直接点击执行测试：
- 作品 ID: `102814610` (一个流行的初音作品)
- 用户 ID: `11` (Pixiv 官方账号)
- 小说 ID: `15809265`
- 系列 ID: `8174474`

### 技巧 2: 查看原始 JSON 调试 DTO

如果遇到 JSON 解析错误：
1. 切换到 **JSON** 标签页
2. 复制完整的 JSON 响应
3. 对比 DTO 定义，找出不匹配的字段
4. 修改 `shared/data/remote/dto/` 下对应的 DTO 文件
5. 重新编译测试

### 技巧 3: 理解错误信息

常见错误及解决方案：

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `401 Unauthorized` | PHPSESSID 过期或无效 | 重新获取 PHPSESSID |
| `403 Forbidden` | 缺少必要的请求头或访问频率过高 | 检查请求配置，降低请求频率 |
| `Fields [xxx] are required...` | DTO 定义与实际 API 响应不匹配 | 查看 JSON 标签页，修改 DTO |
| `不正确的请求` | 参数错误或请求体格式不正确 | 检查参数格式和必填项 |

### 技巧 4: 排行榜测试

- **插画排行榜**: 返回 JSON，直接解析
- **小说排行榜**: 返回 HTML，工具会自动提取 `__NEXT_DATA__` JSON
- 每页固定 50 条数据
- 支持所有 RankingMode 枚举值

### 技巧 5: 图片加载问题

如果在应用中加载 Pixiv 图片时遇到 403 错误：

**问题**: 图片显示 403 或显示占位符

**解决**: 确保添加 `Referer` 请求头
```kotlin
.setHeader("Referer", "https://www.pixiv.net/")
```

参考上方 [代码集成指南 - 加载图片](#4-加载图片-重要) 部分获取完整配置。

---

## 🔧 技术细节

### 认证方式

- **PHPSESSID Cookie**: 从浏览器登录后获取，存储在 DataStore 中
- **CSRF Token**: 自动获取和刷新，存储在内存中
- **安全提示**: 
  - Android 使用 EncryptedSharedPreferences
  - Desktop 使用环境变量或加密配置
  - 不要将 PHPSESSID 硬编码到代码中

### 技术栈

- **Kotlin Multiplatform**: 跨平台支持 (Android + Desktop)
- **Ktor 3.3.1**: HTTP 客户端
- **Kotlin Coroutines**: 异步处理
- **Kotlinx.serialization**: JSON 解析
- **DataStore**: 配置持久化

### 频率限制处理

**问题**: 请求过快导致被限流

**解决**:
- 实现请求节流（Throttle）
- 添加本地缓存减少请求
- 使用分页加载避免一次性请求过多数据

---

## 📊 测试进度

> 📈 **完整的测试统计**: 详见 [API_STATUS.md](../shared/API_STATUS.md#整体进度) - 包含所有模块的方法数量、测试状态和完成度

所有已实现的 API 方法均已通过测试，测试覆盖率 100%。每个模块都经过严格验证，确保功能正常。

---

## 📄 相关文档

- [API 状态文档](../shared/API_STATUS.md) - API 实现状态和详细信息
- [项目架构文档](../project/项目架构参考文档.md) - 整体架构设计
- [使用示例代码](../../shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt) - 完整的代码示例

---

## 💡 最佳实践

1. **从 P0 API 开始测试** - 先确保核心功能正常
2. **使用测试账号** - 避免误操作影响主账号数据
3. **保存 JSON 响应** - 便于后续 DTO 调试和文档编写
4. **定期更新 PHPSESSID** - 避免因过期导致测试失败
5. **注意请求频率** - 过于频繁的请求可能被 Pixiv 限制

---

> 📝 **提示**: 所有 API 方法的详细端点、参数说明和测试状态请参考 [API_STATUS.md](../shared/API_STATUS.md)
