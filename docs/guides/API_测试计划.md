# Pixiv API 调试测试计划

> 📅 创建日期: 2025-10-30  
> 🔄 更新日期: 2025-11-20  
> 🎯 目的: 系统化测试所有已集成的 Pixiv API，确保接口参数和返回值与实际一致

---

## 📋 测试前准备

### 1. 获取测试凭据

1. 在浏览器中登录 [Pixiv](https://www.pixiv.net/)
2. 打开开发者工具 (F12)
3. Application/Storage → Cookies → `https://www.pixiv.net`
4. 复制 `PHPSESSID` 的值（格式: `12345678_xxxxxxxxxxxx`）
5. 在应用中通过登录界面输入 PHPSESSID

### 2. 测试环境配置

- **Android**: API 24+ 设备或模拟器
- **Desktop**: Windows/Mac/Linux 开发环境
- **网络**: 可访问 Pixiv 的网络环境（可能需要代理）
- **测试工具**: 内置 API 测试模块（在应用设置中访问）

---

## 🧪 API 测试清单

### IllustApi - 插画相关 API

> 📁 文件: `shared/data/remote/api/IllustApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getIllustDetail` | `/ajax/illust/{illustId}` | P0 | ✅ | 测试作品ID: `102814610` |
| `searchIllust` | `/ajax/search/artworks/{keyword}` | P0 | ✅ | 测试关键词: `初音ミク` |
| `getRecommendedIllust` | `/ajax/illust/recommend/init` | P1 | ✅ | 需要登录 |
| `getRelatedIllust` | `/ajax/illust/{illustId}/recommend` | P1 | ✅ | 相关作品推荐 |
| `getDiscoveryIllust` | `/ajax/discovery/artworks` | P1 | ✅ | 发现页作品 |
| `getUgoiraMetadata` | `/ajax/illust/{illustId}/ugoira_meta` | P2 | ✅ | 动图元数据 |

---

### UserApi - 用户相关 API

> 📁 文件: `shared/data/remote/api/UserApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getUserInfo` | `/ajax/user/{userId}` | P0 | ✅ | 获取用户基本信息 |
| `getProfileAll` | `/ajax/user/{userId}/profile/all` | P0 | ✅ | 用户作品概况（作品ID列表） |
| `getProfileIllusts` | `/ajax/user/{userId}/profile/illusts` | P1 | ✅ | 用户作品详细信息 |
| `getUserBookmarkIllusts` | `/ajax/user/{userId}/illusts/bookmarks` | P1 | ✅ | 用户收藏的插画 |
| `getUserFollowing` | `/ajax/user/{userId}/following` | P2 | ✅ | 用户关注列表 |
| `getUserFollowers` | `/ajax/user/{userId}/followers` | P2 | ✅ | 用户粉丝列表 |
| `getRecommendUsers` | `/ajax/user/{userId}/recommends` | P2 | ✅ | 推荐用户 |
| `followUser` | `POST /bookmark_add.php` | P1 | ✅ | 关注用户 |
| `unfollowUser` | `POST /rpc_group_setting.php` | P1 | ✅ | 取消关注用户 |

---

### BookmarkApi - 收藏相关 API

> 📁 文件: `shared/data/remote/api/BookmarkApi.kt`  
> ✅ 状态: 已集成，已测试

#### 插画收藏 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `addIllust` | `POST /bookmark_add.php` | P0 | ✅ | 添加插画收藏 |
| `deleteIllust` | `POST /ajax/illusts/bookmarks/remove` | P0 | ✅ | 删除单个插画收藏 |
| `deleteIllusts` | `POST /ajax/illusts/bookmarks/remove` | P1 | ✅ | 批量删除插画收藏 |
| `getIllustBookmarkTags` | `GET /ajax/user/{userId}/illusts/bookmark/tags` | P2 | ✅ | 获取插画收藏标签 |

#### 小说收藏 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `addNovel` | `POST /novel/bookmark_add.php` | P0 | ✅ | 添加小说收藏 |
| `deleteNovel` | `POST /ajax/novels/bookmarks/remove` | P0 | ✅ | 删除单个小说收藏 |
| `deleteNovels` | `POST /ajax/novels/bookmarks/remove` | P1 | ✅ | 批量删除小说收藏 |
| `getNovelBookmarkTags` | `GET /ajax/user/{userId}/novels/bookmark/tags` | P2 | ✅ | 获取小说收藏标签 |

---

### RankingApi - 排行榜相关 API

> 📁 文件: `shared/data/remote/api/RankingApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getIllustRanking` | `/ranking.php` | P0 | ✅ | 插画排行榜 |
| `getNovelRanking` | `/novel/ranking.php` | P1 | ✅ | 小说排行榜（HTML解析） |

---

### CommentApi - 评论相关 API

> 📁 文件: `shared/data/remote/api/CommentApi.kt`  
> ✅ 状态: 已集成，已测试

#### 插画评论 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getIllustCommentRoots` | `/ajax/illusts/comments/roots` | P1 | ✅ | 获取插画根评论 |
| `getCommentReplies` | `/ajax/illusts/comments/replies` | P2 | ✅ | 获取评论回复 |
| `postIllustComment` | `POST /rpc_post_comment.php` | P1 | ✅ | 发表插画评论 |
| `deleteIllustComment` | `POST /rpc_delete_comment.php` | P1 | ✅ | 删除插画评论 |

#### 小说评论 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getNovelCommentRoots` | `/ajax/novels/comments/roots` | P1 | ✅ | 获取小说根评论 |
| `getNovelCommentReplies` | `/ajax/novels/comments/replies` | P2 | ✅ | 获取评论回复 |
| `postNovelComment` | `POST /novel/rpc_post_comment.php` | P1 | ✅ | 发表小说评论 |
| `deleteNovelComment` | `POST /novel/rpc_delete_comment.php` | P1 | ✅ | 删除小说评论 |

---

### NovelApi - 小说相关 API

> 📁 文件: `shared/data/remote/api/NovelApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getNovelDetail` | `/ajax/novel/{novelId}` | P0 | ✅ | 小说详情 |
| `getNovelBookmarkData` | `/ajax/novel/{novelId}/bookmarkData` | P1 | ✅ | 小说收藏数据 |
| `searchNovel` | `/ajax/search/novels/{keyword}` | P0 | ✅ | 搜索小说 |
| `getNovelDiscovery` | `/ajax/discovery/novels` | P1 | ✅ | 发现页小说 |
| `getNovelFollowLatest` | `/ajax/novel/follow_latest` | P1 | ✅ | 关注作者最新小说 |

---

### NovelSeriesApi - 小说系列相关 API

> 📁 文件: `shared/data/remote/api/NovelSeriesApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getDetail` | `/ajax/novel/series/{seriesId}` | P0 | ✅ | 系列详情 |
| `getContents` | `/ajax/novel/series_content/{seriesId}` | P0 | ✅ | 系列内容列表 |
| `getTitles` | `/ajax/novel/series/{seriesId}/content_titles` | P1 | ✅ | 系列标题列表 |
| `watch` | `POST /ajax/novel/series/{seriesId}/watch` | P1 | ✅ | 追更系列 |
| `unwatch` | `POST /ajax/novel/series/{seriesId}/unwatch` | P1 | ✅ | 取消追更 |

⚠️ **注意**: 
- DTO 已完全重构以匹配实际响应
- `watch` 和 `unwatch` 需要发送空 JSON 对象 `{}`
- 所有方法已通过测试验证

---

### TagApi - 标签相关 API

> 📁 文件: `shared/data/remote/api/TagApi.kt`  
> ✅ 状态: 已集成，已测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getSuggestByWord` | `/ajax/tags/suggest_by_word` | P1 | ✅ | 添加标签时的建议 |
| `getSearchSuggestion` | `/ajax/search/suggestion` | P1 | ✅ | 点击搜索框时触发 |
| `getSearchSuggest` | `/rpc/cps.php` | P1 | ✅ | RPC 标签搜索建议 |
| `getTagInfo` | `/ajax/tags/{tag}` | P2 | ✅ | 标签信息 |

⚠️ **注意**: 
- `TagCandidate` DTO 已修复（illustCount, totalCount, suggestType）
- `TagInfoBody` 已重构，包含完整的翻译信息（en, ja, en_new, ja_new）
- **新增** `getSearchSuggestion`: 返回热门标签、推荐标签、我的收藏标签、标签翻译、缩略图预览
  - 支持模式: `all`(全部作品), `r18`(R18作品)
  - 包含 DTO: SearchSuggestionBody, PopularTags, TagTranslationInfo, ThumbnailInfo, BookmarkData
- **新增** `getSearchSuggest`: RPC 接口，输入关键字时的实时搜索建议

---

### MarkerApi - 阅读标记相关 API

> 📁 文件: `shared/data/remote/api/MarkerApi.kt`  
> ✅ 状态: 已集成并测试完成

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `addNovelMarker` | `POST /ajax/novel/markers/add` | P2 | ✅ | 添加小说阅读标记 |
| `deleteNovelMarker` | `POST /ajax/novel/markers/delete` | P2 | ✅ | 删除小说阅读标记 |
| `getNovelMarkerList` | `/ajax/novel/markers` | P2 | ✅ | 获取小说阅读标记列表 |

---

## 📊 测试进度追踪

| 模块 | 总方法数 | 已测试 | 通过 | 失败 | 进度 |
|------|---------|--------|------|------|------|
| IllustApi | 6 | 6 | 6 | 0 | 100% |
| UserApi | 9 | 9 | 9 | 0 | 100% |
| BookmarkApi | 8 | 8 | 8 | 0 | 100% |
| RankingApi | 2 | 2 | 2 | 0 | 100% |
| CommentApi | 8 | 8 | 8 | 0 | 100% |
| NovelApi | 5 | 5 | 5 | 0 | 100% |
| NovelSeriesApi | 5 | 5 | 5 | 0 | 100% |
| TagApi | 4 | 4 | 4 | 0 | 100% |
| MarkerApi | 3 | 3 | 3 | 0 | 100% |
| **总计** | **50** | **50** | **50** | **0** | **100%** |

---

## 🛠️ 内置 API 测试工具

项目包含完整的 API 测试模块，位于应用设置中。

### 功能特性

1. **API 方法选择器**: 按模块分类浏览所有可用 API
2. **参数输入面板**: 动态生成参数输入字段，支持下拉选项
3. **实时测试执行**: 点击执行按钮立即调用 API
4. **双标签结果展示**:
   - JSON 标签页：显示原始 JSON 响应
   - 摘要标签页：格式化的可读摘要
5. **错误处理**: 完整的错误堆栈跟踪和错误信息

### 测试步骤

1. 在应用中进入 **设置** → **API 测试工具**
2. 选择要测试的 **API 模块**（如 IllustApi、NovelSeriesApi）
3. 选择具体的 **API 方法**
4. 填写必需参数，可选参数可留空使用默认值
5. 点击 **执行测试** 按钮
6. 查看测试结果：
   - **摘要** 标签页：格式化的测试结果摘要
   - **JSON** 标签页：完整的原始 JSON 响应

### 测试覆盖模块

- ✅ IllustApi - 6 个方法
- ✅ UserApi - 9 个方法  
- ✅ BookmarkApi - 8 个方法
- ✅ RankingApi - 2 个方法
- ✅ CommentApi - 8 个方法
- ✅ NovelApi - 5 个方法
- ✅ NovelSeriesApi - 5 个方法（含 watch/unwatch）
- ✅ TagApi - 4 个方法（含搜索建议）
- ✅ MarkerApi - 3 个方法（阅读标记功能）

---

## ⚠️ 常见问题处理

### 1. 401 Unauthorized

**原因**: PHPSESSID 过期或无效  
**解决**: 
1. 在浏览器重新获取 PHPSESSID
2. 检查格式是否正确（`userid_xxxxx`）

### 2. 403 Forbidden

**原因**: 
- 缺少 Referer 头
- 访问频率过高

**解决**:
2. 添加请求间隔（建议 1 秒）

### 3. 数据类型不匹配

**原因**: DTO 定义与实际 API 返回不符  
**解决**:
1. 打印原始 JSON 对比
2. 修改 DTO 定义
3. 更新 Mapper

### 4. 图片加载失败

**原因**: 图片 URL 需要特殊处理  
**解决**:
1. 使用 Coil 的 `setHeader("Referer", "https://www.pixiv.net/")`
2. 检查 URL 是否需要转换（如 pximg.net → i.pximg.net）

---

## 📊 测试进度追踪

| 模块 | 总方法数 | 已测试 | 通过 | 失败 | 进度 |
|------|---------|--------|------|------|------|
| IllustApi | 6 | 6 | 6 | 0 | 100% |
| UserApi | 7 | 7 | 7 | 0 | 100% |
| BookmarkApi | 8 | 8 | 8 | 0 | 100% |
| RankingApi | 2 | 2 | 2 | 0 | 100% |
| **总计** | **23** | **23** | **23** | **0** | **100%** |

---

## 📄 相关文档

- [Pixiv API 集成指南](../pixiv/PIXIV_API_集成指南.md)
- [API 状态文档](../shared/API_STATUS.md)
- [项目架构文档](../project/项目架构参考文档.md)
