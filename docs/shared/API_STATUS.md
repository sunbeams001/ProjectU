# Pixiv API 集成状态

> 📅 最后更新: 2025-10-30

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
| 添加收藏 | `/ajax/illusts/bookmarks/add` | ✅ | 支持公开/非公开 |
| 删除收藏 | `/rpc/index.php` (delete) | ✅ | 批量删除支持 |
| 获取收藏标签 | `/ajax/user/{userId}/illusts/bookmark/tags` | ✅ | 用户的收藏标签 |

### RankingApi - 排行榜相关 API
| 功能 | 端点 | 状态 | 说明 |
|-----|------|------|------|
| 获取排行榜 | `/ranking.php` | ✅ | 支持多种模式 |

**支持的排行榜模式**:
- `daily` - 日榜
- `weekly` - 周榜
- `monthly` - 月榜
- `rookie` - 新人榜
- `original` - 原创榜
- `male` - 男性向榜
- `female` - 女性向榜
- `daily_r18` - R-18 日榜
- `weekly_r18` - R-18 周榜
- `male_r18` - R-18 男性向
- `female_r18` - R-18 女性向

---

## ⏳ 计划中的 API

### CommentApi - 评论相关 API (P1)
- [ ] 获取作品评论
- [ ] 发表评论
- [ ] 删除评论
- [ ] 回复评论

### NovelApi - 小说相关 API (P2)
- [ ] 获取小说详情
- [ ] 搜索小说
- [ ] 推荐小说
- [ ] 小说排行榜

### TagApi - 标签相关 API (P2)
- [ ] 热门标签
- [ ] 标签搜索建议
- [ ] 标签统计信息

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

| API 模块 | 方法数 | 完成度 | 测试覆盖 |
|---------|-------|--------|---------|
| IllustApi | 6 | 100% | ✅ |
| UserApi | 5 | 100% | ✅ |
| BookmarkApi | 3 | 100% | ✅ |
| RankingApi | 1 | 100% | ✅ |
| CommentApi | 0 | 0% | ⏳ |
| NovelApi | 0 | 0% | ⏳ |
| **总计** | **15** | **85%** | - |

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
