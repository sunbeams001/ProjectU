# Pixiv API 集成状态

> 📅 最后更新: 2025-11-17

## 📖 完整文档

- **快速开始**: [docs/pixiv/PIXIV_API_集成指南.md](../pixiv/PIXIV_API_集成指南.md)
- **使用示例**: [shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt)

---

## ✅ 已集成 API 模块

### IllustApi - 插画相关 API
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取作品详情 | `/ajax/illust/{illustId}` | ✅ | 包含完整作品信息 |
| 获取作品页面 | `/ajax/illust/{illustId}/pages` | ✅ | 多图作品的所有页面 |
| 搜索作品 | `/ajax/search/artworks/{keyword}` | ✅ | 支持排序和筛选 |
| 推荐作品 | `/ajax/illust/recommend/init` | ✅ | 个性化推荐 |
| 发现作品 | `/ajax/discovery/artworks` | ✅ | 编辑精选 |
| Ugoira 元数据 | `/ajax/illust/{illustId}/ugoira_meta` | ✅ | 动图 ZIP 和帧信息 |

### UserApi - 用户相关 API
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取用户信息 | `/ajax/user/{userId}` | ✅ | 用户基本信息 |
| 获取用户全部信息 | `/ajax/user/{userId}/profile/all` | ✅ | 包含作品、收藏统计 |
| 用户作品列表 | `/ajax/user/{userId}/profile/illusts` | ✅ | 支持分页 |
| 用户收藏列表 | `/ajax/user/{userId}/illusts/bookmarks` | ✅ | 支持公开/非公开 |
| 关注列表 | `/ajax/user/{userId}/following` | ✅ | 正在关注的用户 |

### BookmarkApi - 收藏相关 API
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 添加插画收藏 | `/ajax/illusts/bookmarks/add` | ✅ | 支持公开/非公开 |
| 删除插画收藏 | `/ajax/illusts/bookmarks/delete` | ✅ | 批量删除支持 |
| 添加小说收藏 | `/ajax/novels/bookmarks/add` | ✅ | 支持公开/非公开 |
| 删除小说收藏 | `/ajax/novels/bookmarks/delete` | ✅ | 批量删除支持 |
| 获取收藏标签 | `/ajax/user/{userId}/illusts/bookmark/tags` | ✅ | 用户的收藏标签 |

### RankingApi - 排行榜相关 API
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取插画排行榜 | `/ranking.php` | ✅ | 统一接口，支持枚举参数，返回 JSON |
| 获取小说排行榜 | `/novel/ranking.php` | ✅ | 参数同插画，返回 HTML |

**支持的排行榜模式** (RankingMode 枚举):
- **一般排行榜**: `daily`, `weekly`, `monthly`, `rookie`, `original`, `daily_ai`✨, `male`, `female`
- **R-18 排行榜**: `daily_r18`, `weekly_r18`, `daily_r18_ai`✨, `male_r18`, `female_r18`

**内容类型** (RankingContent 枚举):
- `all` - 全部
- `illust` - 插画
- `manga` - 漫画
- `ugoira` - 动图

**响应格式**:
- 插画排行榜: JSON (RankingResponse)
- 小说排行榜: HTML (String)

### CommentApi - 评论相关 API ✨新增
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取插画评论根楼层 | `/ajax/illusts/comments/roots` | ✅ | 支持分页 |
| 获取评论回复 | `/ajax/illusts/comments/replies` | ✅ | 获取评论的回复列表 |
| 发表插画评论 | `/rpc/post_comment.php` | ✅ | 支持文字和表情评论 |
| 删除插画评论 | `/rpc_delete_comment.php` | ✅ | 删除自己的评论 |
| 获取小说评论根楼层 | `/ajax/novels/comments/roots` | ✅ | 支持分页 |
| 发表小说评论 | `/rpc/post_comment.php` | ✅ | 支持文字和表情评论 |
| 删除小说评论 | `/rpc_delete_comment.php` | ✅ | 删除自己的评论 |

### NovelApi - 小说相关 API ✨新增
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取小说详情 | `/ajax/novel/{novelId}` | ✅ | 包含完整小说信息 |
| 获取小说收藏状态 | `/ajax/novel/{novelId}/bookmarkData` | ✅ | 收藏状态信息 |
| 搜索小说 | `/ajax/search/novels/{keyword}` | ✅ | 支持排序和筛选 |
| 发现小说 | `/ajax/discovery/novels` | ✅ | 编辑精选 |
| 关注作者的最新小说 | `/ajax/follow_latest/novel` | ✅ | 获取关注作者的新作 |
| 获取新作小说 | `/ajax/novel/new` | ✅ | 最新发布的小说 |

### NovelSeriesApi - 小说系列相关 API ✨新增
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取小说系列详情 | `/ajax/novel/series/{seriesId}` | ✅ | 系列基本信息 |
| 获取系列内容列表 | `/ajax/novel/series_content/{seriesId}` | ✅ | 系列中的小说列表 |
| 获取系列标题列表 | `/ajax/novel/series/{seriesId}/content_titles` | ✅ | 系列中各篇标题 |

### TagApi - 标签相关 API ✨新增
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取标签建议 | `/ajax/tags/suggest_by_word` | ✅ | 搜索或添加标签时的建议 |
| 获取标签信息 | `/ajax/tag/info` | ✅ | 标签详细信息 |
| 为插画添加标签 | `/ajax/tags/illust/{illustId}/add` | ✅ | 添加作品标签 |
| 删除插画标签 | `/ajax/tags/illust/{illustId}/delete` | ✅ | 删除作品标签 |
| 为小说添加标签 | `/ajax/tags/novel/{novelId}/add` | ✅ | 添加小说标签 |
| 删除小说标签 | `/ajax/tags/novel/{novelId}/delete` | ✅ | 删除小说标签 |
| 获取热门标签 | `/ajax/tags/popular` | ✅ | 当前热门标签 |

---

## ⏳ 计划中的 API

### FollowApi - 关注相关 API (P2)
- [ ] 关注用户
- [ ] 取消关注
- [ ] 获取粉丝列表
- [ ] 获取关注推荐

### MangaSeriesApi - 漫画系列相关 API (P3)
- [ ] 获取漫画系列详情
- [ ] 获取系列内容列表

---

## 🔐 认证方式

### 当前实现
- ✅ **PHPSESSID Cookie 认证**
  - 从浏览器登录后获取
  - 存储在 DataStore 中
  - 自动附加到所有请求

- ✅ **CSRF Token**
  - 自动获取和刷新
  - 需要时从服务器获取最新 token
  - 存储在内存中

### 使用方法

```kotlin
// 1. 配置 PHPSESSID
val pixivConfig = PixivConfig(
    phpSessionId = "12345678_xxxxxxxxxxxx"  // 从浏览器 Cookie 获取
)

// 2. 创建 API 实例
val pixivApi = PixivApi.create(
    httpClient = httpClient,
    phpSessionId = pixivConfig.phpSessionId
)

// 3. 调用 API
val artworkDetail = pixivApi.illustApi.getIllustDetail("123456")
```

---

## 📊 API 使用统计

| API 模块 | 方法数 | 完成度 | 最后更新 |
|---------|-------|--------|---------|
| IllustApi | 9 | 100% | 2025-10-30 |
| UserApi | 7 | 100% | 2025-10-30 |
| BookmarkApi | 8 | 100% | 2025-10-30 |
| RankingApi | 2 | 100% | 2025-10-30 |
| CommentApi | 7 | 100% | 2025-11-17 ✨ |
| NovelApi | 6 | 100% | 2025-11-17 ✨ |
| NovelSeriesApi | 3 | 100% | 2025-11-17 ✨ |
| TagApi | 7 | 100% | 2025-11-17 ✨ |
| **总计** | **49** | **100%** | - |

---

## 🔄 数据流程

```
UI Layer
   ↓ call ViewModel
ViewModel
   ↓ call UseCase
UseCase
   ↓ call Repository
Repository
   ↓ call PixivApi
PixivApi
   ↓ HTTP Request
Pixiv Server
   ↓ JSON Response
DTO
   ↓ Mapper
Domain Model
   ↓ return
UI Layer
```

---

## 📝 API 使用示例

查看完整示例: [PixivApiUsageExample.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt)

### 获取作品详情
```kotlin
val illustDetail = pixivApi.illustApi.getIllustDetail("123456")
println("标题: ${illustDetail.body.title}")
println("作者: ${illustDetail.body.userName}")
```

### 搜索作品
```kotlin
val searchResult = pixivApi.illustApi.searchArtworks(
    keyword = "初音ミク",
    order = "date_desc",
    mode = "all"
)
searchResult.body.illust.data.values.forEach { illust ->
    println("${illust.title} - ${illust.userName}")
}
```

### 添加收藏
```kotlin
pixivApi.bookmarkApi.addBookmark(
    illustId = "123456",
    restrict = BookmarkRestrict.PUBLIC,
    tags = listOf("可爱", "初音ミク")
)
```

---

> 💡 **提示**: API 详细使用方法请参考 [PIXIV_API_集成指南.md](../pixiv/PIXIV_API_集成指南.md)
