# Pixiv API 集成状态

> 📅 最后更新: 2025-11-20

## 📖 完整文档

- **API 测试计划**: [docs/guides/API_测试计划.md](../guides/API_测试计划.md)
- **测试工具使用指南**: [docs/guides/API测试工具使用指南.md](../guides/API测试工具使用指南.md)
- **API 集成指南**: [docs/pixiv/PIXIV_API_集成指南.md](../pixiv/PIXIV_API_集成指南.md)
- **使用示例**: [shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt](../../shared/src/commonMain/kotlin/com/projectu/shared/examples/PixivApiUsageExample.kt)

---

## 📊 整体进度

| 模块 | 总方法数 | 已实现 | 已测试 | 完成度 |
|------|---------|-------|-------|--------|
| IllustApi | 6 | 6 | 6 | ✅ 100% |
| UserApi | 10 | 10 | 10 | ✅ 100% |
| BookmarkApi | 8 | 8 | 8 | ✅ 100% |
| RankingApi | 2 | 2 | 2 | ✅ 100% |
| CommentApi | 8 | 8 | 8 | ✅ 100% |
| NovelApi | 5 | 5 | 5 | ✅ 100% |
| NovelSeriesApi | 5 | 5 | 5 | ✅ 100% |
| TagApi | 4 | 4 | 4 | ✅ 100% |
| MarkerApi | 3 | 3 | 3 | ✅ 100% |
| **总计** | **51** | **51** | **51** | **✅ 100%** |

---

## ✅ 已集成 API 模块

### IllustApi - 插画相关 API (6/6) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取作品详情 | `/ajax/illust/{illustId}` | ✅ | ✅ | ✅ | 完整作品信息 |
| 搜索作品 | `/ajax/search/artworks/{keyword}` | ✅ | ✅ | ✅ | 支持排序和筛选 |
| 推荐作品（初始化） | `/ajax/illust/recommend/init` | ✅ | ✅ | ✅ | 个性化推荐 |
| 推荐作品（续） | `/ajax/illust/recommend/illusts` | ✅ | ✅ | ✅ | 更多推荐 |
| 发现作品 | `/ajax/discovery/artworks` | ✅ | ✅ | ✅ | 编辑精选 |
| Ugoira 元数据 | `/ajax/illust/{illustId}/ugoira_meta` | ✅ | ✅ | ✅ | 动图 ZIP 和帧信息 |

### UserApi - 用户相关 API (10/10) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取用户信息 | `/ajax/user/{userId}` | ✅ | ✅ | ✅ | 用户基本信息 |
| 获取用户全部信息 | `/ajax/user/{userId}/profile/all` | ✅ | ✅ | ✅ | 包含作品、收藏统计 |
| 用户作品列表 | `/ajax/user/{userId}/profile/illusts` | ✅ | ✅ | ✅ | 支持分页 |
| 用户收藏列表 | `/ajax/user/{userId}/illusts/bookmarks` | ✅ | ✅ | ✅ | 支持公开/非公开 |
| 关注列表 | `/ajax/user/{userId}/following` | ✅ | ✅ | ✅ | 正在关注的用户 |
| 粉丝列表 | `/ajax/user/{userId}/followers` | ✅ | ✅ | ✅ | 粉丝用户列表 |
| 推荐用户(针对用户) | `/ajax/user/{userId}/recommends` | ✅ | ✅ | ✅ | 基于特定用户推荐 |
| 发现用户(总体推荐) | `/ajax/discovery/users` | ✅ | ✅ | ✅ | 推荐给当前账户 |
| 关注用户 | `/bookmark_add.php` | ✅ | ✅ | ✅ | POST请求 |
| 取消关注 | `/rpc_group_setting.php` | ✅ | ✅ | ✅ | POST请求 |

### BookmarkApi - 收藏相关 API (8/8) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 添加插画收藏 | `/ajax/illusts/bookmarks/add` | ✅ | ✅ | ✅ | 支持公开/非公开+标签 |
| 删除插画收藏 | `/ajax/illusts/bookmarks/delete` | ✅ | ✅ | ✅ | 单个删除 |
| 批量删除插画收藏 | `/ajax/illusts/bookmarks/delete` | ✅ | ✅ | ✅ | 批量删除 |
| 获取插画收藏标签 | `/ajax/user/{userId}/illusts/bookmark/tags` | ✅ | ✅ | ✅ | 公开/非公开标签列表 |
| 添加小说收藏 | `/ajax/novels/bookmarks/add` | ✅ | ✅ | ✅ | 支持公开/非公开+标签 |
| 删除小说收藏 | `/ajax/novels/bookmarks/delete` | ✅ | ✅ | ✅ | 单个删除 |
| 批量删除小说收藏 | `/ajax/novels/bookmarks/delete` | ✅ | ✅ | ✅ | 批量删除 |
| 获取小说收藏标签 | `/ajax/user/{userId}/novels/bookmark/tags` | ✅ | ✅ | ✅ | 公开/非公开标签列表 |

### RankingApi - 排行榜相关 API (2/2) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取插画排行榜 | `/ranking.php` | ✅ | ✅ | ✅ | 返回 JSON |
| 获取小说排行榜 | `/ajax/ranking/novel` | ✅ | ✅ | ✅ | 返回 JSON 🆕 |

**支持的排行榜模式** (RankingMode 枚举):
- **一般排行榜**: `daily`, `weekly`, `monthly`, `rookie`, `original`, `daily_ai`, `male`, `female`
- **R-18 排行榜**: `daily_r18`, `weekly_r18`, `daily_r18_ai`, `male_r18`, `female_r18`
- **R-18G 排行榜**: `r18g`

**内容类型** (RankingContent 枚举):
- `all` - 全部
- `illust` - 插画
- `manga` - 漫画
- `ugoira` - 动图

### CommentApi - 评论相关 API (8/8) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取插画评论根楼层 | `/ajax/illusts/comments/roots` | ✅ | ✅ | ✅ | 支持分页 |
| 获取评论回复 | `/ajax/illusts/comments/replies` | ✅ | ✅ | ✅ | 回复列表 |
| 发表插画评论 | `/rpc/post_comment.php` | ✅ | ✅ | ✅ | 文字+表情 |
| 删除插画评论 | `/rpc_delete_comment.php` | ✅ | ✅ | ✅ | 删除自己的评论 |
| 获取小说评论根楼层 | `/ajax/novels/comments/roots` | ✅ | ✅ | ✅ | 支持分页 |
| 获取小说评论回复 | `/ajax/novels/comments/replies` | ✅ | ✅ | ✅ | 回复列表 |
| 发表小说评论 | `/rpc/post_comment.php` | ✅ | ✅ | ✅ | 文字+表情 |
| 删除小说评论 | `/rpc_delete_comment.php` | ✅ | ✅ | ✅ | 删除自己的评论 |

### NovelApi - 小说相关 API (5/5) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取小说详情 | `/ajax/novel/{novelId}` | ✅ | ✅ | ✅ | 完整小说信息 |
| 获取小说收藏状态 | `/ajax/novel/{novelId}/bookmarkData` | ✅ | ✅ | ✅ | 收藏状态 |
| 搜索小说 | `/ajax/search/novels/{keyword}` | ✅ | ✅ | ✅ | 支持排序和筛选 |
| 发现小说 | `/ajax/discovery/novels` | ✅ | ✅ | ✅ | 编辑精选 |
| 关注作者的最新小说 | `/ajax/follow_latest/novel` | ✅ | ✅ | ✅ | 关注作者的新作 |

### NovelSeriesApi - 小说系列相关 API (5/5) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取小说系列详情 | `/ajax/novel/series/{seriesId}` | ✅ | ✅ | ✅ 已重构 | 2025-11-20 完全重构 |
| 获取系列内容列表 | `/ajax/novel/series_content/{seriesId}` | ✅ | ✅ | ✅ 已重构 | 2025-11-20 完全重构 |
| 获取系列标题列表 | `/ajax/novel/series/{seriesId}/content_titles` | ✅ | ✅ | ✅ 已简化 | 2025-11-20 简化结构 |
| 追更小说系列 | `/ajax/novel/series/{seriesId}/watch` | ✅ | ✅ | ✅ | 2025-11-20 新增，POST {} |
| 取消追更 | `/ajax/novel/series/{seriesId}/unwatch` | ✅ | ✅ | ✅ | 2025-11-20 新增，POST {} |

**重要更新**:
- `NovelSeriesBody` 完全重构，新增 30+ 字段
- `NovelSeriesContentBody` 完全重构，新增嵌套结构
- `watch`/`unwatch` 方法需发送空 JSON 对象 `{}`

### TagApi - 标签相关 API (4/4) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 获取标签建议 | `/ajax/tags/suggest_by_word` | ✅ | ✅ | ✅ 已修复 | TagCandidate 字段修正 |
| 获取搜索建议 | `/ajax/search/suggestion` | ✅ | ✅ | ✅ | 点击搜索框时触发 |
| 标签搜索建议 (RPC) | `/rpc/cps.php` | ✅ | ✅ | ✅ | 输入时的标签提示 |
| 获取标签信息 | `/ajax/tag/info` | ✅ | ✅ | ✅ 已重构 | TagInfoBody 完全重构 |

**新增接口详情**:
- **搜索建议** (`getSearchSuggestion`): 
  - 点击搜索框时触发，无需输入关键字
  - 返回热门标签(插画/小说)、推荐标签、基于标签的推荐、我的收藏标签、标签翻译、缩略图预览
  - 支持模式: `all`(全部作品), `r18`(R18作品)
- **标签搜索建议** (`getSearchSuggest`):
  - 输入关键字时的实时搜索建议
  - RPC 接口，返回格式不同于标准 PixivResponse

**DTO 修复记录**:
- `TagCandidate`: accessCount→illustCount, type→suggestType, 新增 totalCount
- `TagInfoBody`: 新增 TagTranslation 嵌套类，重构翻译字段结构
- `SearchSuggestionBody`: 包含 PopularTags, RecommendTags, TagTranslationInfo, ThumbnailInfo
- `ThumbnailInfo`: 完整的缩略图信息，包含 BookmarkData 对象

### MarkerApi - 阅读标记相关 API (3/3) ✅

| 功能 | 端点 | 实现 | 测试 | DTO状态 | 备注 |
|-----|------|------|------|---------|------|
| 添加小说阅读标记 | `/ajax/novel/marker/add` | ✅ | ✅ | ✅ | 保存阅读位置 |
| 删除小说阅读标记 | `/ajax/novel/marker/delete` | ✅ | ✅ | ✅ | 清除阅读位置 |
| 获取阅读标记列表 | `/ajax/novel/marker/list` | ✅ | ✅ | ✅ | 所有阅读记录 |

---

## 📝 重大更新记录

### 2025-11-20
- ✅ NovelSeriesApi 所有 DTO 完全重构
- ✅ 新增 watch/unwatch 接口（追更功能）
- ✅ TagApi 新增 getSearchSuggestion 接口（搜索建议）
- ✅ TagApi 新增 getSearchSuggest 接口（RPC标签搜索）
- ✅ TagApi DTO 修复（TagCandidate, TagInfoBody）
- ✅ 移除废弃的 getPopularTags 接口
- ✅ 新增语言支持：Thai (th)、Malay (ms)

### 2025-11-17
- ✅ 完成 CommentApi 全部 8 个方法
- ✅ 完成 NovelApi 全部 5 个方法
- ✅ 完成 NovelSeriesApi 初版 3 个方法
- ✅ 完成 TagApi 初版 2 个方法

### 2025-10-30
- ✅ 完成 IllustApi 全部 6 个方法
- ✅ 完成 UserApi 全部 9 个方法
- ✅ 完成 BookmarkApi 全部 8 个方法
- ✅ 完成 RankingApi 全部 2 个方法
- ✅ 创建 API 测试工具

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

### 获取小说系列详情（含追更）
```kotlin
// 获取系列详情
val seriesDetail = pixivApi.novelSeriesApi.getDetail("8174474")
println("系列名: ${seriesDetail.body.title}")
println("追更状态: ${seriesDetail.body.isWatched}")

// 追更系列
pixivApi.novelSeriesApi.watch("8174474")

// 取消追更
pixivApi.novelSeriesApi.unwatch("8174474")
```

### 获取标签建议
```kotlin
val suggestions = pixivApi.tagApi.getSuggestByWord("初音")
suggestions.body.candidates.forEach { candidate ->
    println("${candidate.tagName} - ${candidate.illustCount} 作品")
}
```

---

## 🧪 测试工具

本项目包含内置的 API 测试工具，可以在应用中直接测试所有 API：

1. 打开应用，进入 **设置** 页面
2. 点击 **API 测试工具 🛠️**
3. 选择要测试的 API 模块和方法
4. 输入参数后点击 **执行测试**
5. 查看结果（摘要 + JSON）

详细使用说明：[API测试工具使用指南.md](../guides/API测试工具使用指南.md)

---

## ⚠️ 已知问题和限制

1. **小说排行榜**: 返回 HTML 需要解析 `__NEXT_DATA__`，如果 Pixiv 改变 HTML 结构可能需要更新解析器
2. **POST 请求**: 部分 POST 接口（如 watch/unwatch）需要发送空 JSON 对象 `{}`
3. **PHPSESSID 有效期**: PHPSESSID 会过期，需要定期从浏览器重新获取

---

## 🎯 下一步计划

1. ✅ 所有 48 个 API 方法测试完成！
2. 🚀 开始实现 UI 功能（作品列表、详情、用户主页、小说阅读等）
3. 📝 补充更多 API 使用示例和文档
4. 🔧 持续修复发现的 DTO 问题
5. ✨ 实现更多 Pixiv API（关注推荐、漫画系列等）

---

> 💡 **提示**: API 详细使用方法请参考 [PIXIV_API_集成指南.md](../pixiv/PIXIV_API_集成指南.md)

