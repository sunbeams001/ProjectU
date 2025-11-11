package com.projectu.ui.screens.apitest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.remote.api.PixivApi
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
                        ApiMethod.GetBookmarkTags -> testGetBookmarkTags()
                        
                        // ==================== RankingApi ====================
                        ApiMethod.GetDailyRanking -> testGetDailyRanking()
                        ApiMethod.GetWeeklyRanking -> testGetWeeklyRanking()
                        ApiMethod.GetMonthlyRanking -> testGetMonthlyRanking()
                        ApiMethod.GetRookieRanking -> testGetRookieRanking()
                        ApiMethod.GetOriginalRanking -> testGetOriginalRanking()
                        ApiMethod.GetMaleRanking -> testGetMaleRanking()
                        ApiMethod.GetFemaleRanking -> testGetFemaleRanking()
                        ApiMethod.GetR18DailyRanking -> testGetR18DailyRanking()
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
        
        val requestBody = mapOf(
            "illust_id" to illustId,
            "restrict" to restrict,
            "comment" to comment,
            "tags" to tagList
        )
        
        val responseWithRaw = pixivApi.client.postJsonWithRaw<Map<String, Any>>(
            "/ajax/illusts/bookmarks/add",
            requestBody
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("⚠️ 收藏操作已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("公开性: ${if (restrict == 0) "公开" else "私密"}")
            appendLine("评论: ${comment.ifBlank { "无" }}")
            appendLine("标签: ${tagList.joinToString().ifBlank { "无" }}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testDeleteBookmark() {
        val bookmarkId = getParam("bookmarkId")
        
        val responseWithRaw = pixivApi.client.postFormWithRaw<Unit>(
            "/ajax/illusts/bookmarks/delete",
            mapOf("bookmark_id" to bookmarkId)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("⚠️ 取消收藏已执行")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("收藏ID: $bookmarkId")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetBookmarkTags() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 使用用户收藏 API，可以间接获取标签信息
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.UserBookmarkBody>(
            "/ajax/user/$userId/illusts/bookmarks",
            mapOf("limit" to 10)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户收藏获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("注意: 使用收藏列表 API 代替收藏标签")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== RankingApi 测试方法 ====================
    
    private suspend fun testGetDailyRanking() = testRanking("日榜", "daily")
    
    private suspend fun testGetWeeklyRanking() = testRanking("周榜", "weekly")
    
    private suspend fun testGetMonthlyRanking() = testRanking("月榜", "monthly")
    
    private suspend fun testGetRookieRanking() = testRanking("新人榜", "rookie")
    
    private suspend fun testGetOriginalRanking() = testRanking("原创榜", "original")
    
    private suspend fun testGetMaleRanking() = testRanking("男性向", "male")
    
    private suspend fun testGetFemaleRanking() = testRanking("女性向", "female")
    
    private suspend fun testGetR18DailyRanking() = testRanking("R18 日榜", "daily_r18")
    
    private suspend fun testRanking(name: String, mode: String) {
        val page = getParam("page").toIntOrNull() ?: 1
        val date = getParam("date").ifBlank { null }
        
        val params = mutableMapOf<String, Any?>(
            "mode" to mode,
            "p" to page
        )
        if (date != null) {
            params["date"] = date
        }
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.pixiv.RankingResponse>(
            "/ranking.php",
            params
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ $name 获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: $mode")
            appendLine("页码: $page")
            appendLine("日期: ${date ?: "最新"}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
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
