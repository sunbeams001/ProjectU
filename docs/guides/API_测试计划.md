# Pixiv API 调试测试计划

> 📅 创建日期: 2025-10-30  
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

#### 测试用例示例

```kotlin
// 1. 获取作品详情
suspend fun testGetIllustDetail(pixivApi: PixivApi) {
    val illustId = "102814610"  // 测试用作品ID
    val response = pixivApi.illustApi.getIllustDetail(illustId)
    
    println("=== 作品详情测试 ===")
    println("ID: ${response.body.id}")
    println("标题: ${response.body.title}")
    println("作者: ${response.body.userId}")
    println("浏览量: ${response.body.viewCount}")
    println("收藏数: ${response.body.bookmarkCount}")
    println("图片URL: ${response.body.urls.regular}")
    
    // 验证关键字段
    assert(response.error == false)
    assert(response.body.id == illustId)
    assert(response.body.title.isNotBlank())
}

// 2. 搜索作品
suspend fun testSearchIllust(pixivApi: PixivApi) {
    val keyword = "初音ミク"
    val response = pixivApi.illustApi.searchIllust(
        keyword = keyword,
        mode = "safe",  // safe, r18
        order = "date_d", // date_d(日期降序), date_asc, popular_d
        sMode = "s_tag",  // s_tag(标签), s_tc(标题+标注)
        type = "all"      // all, illust, manga, ugoira
    )
    
    println("=== 搜索测试 ===")
    println("关键词: $keyword")
    println("总数: ${response.body.total}")
    println("结果数: ${response.body.illusts.size}")
    
    response.body.illusts.take(3).forEach { illust ->
        println("- [${illust.id}] ${illust.title} by ${illust.userName}")
    }
    
    // 验证
    assert(response.error == false)
    assert(response.body.illusts.isNotEmpty())
}
```

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

#### 测试用例示例

```kotlin
suspend fun testGetUserInfo(pixivApi: PixivApi) {
    val userId = "123456"  // 替换为真实用户ID
    val response = pixivApi.userApi.getUserInfo(userId)
    
    println("=== 用户信息测试 ===")
    println("ID: ${response.body.userId}")
    println("用户名: ${response.body.name}")
    println("头像: ${response.body.image}")
    
    // 验证
    assert(response.error == false)
    assert(response.body.userId == userId)
}
```

---

### BookmarkApi - 收藏相关 API

> 📁 文件: `shared/data/remote/api/BookmarkApi.kt`  
> ✅ 状态: 已集成，已测试

#### 插画收藏 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `addIllust` | `POST /bookmark_add.php` | P0 | ✅ | 添加插画收藏，返回 BookmarkAddResponse |
| `deleteIllust` | `POST /ajax/illusts/bookmarks/delete` | P0 | ✅ | 删除单个插画收藏，返回空数组 |
| `deleteIllusts` | `POST /ajax/illusts/bookmarks/delete` | P1 | ✅ | 批量删除插画收藏 |
| `getIllustBookmarkTags` | `GET /ajax/user/{userId}/illusts/bookmark/tags` | P2 | ✅ | 获取插画收藏标签 |

#### 小说收藏 API

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `addNovel` | `POST /novel/bookmark_add.php` | P0 | ✅ | 添加小说收藏，返回 bookmark_id 字符串 |
| `deleteNovel` | `POST /ajax/novels/bookmarks/delete` | P0 | ✅ | 删除单个小说收藏，返回空数组 |
| `deleteNovels` | `POST /ajax/novels/bookmarks/delete` | P1 | ✅ | 批量删除小说收藏 |
| `getNovelBookmarkTags` | `GET /ajax/user/{userId}/novels/bookmark/tags` | P2 | ✅ | 获取小说收藏标签 |

⚠️ **注意**: 
- 收藏操作会修改实际数据，测试时请谨慎！
- 插画和小说的添加接口返回类型不同：插画返回对象，小说返回字符串 bookmark_id
- 删除接口统一返回空数组 `[]`，需使用 `EmptyArrayResponse` 或 `List<String>` 处理

---

### RankingApi - 排行榜相关 API

> 📁 文件: `shared/data/remote/api/RankingApi.kt`  
> ✅ 状态: 已集成，待测试

| API 方法 | 端点 | 优先级 | 测试状态 | 备注 |
|---------|------|--------|---------|------|
| `getRanking` | `/ranking.php` | P0 | ⏳ | 综合排行榜 |
| `getDailyRanking` | `/ranking.php?mode=daily` | P0 | ⏳ | 日榜 |
| `getWeeklyRanking` | `/ranking.php?mode=weekly` | P1 | ⏳ | 周榜 |
| `getMonthlyRanking` | `/ranking.php?mode=monthly` | P1 | ⏳ | 月榜 |
| `getRookieRanking` | `/ranking.php?mode=rookie` | P2 | ⏳ | 新人榜 |
| `getOriginalRanking` | `/ranking.php?mode=original` | P2 | ⏳ | 原创榜 |
| `getMaleRanking` | `/ranking.php?mode=male` | P2 | ⏳ | 男性向 |
| `getFemaleRanking` | `/ranking.php?mode=female` | P2 | ⏳ | 女性向 |

---

## 🛠️ 测试工具实现方案

### 方案 1: 调试测试页面 (推荐)

创建一个专门的测试页面 `ApiTestScreen`，包含：

1. **API 选择器**: 选择要测试的 API 模块和方法
2. **参数输入**: 动态输入测试参数
3. **执行按钮**: 调用 API
4. **结果展示**: 显示原始 JSON 和格式化结果
5. **日志面板**: 显示请求/响应日志

```kotlin
// composeApp/ui/screens/debug/ApiTestScreen.kt
class ApiTestScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ApiTestViewModel = koinInject()
        val state by viewModel.state.collectAsState()
        
        Column {
            // API 选择
            ApiSelector(
                selectedModule = state.selectedModule,
                selectedMethod = state.selectedMethod,
                onSelect = { module, method -> 
                    viewModel.selectApi(module, method) 
                }
            )
            
            // 参数输入
            ParameterInputs(
                parameters = state.parameters,
                onParameterChange = { key, value ->
                    viewModel.updateParameter(key, value)
                }
            )
            
            // 执行按钮
            Button(
                onClick = { viewModel.executeTest() },
                enabled = !state.isLoading
            ) {
                Text("执行测试")
            }
            
            // 结果展示
            TestResultView(
                result = state.result,
                error = state.error
            )
        }
    }
}
```

### 方案 2: 单元测试套件

在 `shared/src/commonTest` 中创建测试类：

```kotlin
// shared/src/commonTest/kotlin/com/projectu/shared/api/PixivApiTest.kt
class PixivApiTest {
    private lateinit var pixivApi: PixivApi
    
    @BeforeTest
    fun setup() {
        val phpSessionId = System.getenv("PIXIV_PHPSESSID") 
            ?: error("请设置 PIXIV_PHPSESSID 环境变量")
        
        pixivApi = PixivApi.create(
            httpClient = NetworkClient.create(CIO.create()),
            phpSessionId = phpSessionId
        )
    }
    
    @Test
    fun testGetIllustDetail() = runBlocking {
        val response = pixivApi.illustApi.getIllustDetail("102814610")
        assert(response.error == false)
        assert(response.body.id == "102814610")
    }
    
    // ... 更多测试
}
```

### 方案 3: 命令行测试脚本

创建 Kotlin 脚本用于快速测试：

```kotlin
// scripts/test-pixiv-api.main.kts
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.util.NetworkClient
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking

val phpSessionId = System.getenv("PIXIV_PHPSESSID") 
    ?: error("请设置 PIXIV_PHPSESSID 环境变量")

val pixivApi = PixivApi.create(
    httpClient = NetworkClient.create(CIO.create()),
    phpSessionId = phpSessionId
)

runBlocking {
    // 测试作品详情
    println("=== 测试作品详情 ===")
    val detail = pixivApi.illustApi.getIllustDetail("102814610")
    println("标题: ${detail.body.title}")
    
    // 测试搜索
    println("\n=== 测试搜索 ===")
    val search = pixivApi.illustApi.searchIllust("初音ミク")
    println("结果数: ${search.body.illusts.size}")
}
```

---

## 📝 测试记录模板

为每个 API 创建测试记录：

```markdown
### getIllustDetail 测试记录

**测试时间**: 2025-10-30  
**测试环境**: Android / Desktop  
**测试参数**: 
- illustId: `102814610`

**预期行为**:
- 返回作品详细信息
- 包含标题、作者、图片URL等

**实际结果**:
- ✅ 接口调用成功
- ✅ 返回数据格式正确
- ⚠️ 发现问题: `xxxField` 字段类型与定义不符

**需要修复**:
1. 修改 `IllustDetailDto.kt` 中 `xxxField` 的类型
2. 更新 Mapper 逻辑

---
```

---

## 🚀 测试执行步骤

### 阶段 1: P0 高优先级 API (必须)

1. **IllustApi**
   - ✅ `getIllustDetail` - 作品详情 (核心功能)
   - ✅ `searchIllust` - 搜索作品 (核心功能)

2. **UserApi**
   - ✅ `getUserInfo` - 用户信息
   - ✅ `getUserFullInfo` - 完整用户信息

3. **BookmarkApi**
   - ✅ `addBookmark` - 添加收藏
   - ✅ `deleteBookmark` - 删除收藏

4. **RankingApi**
   - ✅ `getDailyRanking` - 日榜

### 阶段 2: P1 中优先级 API

- IllustApi 其他方法
- UserApi 其他方法
- RankingApi 其他排行榜

### 阶段 3: P2 低优先级 API

- Ugoira 相关
- 其他辅助功能

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
1. 确保所有请求都包含 `Referer: https://www.pixiv.net/`
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
| RankingApi | 7 | 0 | 0 | 0 | 0% |
| **总计** | **28** | **21** | **21** | **0** | **75%** |

---

## 📄 相关文档

- [Pixiv API 集成指南](../pixiv/PIXIV_API_集成指南.md)
- [API 状态文档](../shared/API_STATUS.md)
- [项目架构文档](../project/项目架构参考文档.md)
