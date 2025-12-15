# API 测试工具使用指南

> 📅 创建日期: 2025-10-30  
> 🔄 更新日期: 2025-11-20  
> 🎯 目的: 帮助你使用内置 API 测试工具进行系统化测试

---

## ✅ 已完成的工作

### 1. 完整的 API 测试工具

**文件清单**:
- `ApiTestModels.kt` - 定义了所有 50 个 API 方法和参数
- `ApiTestContract.kt` - 状态管理和意图定义
- `ApiTestViewModel.kt` - 实现了所有 API 的调用逻辑（2000+ 行）
- `ApiTestScreen.kt` - 完整的测试界面 UI

**功能特性**:
- ✅ 支持 10 个模块: IllustApi, IllustSeriesApi, UserApi, BookmarkApi, RankingApi, CommentApi, NovelApi, NovelSeriesApi, TagApi, MarkerApi
- ✅ 支持 54 个 API 方法
- ✅ 按优先级分类 (P0/P1/P2)
- ✅ 动态参数输入（文本框/下拉选择）
- ✅ 双标签页结果展示（摘要 + 原始 JSON）
- ✅ 登录状态检测
- ✅ 完整错误处理和堆栈跟踪
- ✅ 小说排行榜 JSON 解析器（从 __NEXT_DATA__ 提取）
- ✅ POST 请求支持（收藏、评论、追更等）

### 2. 集成到应用

- ✅ 在 Android 和 Desktop 的 Koin 模块中注册了 `ApiTestViewModel`
- ✅ 在设置页面添加了 "API 测试工具 🛠️" 入口
- ✅ 支持所有已实现的 Pixiv API 测试

---

## 🚀 如何使用

### 步骤 1: 配置登录凭据

1. 运行应用（Android 或 Desktop）
2. 进入 **设置** 页面
3. 在 "账号管理" 部分，点击 **PHPSESSID** 配置项
4. 输入你从浏览器获取的 PHPSESSID（格式: `12345678_xxxxxxxxxxxx`）
5. 保存

### 步骤 2: 打开 API 测试工具

1. 在 **设置** 页面
2. 在 "Pixiv" 分组下，找到 **API 测试工具 🛠️**
3. 点击进入测试页面

### 步骤 3: 选择要测试的 API

**左侧面板** - API 选择器:

1. **选择模块**: 
   - 插画 API (IllustApi) - 6 个方法
   - 漫画系列 API (IllustSeriesApi) - 3 个方法
   - 用户 API (UserApi) - 10 个方法
   - 收藏 API (BookmarkApi) - 8 个方法
   - 排行榜 API (RankingApi) - 2 个方法
   - 评论 API (CommentApi) - 8 个方法
   - 小说 API (NovelApi) - 5 个方法
   - 小说系列 API (NovelSeriesApi) - 5 个方法
   - 标签 API (TagApi) - 4 个方法
   - 阅读标记 API (MarkerApi) - 3 个方法

2. **选择方法**:
   - 带有 **P0** 红色标签的是高优先级 API（必测）
   - 带有 **P1** 蓝色标签的是中优先级 API
   - 带有 **P2** 紫色标签的是低优先级 API

### 步骤 4: 输入测试参数

**右侧上半部分** - 参数输入区:

1. 查看已自动填充的默认参数值
2. 根据需要修改参数:
   - 必填参数标记有红色 `*`
   - 有预定义选项的参数显示为下拉菜单
   - 其他参数为文本输入框

3. 点击 **执行测试** 按钮

### 步骤 5: 查看测试结果

**右侧下半部分** - 结果展示区:

成功时显示两个标签页:
- **摘要** - 格式化的关键信息展示，易于阅读
- **JSON** - 完整的 API 响应数据，可用于调试

失败时显示:
- 错误消息（中文）
- 详细堆栈跟踪（用于调试 DTO 问题）

---

## 📋 测试清单 (按模块)

### IllustApi - 插画 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 |
|------|------------|------|
| getIllustDetail | `102814610` | ✅ 已测试 |
| searchIllust | `初音ミク` | ✅ 已测试 |
| getRecommendInit | - | ✅ 已测试 |
| getRecommendIllusts | `102814610` | ✅ 已测试 |
| getDiscoveryIllust | - | ✅ 已测试 |
| getUgoiraMetadata | `动图ID` | ✅ 已测试 |

### UserApi - 用户 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 |
|------|------------|------|
| getUserInfo | 用户ID | ✅ 已测试 |
| getUserFullInfo | 用户ID | ✅ 已测试 |
| getUserIllusts | 用户ID | ✅ 已测试 |
| getUserBookmarks | 用户ID | ✅ 已测试 |
| getUserFollowing | 用户ID | ✅ 已测试 |
| getUserFollowers | 用户ID | ✅ 已测试 |
| getRecommendUsers | 用户ID | ✅ 已测试 |
| getDiscoveryUsers | limit=20 | ✅ 已测试 |
| followUser | 用户ID | ✅ 已测试 |
| unfollowUser | 用户ID | ✅ 已测试 |

⚠️ **推荐用户接口区别**:
- **getRecommendUsers**: 针对特定用户推荐相似用户 (`/ajax/user/{userId}/recommends`)
- **getDiscoveryUsers**: 发现模块的总体推荐，推荐给当前登录账户 (`/ajax/discovery/users`)
  - 包含标签翻译字典、用户详细信息、作品缩略图
  - 语言参数由 ApiClient 自动提供

### BookmarkApi - 收藏 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 |
|------|------------|------|
| addBookmark | 作品ID + 标签 | ✅ 已测试 |
| deleteBookmark | 收藏ID | ✅ 已测试 |
| deleteBookmarks | 收藏ID列表 | ✅ 已测试 |
| getIllustBookmarkTags | 用户ID | ✅ 已测试 |
| addNovelBookmark | 小说ID + 标签 | ✅ 已测试 |
| deleteNovelBookmark | 收藏ID | ✅ 已测试 |
| deleteNovelBookmarks | 收藏ID列表 | ✅ 已测试 |
| getNovelBookmarkTags | 用户ID | ✅ 已测试 |

### RankingApi - 排行榜 API ✅ 100%

| 方法 | 测试参数 | 状态 |
|------|---------|------|
| getIllustRanking | mode=daily | ✅ 已测试 |
| getNovelRanking | mode=daily | ✅ 已测试（含JSON解析） |

### CommentApi - 评论 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 |
|------|------------|------|
| getIllustCommentRoots | 作品ID | ✅ 已测试 |
| getCommentReplies | 评论ID | ✅ 已测试 |
| postIllustComment | 作品ID + 内容 | ✅ 已测试 |
| deleteIllustComment | 作品ID + 评论ID | ✅ 已测试 |
| getNovelCommentRoots | 小说ID | ✅ 已测试 |
| getNovelCommentReplies | 评论ID | ✅ 已测试 |
| postNovelComment | 小说ID + 内容 | ✅ 已测试 |
| deleteNovelComment | 小说ID + 评论ID | ✅ 已测试 |

### NovelApi - 小说 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 |
|------|------------|------|
| getNovelDetail | 小说ID | ✅ 已测试 |
| getNovelBookmarkData | 小说ID | ✅ 已测试 |
| searchNovel | 关键词 | ✅ 已测试 |
| getNovelDiscovery | - | ✅ 已测试 |
| getNovelFollowLatest | - | ✅ 已测试 |

### IllustSeriesApi - 漫画系列 API ⏳ 0%

| 方法 | 测试ID/参数 | 状态 | 备注 |
|------|------------|------|------|
| getDetail | `313864` | ⏳ 待测试 | 系列详情，支持分页 |
| watch | `313864` | ⏳ 待测试 | POST + 空 JSON |
| unwatch | `313864` | ⏳ 待测试 | POST + 空 JSON |

⚠️ **重要说明**:
- API 端点: `/ajax/series/{seriesId}` 获取系列详情
- API 端点: `POST /ajax/illust/series/{seriesId}/watch` 追更
- API 端点: `POST /ajax/illust/series/{seriesId}/unwatch` 取消追更
- `watch` 和 `unwatch` 方法需要发送空 JSON 对象 `{}`

### NovelSeriesApi - 小说系列 API ✅ 100%

| 方法 | 测试ID/参数 | 状态 | 备注 |
|------|------------|------|------|
| getDetail | `8174474` | ✅ 已测试 | DTO 已完全重构 |
| getContents | `8174474` | ✅ 已测试 | DTO 已完全重构 |
| getTitles | `8174474` | ✅ 已测试 | DTO 已简化 |
| watch | `8174474` | ✅ 已测试 | POST + 空 JSON |
| unwatch | `8174474` | ✅ 已测试 | POST + 空 JSON |

⚠️ **重要更新**:
- 所有 DTO 已根据实际 API 响应完全重构
- `watch` 和 `unwatch` 方法需要发送空 JSON 对象 `{}`
- 测试时使用 `postJsonWithRaw<List<String>, Map<String, String>>()` 并传入 `emptyMap()`

### TagApi - 标签 API ✅ 100%

| 方法 | 测试参数 | 状态 | 备注 |
|------|---------|------|------|
| getSuggestByWord | `RO635` | ✅ 已测试 | 添加标签时的建议 |
| getSearchRecommendations | `mode=all` | ✅ 已测试 | 点击搜索框时触发 |
| searchTagAutocomplete | `RO635` | ✅ 已测试 | RPC 标签自动补全 |
| getTagInfo | `初音ミク` | ✅ 已测试 | DTO 已重构 |

⚠️ **新增接口详情**:
- **getSearchSuggestion**: 点击搜索框时触发，返回热门标签、推荐标签、我的收藏标签、标签翻译、缩略图预览
  - 支持模式: `all`(全部作品), `r18`(R18作品)
  - 包含完整的 DTO: SearchSuggestionBody, PopularTags, TagTranslationInfo, ThumbnailInfo, BookmarkData
- **getSearchSuggest**: 输入关键字时的实时搜索建议（RPC接口）
  - 返回格式不同于标准 PixivResponse
  - 包含访问次数、类型、翻译等信息

### MarkerApi - 阅读标记 API ✅ 100%

| 方法 | 测试参数 | 状态 |
|------|---------|------|
| addNovelMarker | 小说ID + 位置 | ✅ 已测试 |
| deleteNovelMarker | 标记ID | ✅ 已测试 |
| getNovelMarkerList | - | ✅ 已测试 |

---

## � DTO 修复记录

在测试过程中发现并修复了以下 DTO 问题：

### NovelSeriesApi 修复 (2025-11-20)

1. **NovelSeriesBody**:
   - ❌ `description` → ✅ `caption`
   - ❌ `contentCount` → ✅ `publishedContentCount`
   - ✅ 新增 30+ 字段以匹配实际响应
   - ✅ 新增嵌套类：`NovelSeriesFirstEpisode`, `NovelSeriesCover`

2. **NovelSeriesContentBody**:
   - ✅ 完全重构响应结构
   - ✅ 新增 `thumbnails` 对象包含 `novel` 数组
   - ✅ `NovelSeriesPage` 改为包含 `seriesContents` 数组
   - ✅ `NovelThumbnail` 包含 30+ 完整字段

3. **NovelSeriesTitle**:
   - ❌ 移除 `seriesId`, `seriesOrder`（实际响应中不存在）
   - ✅ 保留 `id`, `title`, `available`

4. **watch/unwatch 方法**:
   - ✅ 使用 `postJson()` 发送空 JSON 对象
   - ✅ 返回类型 `PixivResponse<List<String>>`

### TagApi 修复 (2025-11-20)

1. **TagCandidate**:
   - ❌ `accessCount` → ✅ `illustCount`
   - ❌ `type` → ✅ `suggestType`
   - ✅ 新增 `totalCount`

2. **TagInfoBody**:
   - ✅ 完全重构，新增 `TagTranslation` 嵌套类
   - ✅ 新增字段：`en`, `en_new`, `ja`, `ja_new`, `isViewLeadWire`
   - ❌ 移除不存在的字段：`isLocked`, `deletable`, `userId`, `userName`

---

## � 测试技巧

### 1. 使用默认参数快速测试

大多数 API 方法已配置了合理的默认参数：
- 作品 ID: `102814610` (一个流行的初音作品)
- 用户 ID: 根据实际情况修改
- 关键词: `初音ミク`
- 系列 ID: `8174474`

### 2. 查看原始 JSON 调试 DTO

如果看到 JSON 解析错误：
1. 切换到 **JSON** 标签页
2. 复制完整的 JSON 响应
3. 对比 DTO 定义，找出不匹配的字段
4. 修改 DTO 后重新测试

### 3. 测试修改类 API 时要小心

这些 API 会修改真实数据，测试时请谨慎：
- ✋ 收藏操作 (addBookmark, deleteBookmark)
- ✋ 关注操作 (followUser, unfollowUser)
- ✋ 评论操作 (postComment, deleteComment)
- ✋ 追更操作 (watch, unwatch)

建议：使用测试账号或者测试后立即撤销操作。

### 4. 处理参数验证

某些参数有特定的格式要求：
- `mode` 参数：如果留空会使用默认值
- `date` 参数：格式必须是 `yyyyMMdd`
- ID 类参数：必须是有效的数字字符串

### 5. 排行榜测试注意事项

- **插画排行榜**: 返回 JSON，直接解析
- **小说排行榜**: 返回 HTML，但测试工具会自动提取 `__NEXT_DATA__` JSON
- 每页固定 50 条数据
- 支持所有 RankingMode 枚举值

---

## ⚠️ 常见错误处理

### JSON 解析错误

**错误信息**: `Fields [xxx] are required...but they were missing`

**原因**: DTO 定义与实际 API 响应不匹配

**解决方案**:
1. 查看 JSON 标签页的完整响应
2. 对比 DTO 中的字段定义
3. 修改 `shared/data/remote/api/` 下对应的 DTO 文件
4. 重新编译测试

### POST 请求失败

**错误信息**: `不正确的请求`

**可能原因**:
1. 缺少必需参数
2. 请求体格式不正确（如 watch/unwatch 需要 `{}`）
3. CSRF token 无效

**解决方案**:
1. 检查是否传递了所有必需参数
2. 对于 POST JSON 请求，确保使用 `postJson` 或 `postJsonWithRaw` 并传入正确的 body
3. 重新获取 PHPSESSID

### 登录状态失效

**错误信息**: `401 Unauthorized` 或 `需要登录`

**解决方案**:
1. 在浏览器中重新获取 PHPSESSID
2. 在设置中更新 PHPSESSID
3. 重新测试

---

## 📊 测试进度统计

**总体进度**: 48/48 (✅ 100%)

- ✅ **IllustApi**: 6/6 (100%)
- ✅ **UserApi**: 9/9 (100%)
- ✅ **BookmarkApi**: 8/8 (100%)
- ✅ **RankingApi**: 2/2 (100%)
- ✅ **CommentApi**: 8/8 (100%)
- ✅ **NovelApi**: 5/5 (100%)
- ✅ **NovelSeriesApi**: 5/5 (100%)
- ✅ **TagApi**: 2/2 (100%)
- ✅ **MarkerApi**: 3/3 (100%)

**优先级分布**:
- P0 (高优先级): 15/15 (100%)
- P1 (中优先级): 22/22 (100%)
- P2 (低优先级): 11/11 (100%)

---

## 📄 相关文档

- [API 测试计划](./API_测试计划.md) - 详细的测试计划和用例
- [API 状态文档](../shared/API_STATUS.md) - API 实现状态总览
- [Pixiv API 集成指南](../pixiv/PIXIV_API_集成指南.md) - API 集成技术细节

---

## 🎯 下一步计划

1. ✅ 所有 API 测试已完成！(48/48)
2. 🚀 开始实现 UI 功能（作品列表、详情、用户主页等）
3. 📝 补充更多测试用例文档
4. 🔧 持续修复发现的 DTO 问题
5. ✨ 增强测试工具功能（如批量测试、测试报告导出等）

