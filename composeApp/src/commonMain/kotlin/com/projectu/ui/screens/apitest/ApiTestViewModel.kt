package com.projectu.ui.screens.apitest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.remote.api.BookmarkAddResponse
import com.projectu.shared.data.remote.api.BookmarkRequest
import com.projectu.shared.data.remote.api.BookmarkTagsResponse
import com.projectu.shared.data.remote.api.NovelBookmarkRequest
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingMode
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * API 测试 ViewModel
 */
class ApiTestViewModel(
    private val pixivApi: PixivApi,
    private val pixivConfigStore: PixivConfigStore
) : ViewModel() {
    
    private val _state = MutableStateFlow(ApiTestState())
    val state: StateFlow<ApiTestState> = _state.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            val config = pixivConfigStore.getCurrentConfig()
            _state.update { 
                it.copy(isLoginValid = config.phpSessionId.isNotBlank())
            }
        }
    }
    
    /**
     * 处理用户意图
     */
    fun handleIntent(intent: ApiTestIntent) {
        when (intent) {
            is ApiTestIntent.SelectModule -> selectModule(intent.module)
            is ApiTestIntent.SelectMethod -> selectMethod(intent.method)
            is ApiTestIntent.UpdateParameter -> updateParameter(intent.name, intent.value)
            is ApiTestIntent.ExecuteTest -> executeTest()
            is ApiTestIntent.ClearResult -> clearResult()
            is ApiTestIntent.LoadHistoryItem -> loadHistoryItem(intent.item)
        }
    }
    
    /**
     * 选择 API 模块
     */
    private fun selectModule(module: ApiModule) {
        _state.update { 
            it.copy(
                selectedModule = module,
                selectedMethod = null,
                parameterValues = emptyMap(),
                testResult = TestResult.Idle
            )
        }
    }
    
    /**
     * 选择 API 方法
     */
    private fun selectMethod(method: ApiMethod) {
        // 使用默认参数值初始化
        val defaultParams = method.parameters.associate { 
            it.name to it.defaultValue 
        }
        
        _state.update { 
            it.copy(
                selectedMethod = method,
                parameterValues = defaultParams,
                testResult = TestResult.Idle
            )
        }
    }
    
    /**
     * 更新参数值
     */
    private fun updateParameter(name: String, value: String) {
        _state.update { 
            it.copy(
                parameterValues = it.parameterValues + (name to value)
            )
        }
    }
    
    /**
     * 清除测试结果
     */
    private fun clearResult() {
        _state.update { 
            it.copy(testResult = TestResult.Idle)
        }
    }
    
    /**
     * 加载历史记录项
     */
    private fun loadHistoryItem(item: TestHistoryItem) {
        _state.update {
            it.copy(
                selectedModule = item.method.module,
                selectedMethod = item.method,
                parameterValues = item.parameters,
                testResult = item.result
            )
        }
    }
    
    /**
     * 执行 API 测试
     */
    private fun executeTest() {
        val currentState = _state.value
        val method = currentState.selectedMethod ?: return
        
        if (!currentState.isLoginValid) {
            _state.update {
                it.copy(
                    testResult = TestResult.Error("请先配置登录凭据 (PHPSESSID)")
                )
            }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(testResult = TestResult.Loading) }
            
            try {
                val duration = measureTimeMillis {
                    when (method) {
                        // ==================== IllustApi ====================
                        ApiMethod.GetIllustDetail -> testGetIllustDetail()
                        ApiMethod.SearchIllust -> testSearchIllust()
                        ApiMethod.GetRecommendInit -> testGetRecommendInit()
                        ApiMethod.GetRecommendIllusts -> testGetRecommendIllusts()
                        ApiMethod.GetDiscoveryIllust -> testGetDiscoveryIllust()
                        ApiMethod.GetUgoiraMetadata -> testGetUgoiraMetadata()
                        
                        // ==================== UserApi ====================
                        ApiMethod.GetUserInfo -> testGetUserInfo()
                        ApiMethod.GetUserFullInfo -> testGetUserFullInfo()
                        ApiMethod.GetUserIllusts -> testGetUserIllusts()
                        ApiMethod.GetUserBookmarks -> testGetUserBookmarks()
                        ApiMethod.GetUserFollowing -> testGetUserFollowing()
                        ApiMethod.GetUserFollowers -> testGetUserFollowers()
                        
                        // ==================== BookmarkApi ====================
                        ApiMethod.AddBookmark -> testAddBookmark()
                        ApiMethod.DeleteBookmark -> testDeleteBookmark()
                        ApiMethod.DeleteBookmarks -> testDeleteBookmarks()
                        ApiMethod.GetIllustBookmarkTags -> testGetIllustBookmarkTags()
                        ApiMethod.AddNovelBookmark -> testAddNovelBookmark()
                        ApiMethod.DeleteNovelBookmark -> testDeleteNovelBookmark()
                        ApiMethod.DeleteNovelBookmarks -> testDeleteNovelBookmarks()
                        ApiMethod.GetNovelBookmarkTags -> testGetNovelBookmarkTags()
                        
                        // ==================== RankingApi ====================
                        ApiMethod.GetIllustRanking -> testGetIllustRanking()
                        ApiMethod.GetNovelRanking -> testGetNovelRanking()
                    }
                }
                
                // 测试成功后，添加到历史记录
                _state.update { state ->
                    val historyItem = TestHistoryItem(
                        method = method,
                        parameters = state.parameterValues,
                        result = state.testResult
                    )
                    state.copy(
                        testHistory = (listOf(historyItem) + state.testHistory).take(20) // 保留最近20条
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        testResult = TestResult.Error(
                            message = e.message ?: "未知错误",
                            stackTrace = e.stackTraceToString()
                        )
                    )
                }
            }
        }
    }
    
    // ==================== IllustApi 测试方法 ====================
    
    private suspend fun testGetIllustDetail() {
        val illustId = getParam("illustId").toLongOrNull() ?: 102814610L
        // 使用 getWithRaw 获取原始 JSON
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.IllustDetailBody>(
            "/ajax/illust/$illustId"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 作品详情获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testSearchIllust() {
        val keyword = getParam("keyword")
        val mode = getParam("mode")
        val order = getParam("order")
        val searchMode = getParam("sMode")
        val page = getParam("page").toIntOrNull() ?: 1
        
        // URL编码关键词
        val encodedKeyword = keyword.encodeURLPath()
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.IllustSearchBody>(
            "/ajax/search/artworks/$encodedKeyword",
            mapOf(
                "word" to keyword,
                "s_mode" to searchMode,
                "order" to order,
                "mode" to mode,
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 搜索成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("关键词: $keyword")
            appendLine("模式: $mode | 排序: $order | 搜索模式: $searchMode")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetRecommendInit() {
        val pid = getParam("pid").toLongOrNull() ?: 102814610L
        val limit = getParam("limit").toIntOrNull() ?: 18
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.IllustRecommendInitBody>(
            "/ajax/illust/$pid/recommend/init",
            mapOf("limit" to limit)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 推荐初始化成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("基准作品ID: $pid")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("返回作品数: ${body.illusts.size}")
                appendLine("NextIds数量: ${body.nextIds.size}")
                appendLine("NextIds: ${body.nextIds.take(5).joinToString(", ")}${if (body.nextIds.size > 5) "..." else ""}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
            appendLine("提示: 使用 nextIds 调用 getRecommendIllusts 获取后续推荐")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetRecommendIllusts() {
        val illustIdsStr = getParam("illustIds")
        val illustIds = illustIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        
        if (illustIds.isEmpty()) {
            _state.update { current ->
                current.copy(
                    testResult = TestResult.Error("请提供有效的作品ID列表 (逗号分隔)")
                )
            }
            return
        }
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.IllustRecommendBody>(
            "/ajax/illust/recommend/illusts",
            mapOf("illust_ids[]" to illustIds)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 获取推荐作品成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("输入ID数量: ${illustIds.size}")
            appendLine("输入IDs: ${illustIds.take(3).joinToString(", ")}${if (illustIds.size > 3) "..." else ""}")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("返回作品数: ${body.illusts.size}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetDiscoveryIllust() {
        val mode = getParam("mode")
        val limit = getParam("limit").toIntOrNull() ?: 100
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.DiscoveryBody>(
            "/ajax/discovery/artworks",
            mapOf(
                "mode" to mode,
                "limit" to limit
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 发现作品获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: $mode")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUgoiraMetadata() {
        val illustId = getParam("illustId").toLongOrNull() ?: 44298467L
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UgoiraMetaBody>(
            "/ajax/illust/$illustId/ugoira_meta"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 动图元数据获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== UserApi 测试方法 ====================
    
    private suspend fun testGetUserInfo() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UserInfoBody>(
            "/ajax/user/$userId",
            mapOf("full" to 1)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户信息获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserFullInfo() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.ProfileAllBody>(
            "/ajax/user/$userId/profile/all"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户作品概况获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserIllusts() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 首先获取作品ID列表
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.ProfileAllBody>(
            "/ajax/user/$userId/profile/all"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户作品概况获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserBookmarks() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val tag = getParam("tag")
        val rest = getParam("rest").ifBlank { "show" }
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 48
        
        // 使用 UserApi 的方法
        val response = pixivApi.userApi.getUserBookmarkIllusts(
            uid = userId,
            tag = tag,
            offset = offset,
            limit = limit,
            rest = rest
        )
        
        // 获取带原始JSON的响应用于显示
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UserBookmarkBody>(
            "/ajax/user/$userId/illusts/bookmarks",
            mapOf(
                "tag" to tag,
                "offset" to offset,
                "limit" to limit,
                "rest" to rest
            )
        )
        
        val summary = buildString {
            appendLine("✅ 用户收藏获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("标签: ${tag.ifBlank { "全部" }}")
            appendLine("公开性: $rest")
            appendLine("偏移: $offset")
            appendLine("限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserFollowing() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val rest = getParam("rest").ifBlank { "show" }
        val tag = getParam("tag")
        val acceptingRequests = getParam("acceptingRequests").toIntOrNull() ?: 0
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 24
        
        // 使用真实的用户关注列表接口
        val response = pixivApi.userApi.getUserFollowing(
            uid = userId,
            offset = offset,
            limit = limit,
            rest = rest,
            tag = tag,
            acceptingRequests = acceptingRequests
        )
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UserFollowingBody>(
            "/ajax/user/$userId/following",
            mapOf(
                "offset" to offset,
                "limit" to limit,
                "rest" to rest,
                "tag" to tag,
                "acceptingRequests" to acceptingRequests
            )
        )
        
        val summary = buildString {
            appendLine("✅ 用户关注列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("公开状态: $rest")
            appendLine("标签过滤: ${if (tag.isBlank()) "无" else tag}")
            appendLine("仅接稿用户: ${if (acceptingRequests == 1) "是" else "否"}")
            appendLine("偏移量: $offset")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            val body = response.body
            if (body != null) {
                appendLine("关注用户总数: ${body.total}")
                appendLine("当前返回数量: ${body.users.size}")
            }
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserFollowers() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 24
        
        // 使用真实的用户粉丝列表接口
        val response = pixivApi.userApi.getUserFollowers(
            uid = userId,
            offset = offset,
            limit = limit
        )
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UserFollowingBody>(
            "/ajax/user/$userId/followers",
            mapOf(
                "offset" to offset,
                "limit" to limit
            )
        )
        
        val summary = buildString {
            appendLine("✅ 用户粉丝列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("偏移量: $offset")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            val body = response.body
            if (body != null) {
                appendLine("粉丝总数: ${body.total}")
                appendLine("当前返回数量: ${body.users.size}")
            }
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== BookmarkApi 测试方法 ====================
    
    private suspend fun testAddBookmark() {
        val illustId = getParam("illustId").toLongOrNull() ?: 102814610L
        val restrict = getParam("restrict").toIntOrNull() ?: 0
        val comment = getParam("comment")
        val tags = getParam("tags")
        
        val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        
        // 创建请求体（使用shared模块的BookmarkRequest）
        val requestBody = BookmarkRequest(
            illustId = illustId.toString(),
            restrict = restrict,
            comment = comment,
            tags = tagList
        )
        
        // 使用postJsonWithRaw获取原始响应和强类型数据
        val responseWithRaw = pixivApi.client.postJsonWithRaw<BookmarkAddResponse, BookmarkRequest>(
            "/ajax/illusts/bookmarks/add",
            requestBody
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 收藏操作已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("公开性: ${if (restrict == 0) "公开" else "私密"}")
            appendLine("评论: ${comment.ifBlank { "无" }}")
            appendLine("标签: ${tagList.joinToString().ifBlank { "无" }}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("收藏ID: ${body.lastBookmarkId ?: "未返回"}")
                appendLine("状态ID: ${body.staccStatusId ?: "未返回"}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testDeleteBookmark() {
        val bookmarkId = getParam("bookmarkId")
        
        // 删除接口返回空数组 []，使用 List<String> 来接收
        val responseWithRaw = pixivApi.client.postFormWithRaw<List<String>>(
            "/ajax/illusts/bookmarks/delete",
            mapOf("bookmark_id" to bookmarkId)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 取消收藏已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("收藏ID: $bookmarkId")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("响应体: ${if (body.isEmpty()) "空数组 []" else body.toString()}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetIllustBookmarkTags() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 使用插画收藏标签 API
        val responseWithRaw = pixivApi.client.getWithRaw<BookmarkTagsResponse>(
            "/ajax/user/$userId/illusts/bookmark/tags"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 插画收藏标签获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("公开标签数: ${body.public.size}")
                appendLine("私密标签数: ${body.private.size}")
                appendLine("收藏过多: ${body.tooManyBookmark}")
                appendLine("标签过多: ${body.tooManyBookmarkTags}")
                if (body.public.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("公开标签（前10个）:")
                    body.public.take(10).forEach { tag ->
                        appendLine("  • ${tag.tag} (${tag.cnt})")
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelBookmarkTags() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 使用小说收藏标签 API
        val responseWithRaw = pixivApi.client.getWithRaw<BookmarkTagsResponse>(
            "/ajax/user/$userId/novels/bookmark/tags"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说收藏标签获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("公开标签数: ${body.public.size}")
                appendLine("私密标签数: ${body.private.size}")
                appendLine("收藏过多: ${body.tooManyBookmark}")
                appendLine("标签过多: ${body.tooManyBookmarkTags}")
                if (body.public.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("公开标签（前10个）:")
                    body.public.take(10).forEach { tag ->
                        appendLine("  • ${tag.tag} (${tag.cnt})")
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testDeleteBookmarks() {
        val bookmarkIds = getParam("bookmarkIds")
        val idList = bookmarkIds.split(",").map { it.trim() }
        
        // 批量删除插画收藏，返回空数组
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, List<String>>>(
            "/ajax/illusts/bookmarks/remove",
            mapOf("bookmarkIds" to idList)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 批量取消收藏已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("收藏ID列表: ${idList.joinToString()}")
            appendLine("删除数量: ${idList.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("响应体: ${if (body.isEmpty()) "空数组 []" else body.toString()}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testAddNovelBookmark() {
        val novelId = getParam("novelId").toLongOrNull() ?: 0L
        val restrict = getParam("restrict").toIntOrNull() ?: 0
        val comment = getParam("comment")
        val tags = getParam("tags")
        val tagList = if (tags.isNotBlank()) tags.split(",").map { it.trim() } else emptyList()
        
        // 添加小说收藏，使用 NovelBookmarkRequest
        val requestBody = NovelBookmarkRequest(
            novelId = novelId.toString(),
            restrict = restrict,
            comment = comment,
            tags = tagList
        )
        
        // 小说收藏返回的是字符串 (收藏ID)
        val responseWithRaw = pixivApi.client.postJsonWithRaw<String, NovelBookmarkRequest>(
            "/ajax/novels/bookmarks/add",
            requestBody
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说收藏操作已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("公开性: ${if (restrict == 0) "公开" else "私密"}")
            appendLine("评论: ${comment.ifBlank { "无" }}")
            appendLine("标签: ${tagList.joinToString().ifBlank { "无" }}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("收藏ID: $body")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testDeleteNovelBookmark() {
        val bookId = getParam("bookId")
        
        // 删除小说收藏，返回空数组
        val responseWithRaw = pixivApi.client.postFormWithRaw<List<String>>(
            "/ajax/novels/bookmarks/delete",
            mapOf(
                "book_id" to bookId,
                "del" to "1"
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 取消小说收藏已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("收藏ID: $bookId")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("响应体: ${if (body.isEmpty()) "空数组 []" else body.toString()}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testDeleteNovelBookmarks() {
        val bookmarkIds = getParam("bookmarkIds")
        val idList = bookmarkIds.split(",").map { it.trim() }
        
        // 批量删除小说收藏，返回空数组
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, List<String>>>(
            "/ajax/novels/bookmarks/remove",
            mapOf("bookmarkIds" to idList)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 批量取消小说收藏已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("收藏ID列表: ${idList.joinToString()}")
            appendLine("删除数量: ${idList.size}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            val body = response.body
            if (body != null) {
                appendLine("响应体: ${if (body.isEmpty()) "空数组 []" else body.toString()}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== RankingApi 测试方法 ====================
    
    private suspend fun testGetIllustRanking() {
        val modeStr = getParam("mode")
        val page = getParam("page").toIntOrNull() ?: 1
        val contentStr = getParam("content").ifBlank { "all" }
        val date = getParam("date").ifBlank { null }
        
        // 转换为枚举
        val mode = RankingMode.fromValue(modeStr) ?: RankingMode.DAILY
        val content = RankingContent.fromValue(contentStr) ?: RankingContent.ALL
        
        // 构建请求参数（用于显示）
        val params = mutableMapOf<String, Any?>(
            "mode" to mode.value,
            "p" to page,
            "format" to "json",
            "content" to content.value
        )
        if (date != null) {
            params["date"] = date
        }
        
        // 调用 API
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.RankingResponse>(
            "/ranking.php",
            params
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ ${mode.displayName}排行榜 获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: ${mode.value} (${mode.displayName})")
            appendLine("分类: ${if (mode.category == com.projectu.shared.data.remote.model.RankingCategory.GENERAL) "一般" else "R-18"}")
            appendLine("页码: $page")
            appendLine("内容类型: ${content.value} (${content.displayName})")
            appendLine("日期: ${date ?: "最新"}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠️ 排行榜返回 HTML 格式数据")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelRanking() {
        val modeStr = getParam("mode")
        val page = getParam("page").toIntOrNull() ?: 1
        val contentStr = getParam("content").ifBlank { "all" }
        val date = getParam("date").ifBlank { null }
        
        // 转换为枚举
        val mode = RankingMode.fromValue(modeStr) ?: RankingMode.DAILY
        val content = RankingContent.fromValue(contentStr) ?: RankingContent.ALL
        
        // 调用 API（小说排行榜返回解析后的数据）
        val response = pixivApi.rankingApi.getNovelRanking(mode, page, content, date)
        
        val summary = buildString {
            appendLine("✅ ${mode.displayName}小说排行榜 获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: ${mode.value} (${mode.displayName})")
            appendLine("分类: ${if (mode.category == com.projectu.shared.data.remote.model.RankingCategory.GENERAL) "一般" else "R-18"}")
            appendLine("页码: $page / ${response.totalPages}")
            appendLine("排名范围: ${response.rankRange}")
            appendLine("日期: ${response.date}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说数量: ${response.novels.size}")
            appendLine("")
            
            // 显示前3条小说完整信息
            response.novels.take(3).forEachIndexed { index, novel ->
                appendLine("【${novel.rank}位】${novel.title}")
                appendLine("  ID: ${novel.novelId}")
                appendLine("  作者: ${novel.author.userName} (ID: ${novel.author.userId})")
                appendLine("  字数: ${novel.characterCount}")
                appendLine("  书签: ${novel.bookmarkCount}")
                val seriesInfo = novel.series
                if (seriesInfo != null) {
                    appendLine("  系列: ${seriesInfo.seriesTitle} (ID: ${seriesInfo.seriesId})")
                }
                appendLine("  标签: ${novel.tags.take(5).joinToString(", ")}")
                if (novel.isBookmarked) {
                    appendLine("  ⭐ 已收藏")
                    if (novel.marker != null) {
                        appendLine("  📖 阅读进度: ${novel.marker}")
                    }
                    if (novel.bookmarkRestrict == "1") {
                        appendLine("  🔒 私密收藏")
                    }
                }
                if (index < 2) appendLine("")
            }
            
            if (response.novels.size > 3) {
                appendLine("")
                appendLine("... 还有 ${response.novels.size - 3} 条小说")
            }
            
            appendLine("")
            appendLine("📊 从 __NEXT_DATA__ JSON 解析")
            appendLine("查看 JSON 标签页查看完整结果")
        }
        
        // 将解析后的数据转换为 JSON 字符串
        val jsonString = buildString {
            appendLine("{")
            appendLine("  \"mode\": \"${response.mode}\",")
            appendLine("  \"date\": \"${response.date}\",")
            appendLine("  \"currentPage\": ${response.currentPage},")
            appendLine("  \"totalPages\": ${response.totalPages},")
            appendLine("  \"rankRange\": \"${response.rankRange}\",")
            appendLine("  \"totalCount\": ${response.novels.size},")
            appendLine("  \"novels\": [")
            response.novels.forEachIndexed { index, novel ->
                appendLine("    {")
                appendLine("      \"rank\": ${novel.rank},")
                appendLine("      \"novelId\": \"${novel.novelId}\",")
                appendLine("      \"title\": \"${novel.title.replace("\"", "\\\"")}\",")
                appendLine("      \"novelUrl\": \"${novel.novelUrl}\",")
                appendLine("      \"author\": {")
                appendLine("        \"userId\": \"${novel.author.userId}\",")
                appendLine("        \"userName\": \"${novel.author.userName.replace("\"", "\\\"")}\",")
                appendLine("        \"profileImageUrl\": \"${novel.author.profileImageUrl}\",")
                appendLine("        \"novelListUrl\": \"${novel.author.novelListUrl}\"")
                appendLine("      },")
                appendLine("      \"coverImageUrl\": \"${novel.coverImageUrl}\",")
                appendLine("      \"characterCount\": ${novel.characterCount},")
                appendLine("      \"bookmarkCount\": ${novel.bookmarkCount},")
                appendLine("      \"tags\": [${novel.tags.joinToString(", ") { "\"${it.replace("\"", "\\\"")}\"" }}],")
                appendLine("      \"caption\": \"${novel.caption.take(100).replace("\"", "\\\"").replace("\n", "\\n")}...\",")
                val seriesInfo = novel.series
                if (seriesInfo != null) {
                    appendLine("      \"series\": {")
                    appendLine("        \"seriesId\": \"${seriesInfo.seriesId}\",")
                    appendLine("        \"seriesTitle\": \"${seriesInfo.seriesTitle.replace("\"", "\\\"")}\",")
                    appendLine("        \"seriesUrl\": \"${seriesInfo.seriesUrl}\"")
                    appendLine("      },")
                } else {
                    appendLine("      \"series\": null,")
                }
                appendLine("      \"isBookmarked\": ${novel.isBookmarked},")
                appendLine("      \"bookmarkId\": ${novel.bookmarkId?.let { "\"$it\"" } ?: "null"},")
                appendLine("      \"bookmarkRestrict\": ${novel.bookmarkRestrict?.let { "\"$it\"" } ?: "null"},")
                appendLine("      \"marker\": ${novel.marker ?: "null"}")
                appendLine("    }${if (index < response.novels.size - 1) "," else ""}")
            }
            appendLine("  ]")
            appendLine("}")
        }
        
        updateResultWithRaw(jsonString, summary)
    }
    
    // ==================== 辅助方法 ====================
    
    private fun getParam(name: String): String {
        return _state.value.parameterValues[name] ?: ""
    }
    
    private fun updateResultWithRaw(rawJson: String, summary: String) {
        _state.update {
            it.copy(
                testResult = TestResult.Success(
                    rawJson = rawJson,
                    summary = summary,
                    duration = 0 // 会在 executeTest 中被覆盖
                )
            )
        }
    }
}
