package com.projectu.ui.screens.apitest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.dto.bookmark.BookmarkAddResponse
import com.projectu.shared.data.remote.dto.bookmark.BookmarkRequest
import com.projectu.shared.data.remote.dto.bookmark.BookmarkTagsResponse
import com.projectu.shared.data.remote.dto.comment.CommentsBody
import com.projectu.shared.data.remote.dto.comment.DeleteCommentResult
import com.projectu.shared.data.remote.dto.comment.PostCommentResult
import com.projectu.shared.data.remote.dto.illust.DiscoveryBody
import com.projectu.shared.data.remote.dto.illust.FollowLatestBody
import com.projectu.shared.data.remote.dto.illust.IllustDetailBody
import com.projectu.shared.data.remote.dto.illust.IllustRecommendBody
import com.projectu.shared.data.remote.dto.illust.IllustRecommendInitBody
import com.projectu.shared.data.remote.dto.illust.IllustSearchBody
import com.projectu.shared.data.remote.dto.illust.PageInfo
import com.projectu.shared.data.remote.dto.illust.UgoiraMetaBody
import com.projectu.shared.data.remote.dto.illust_series.IllustSeriesBody
import com.projectu.shared.data.remote.dto.bookmark.NovelBookmarkRequest
import com.projectu.shared.data.remote.dto.novel.NovelBookmarkStatusBody
import com.projectu.shared.data.remote.dto.novel.NovelDetailBody
import com.projectu.shared.data.remote.dto.novel.NovelRecommendBody
import com.projectu.shared.data.remote.dto.novel.NovelRecommendInitBody
import com.projectu.shared.data.remote.dto.novel.NovelSearchBody
import com.projectu.shared.data.remote.dto.novel_series.NovelSeriesBody
import com.projectu.shared.data.remote.dto.novel_series.NovelSeriesContentBody
import com.projectu.shared.data.remote.dto.novel_series.NovelSeriesTitle
import com.projectu.shared.data.remote.dto.ranking.RankingResponse
import com.projectu.shared.data.remote.dto.tag.SearchSuggestionBody
import com.projectu.shared.data.remote.dto.tag.TagInfoBody
import com.projectu.shared.data.remote.dto.tag.TagSearchSuggestBody
import com.projectu.shared.data.remote.dto.tag.TagSuggestBody
import com.projectu.shared.data.remote.dto.user.ProfileAllBody
import com.projectu.shared.data.remote.dto.user.ProfileNovelsBody
import com.projectu.shared.data.remote.dto.user.DiscoveryUsersBody
import com.projectu.shared.data.remote.dto.bookmark.UserBookmarkIllustsBody
import com.projectu.shared.data.remote.dto.bookmark.UserBookmarkNovelsBody
import com.projectu.shared.data.remote.dto.user.MyPixivBody
import com.projectu.shared.data.remote.dto.user.UserFollowDetailBody
import com.projectu.shared.data.remote.dto.user.UserFollowingBody
import com.projectu.shared.data.remote.dto.user.UserInfoBody
import com.projectu.shared.data.remote.dto.user.UserRecommendBody
import com.projectu.shared.data.remote.dto.user.UserSearchBody
import com.projectu.shared.data.remote.model.RankingCategory
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingMode
import io.ktor.http.encodeURLPathPart
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
                        ApiMethod.GetRecommendInit -> testGetRecommendInit()
                        ApiMethod.GetRecommendIllusts -> testGetRecommendIllusts()
                        ApiMethod.GetDiscoveryIllust -> testGetDiscoveryIllust()
                        ApiMethod.GetUgoiraMetadata -> testGetUgoiraMetadata()
                        ApiMethod.GetIllustPages -> testGetIllustPages()
                        
                        // ==================== UserApi ====================
                        ApiMethod.GetUserInfo -> testGetUserInfo()
                        ApiMethod.GetUserFullInfo -> testGetUserFullInfo()
                        ApiMethod.GetUserIllusts -> testGetUserIllusts()
                        ApiMethod.GetUserNovels -> testGetUserNovels()
                        ApiMethod.GetUserFollowing -> testGetUserFollowing()
                        ApiMethod.GetUserFollowers -> testGetUserFollowers()
                        ApiMethod.GetMyPixiv -> testGetMyPixiv()
                        ApiMethod.GetRecommendUsers -> testGetRecommendUsers()
                        ApiMethod.GetDiscoveryUsers -> testGetDiscoveryUsers()
                        ApiMethod.FollowUser -> testFollowUser()
                        ApiMethod.UnfollowUser -> testUnfollowUser()
                        ApiMethod.GetUserFollowDetail -> testGetUserFollowDetail()
                        
                        // ==================== BookmarkApi ====================
                        ApiMethod.GetUserBookmarkIllusts -> testGetUserBookmarkIllusts()
                        ApiMethod.GetUserBookmarkNovels -> testGetUserBookmarkNovels()
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
                        
                        // ==================== CommentApi ====================
                        ApiMethod.GetIllustCommentRoots -> testGetIllustCommentRoots()
                        ApiMethod.GetCommentReplies -> testGetCommentReplies()
                        ApiMethod.PostIllustComment -> testPostIllustComment()
                        ApiMethod.DeleteIllustComment -> testDeleteIllustComment()
                        ApiMethod.GetNovelCommentRoots -> testGetNovelCommentRoots()
                        ApiMethod.GetNovelCommentReplies -> testGetNovelCommentReplies()
                        ApiMethod.PostNovelComment -> testPostNovelComment()
                        ApiMethod.DeleteNovelComment -> testDeleteNovelComment()
                        
                        // ==================== NovelApi ====================
                        ApiMethod.GetNovelDetail -> testGetNovelDetail()
                        ApiMethod.GetNovelBookmarkData -> testGetNovelBookmarkData()
                        ApiMethod.GetNovelDiscovery -> testGetNovelDiscovery()
                        ApiMethod.GetNovelRecommendInit -> testGetNovelRecommendInit()
                        ApiMethod.GetRecommendNovels -> testGetRecommendNovels()
                        
                        // ==================== IllustSeriesApi ====================
                        ApiMethod.GetIllustSeriesDetail -> testGetIllustSeriesDetail()
                        ApiMethod.WatchIllustSeries -> testWatchIllustSeries()
                        ApiMethod.UnwatchIllustSeries -> testUnwatchIllustSeries()
                        
                        // ==================== FollowApi ====================
                        ApiMethod.GetFollowLatestIllust -> testGetFollowLatestIllust()
                        ApiMethod.GetFollowLatestNovel -> testGetFollowLatestNovel()
                        ApiMethod.GetWatchListManga -> testGetWatchListManga()
                        ApiMethod.GetWatchListNovel -> testGetWatchListNovel()
                        
                        // ==================== NovelSeriesApi ====================
                        ApiMethod.GetNovelSeriesDetail -> testGetNovelSeriesDetail()
                        ApiMethod.GetNovelSeriesContents -> testGetNovelSeriesContents()
                        ApiMethod.GetNovelSeriesTitles -> testGetNovelSeriesTitles()
                        ApiMethod.WatchNovelSeries -> testWatchNovelSeries()
                        ApiMethod.UnwatchNovelSeries -> testUnwatchNovelSeries()
                        
                        // ==================== TagApi ====================
                        ApiMethod.GetTagSuggest -> testGetTagSuggest()
                        ApiMethod.GetSearchSuggestion -> testGetSearchSuggestion()
                        ApiMethod.GetTagSearchSuggest -> testGetTagSearchSuggest()
                        ApiMethod.GetTagInfo -> testGetTagInfo()
                        
                        // ==================== MarkerApi ====================
                        ApiMethod.AddNovelMarker -> testAddNovelMarker()
                        ApiMethod.DeleteNovelMarker -> testDeleteNovelMarker()
                        ApiMethod.GetNovelMarkerList -> testGetNovelMarkerList()
                        
                        // ==================== SearchApi ====================
                        ApiMethod.SearchIllust -> testSearchIllust()
                        ApiMethod.SearchNovel -> testSearchNovel()
                        ApiMethod.SearchUser -> testSearchUser()
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
        val responseWithRaw = pixivApi.client.getWithRaw<IllustDetailBody>(
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
        
        // URL编码关键词（使用encodeURLPathPart以确保/等特殊字符被正确编码）
        val encodedKeyword = keyword.encodeURLPathPart()
        
        val responseWithRaw = pixivApi.client.getWithRaw<IllustSearchBody>(
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<IllustRecommendInitBody>(
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<IllustRecommendBody>(
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<DiscoveryBody>(
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<UgoiraMetaBody>(
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
    
    private suspend fun testGetIllustPages() {
        val illustId = getParam("illustId").toLongOrNull() ?: 137776727L
        
        val responseWithRaw = pixivApi.client.getWithRaw<List<PageInfo>>(
            "/ajax/illust/$illustId/pages"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 多页作品详情获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            val pages = response.body
            if (pages != null) {
                appendLine("页数: ${pages.size}")
                appendLine()
                pages.forEachIndexed { index, page ->
                    appendLine("📄 第 ${index + 1} 页:")
                    appendLine("  尺寸: ${page.width} x ${page.height}")
                    appendLine("  📐 完整 URL 级别:")
                    appendLine("    • thumb_mini (128x128):")
                    appendLine("      ${page.urls.thumb_mini}")
                    appendLine("    • small (540x540):")
                    appendLine("      ${page.urls.small}")
                    appendLine("    • regular (master1200):")
                    appendLine("      ${page.urls.regular}")
                    appendLine("    • original (原图 ${page.width}x${page.height}):")
                    appendLine("      ${page.urls.original}")
                    if (index < pages.size - 1) appendLine()
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💡 提示: 每页都有 4 个级别的 URL (mini/small/regular/original)")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== UserApi 测试方法 ====================
    
    private suspend fun testGetUserInfo() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserInfoBody>(
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
        
        // 调用 getProfileAll 获取用户的作品概况（作品ID列表等）
        val responseWithRaw = pixivApi.client.getWithRaw<ProfileAllBody>(
            "/ajax/user/$userId/profile/all"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户作品概况获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            
            response.body?.let { body ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 作品统计:")
                appendLine("插画数量: ${body.illusts?.size ?: 0}")
                appendLine("漫画数量: ${body.manga?.size ?: 0}")
                appendLine("小说数量: ${body.novels?.size ?: 0}")
                appendLine("漫画系列: ${body.mangaSeries?.size ?: 0}")
                appendLine("小说系列: ${body.novelSeries?.size ?: 0}")
                appendLine("收藏集: ${body.collections?.size ?: 0}")
                
                body.bookmarkCount?.let { bookmarks ->
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📚 收藏统计:")
                    appendLine("公开插画: ${bookmarks.public?.illust ?: 0}")
                    appendLine("公开小说: ${bookmarks.public?.novel ?: 0}")
                    appendLine("私密插画: ${bookmarks.private?.illust ?: 0}")
                    appendLine("私密小说: ${bookmarks.private?.novel ?: 0}")
                }
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetUserIllusts() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 首先获取作品ID列表
        val responseWithRaw = pixivApi.client.getWithRaw<ProfileAllBody>(
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
    
    private suspend fun testGetUserNovels() {
        val userId = getParam("userId").toLongOrNull() ?: 18662946L
        val novelIdsStr = getParam("novelIds")
        val novelIds = novelIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (novelIds.isEmpty()) {
            _state.update {
                it.copy(
                    testResult = TestResult.Error(
                        message = "参数错误：请提供至少一个小说ID\n格式：用逗号分隔多个ID\n示例：26469344,26469328,25637544"
                    )
                )
            }
            return
        }
        
        // 调用 getProfileNovels 获取用户小说作品详情
        val responseWithRaw = pixivApi.client.getWithRaw<ProfileNovelsBody>(
            "/ajax/user/$userId/profile/novels",
            mapOf("ids[]" to novelIds)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户小说作品获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("请求小说数: ${novelIds.size}")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            
            response.body?.let { body ->
                val works = body.works
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📚 返回小说数: ${works?.size ?: 0}")
                
                works?.entries?.take(5)?.forEach { (id, novel) ->
                    novel?.let {
                        appendLine("━━━━━━━━━━━━━━━━━━━━━")
                        appendLine("ID: $id")
                        appendLine("标题: ${it.title}")
                        appendLine("作者: ${it.userName}")
                        appendLine("字数: ${it.textCount}")
                        appendLine("收藏数: ${it.bookmarkCount}")
                        appendLine("创建时间: ${it.createDate}")
                    }
                }
                
                if ((works?.size ?: 0) > 5) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("... 还有 ${(works?.size ?: 0) - 5} 篇小说")
                }
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }

    private suspend fun testGetUserBookmarkIllusts() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val tag = getParam("tag")
        val rest = getParam("rest").ifBlank { "show" }
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 48
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserBookmarkIllustsBody>(
            "/ajax/user/$userId/illusts/bookmarks",
            mapOf(
                "tag" to tag,
                "offset" to offset,
                "limit" to limit,
                "rest" to rest
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户收藏的插画·漫画获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("标签: ${tag.ifBlank { "全部" }}")
            appendLine("公开性: $rest")
            appendLine("偏移: $offset")
            appendLine("限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            response.body?.let { body ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📚 总数: ${body.total}")
                appendLine("📖 返回数: ${body.works.size}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }

    private suspend fun testGetUserBookmarkNovels() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val tag = getParam("tag")
        val rest = getParam("rest").ifBlank { "show" }
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 30
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserBookmarkNovelsBody>(
            "/ajax/user/$userId/novels/bookmarks",
            mapOf(
                "tag" to tag,
                "offset" to offset,
                "limit" to limit,
                "rest" to rest
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户收藏的小说获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("标签: ${tag.ifBlank { "全部" }}")
            appendLine("公开性: $rest")
            appendLine("偏移: $offset")
            appendLine("限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            response.body?.let { body ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📚 总数: ${body.total}")
                appendLine("📖 返回数: ${body.works.size}")
                if (body.works.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📝 前3篇小说:")
                    body.works.take(3).forEach { novel ->
                        appendLine("  • ${novel.title}")
                        appendLine("    ID: ${novel.id} | 作者: ${novel.userName}")
                        appendLine("    字数: ${novel.textCount} | 收藏数: ${novel.bookmarkCount}")
                    }
                }
            }
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserFollowingBody>(
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
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserFollowingBody>(
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
    
    private suspend fun testGetMyPixiv() {
        val userId = getParam("userId").toLongOrNull() ?: 4966721L
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 24
        
        // 使用好P友列表接口
        val response = pixivApi.userApi.getMyPixiv(
            uid = userId,
            offset = offset,
            limit = limit
        )
        
        val responseWithRaw = pixivApi.client.getWithRaw<MyPixivBody>(
            "/ajax/user/$userId/mypixiv",
            mapOf(
                "offset" to offset,
                "limit" to limit
            )
        )
        
        val summary = buildString {
            appendLine("✅ 好P友列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("偏移量: $offset")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            val body = response.body
            if (body != null) {
                appendLine("好P友总数: ${body.total}")
                appendLine("当前返回数量: ${body.users.size}")
                if (body.users.isNotEmpty()) {
                    appendLine()
                    appendLine("好P友列表:")
                    body.users.take(5).forEachIndexed { index, user ->
                        appendLine("${index + 1}. ${user.userName} (ID: ${user.userId})")
                        appendLine("   作品数: ${user.illusts.size}")
                        user.userComment?.takeIf { it.isNotBlank() }?.let { comment ->
                            appendLine("   简介: ${comment.take(50)}...")
                        }
                    }
                    if (body.users.size > 5) {
                        appendLine("... 还有 ${body.users.size - 5} 个好P友")
                    }
                }
            }
            appendLine()
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetRecommendUsers() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val userNum = getParam("userNum").toIntOrNull() ?: 20
        val workNum = getParam("workNum").toIntOrNull() ?: 3
        val isR18 = getParam("isR18").toBoolean()
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserRecommendBody>(
            "/ajax/user/$userId/recommends",
            mapOf(
                "userNum" to userNum,
                "workNum" to workNum,
                "isR18" to isR18
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 推荐用户获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("基准用户ID: $userId")
            appendLine("推荐用户数: $userNum")
            appendLine("作品数量: $workNum")
            appendLine("包含R18: $isR18")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            
            response.body?.let { body ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 推荐统计:")
                appendLine("推荐用户数: ${body.recommendUsers.size}")
                appendLine("插画缩略图数: ${body.thumbnails?.illust?.size ?: 0}")
                appendLine("小说缩略图数: ${body.thumbnails?.novel?.size ?: 0}")
                
                body.recommendUsers.take(3).forEach { user ->
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("👤 用户 ID: ${user.userId}")
                    appendLine("   插画作品 ID: ${user.illustIds.joinToString(", ")}")
                    appendLine("   小说作品 ID: ${user.novelIds.joinToString(", ")}")
                    
                    // 从缩略图中获取详细信息
                    body.thumbnails?.illust?.let { illusts ->
                        val userIllusts = illusts.filter { it.userId == user.userId }
                        if (userIllusts.isNotEmpty()) {
                            appendLine("   插画标题: ${userIllusts.first().title}")
                        }
                    }
                }
                
                if (body.recommendUsers.size > 3) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("... 还有 ${body.recommendUsers.size - 3} 个用户")
                }
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetDiscoveryUsers() {
        val limit = getParam("limit").toIntOrNull() ?: 20
        
        val responseWithRaw = pixivApi.client.getWithRaw<DiscoveryUsersBody>(
            "/ajax/discovery/users",
            mapOf(
                "limit" to limit
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 发现用户获取成功（总体推荐）")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("推荐数量: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            
            response.body?.let { body ->
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 推荐统计:")
                appendLine("推荐用户数: ${body.users.size}")
                appendLine("推荐条目数: ${body.recommendedUsers.size}")
                appendLine("插画缩略图数: ${body.thumbnails.illust.size}")
                appendLine("小说缩略图数: ${body.thumbnails.novel.size}")
                appendLine("标签翻译数: ${body.tagTranslation.size}")
                
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("👥 推荐用户列表:")
                body.users.take(5).forEach { user ->
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("👤 用户名: ${user.name}")
                    appendLine("   用户ID: ${user.userId}")
                    appendLine("   简介: ${user.comment.take(50)}${if (user.comment.length > 50) "..." else ""}")
                    appendLine("   已关注: ${if (user.isFollowed) "是" else "否"}")
                    appendLine("   高级会员: ${if (user.premium) "是" else "否"}")
                    appendLine("   被对方关注: ${if (user.followedBack) "是" else "否"}")
                    
                    // 查找该用户的作品
                    val userEntry = body.recommendedUsers.find { it.userId == user.userId }
                    userEntry?.let { entry ->
                        if (entry.recentIllustIds.isNotEmpty()) {
                            appendLine("   最近插画: ${entry.recentIllustIds.take(3).joinToString(", ")}")
                        }
                        if (entry.recentNovelIds.isNotEmpty()) {
                            appendLine("   最近小说: ${entry.recentNovelIds.take(3).joinToString(", ")}")
                        }
                    }
                }
                
                if (body.users.size > 5) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("... 还有 ${body.users.size - 5} 个用户")
                }
                
                // 显示部分缩略图信息
                if (body.thumbnails.illust.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("🎨 示例插画作品:")
                    body.thumbnails.illust.take(3).forEach { illust ->
                        appendLine("━━━━━━━━━━━━━━━━━━━━━")
                        appendLine("作品ID: ${illust.id}")
                        appendLine("标题: ${illust.title}")
                        appendLine("作者: ${illust.userName} (ID: ${illust.userId})")
                        appendLine("尺寸: ${illust.width}x${illust.height}")
                        appendLine("AI类型: ${when(illust.aiType) {
                            0 -> "非AI"
                            1 -> "AI生成"
                            2 -> "AI辅助"
                            else -> "未知"
                        }}")
                        appendLine("收藏状态: ${if (illust.isBookmarkable) "可收藏" else "不可收藏"}")
                    }
                }
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果（包括标签翻译等详细信息）")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testFollowUser() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        val tag = getParam("tag")
        val restrict = getParam("restrict").toIntOrNull() ?: 0
        
        // 直接调用API，返回空数组字符串表示成功
        val result = pixivApi.userApi.followUser(userId, tag, restrict)
        val rawJson = result.toString()
        
        val summary = buildString {
            // 空数组表示成功
            val isSuccess = rawJson == "[]"
            if (isSuccess) {
                appendLine("✅ 关注用户成功")
            } else {
                appendLine("❌ 关注用户失败（响应异常）")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("标签: ${if (tag.isBlank()) "(无)" else tag}")
            appendLine("公开性: ${if (restrict == 0) "公开" else "私密"}")
            appendLine("响应: $rawJson")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            
            if (isSuccess) {
                appendLine("💡 提示:")
                appendLine("  - 可以在用户个人页面查看关注状态")
                appendLine("  - 可以使用 getUserFollowing 查看关注列表")
            } else {
                appendLine("⚠️ 可能的原因:")
                appendLine("  1. 未登录或登录已过期")
                appendLine("  2. 用户ID不存在")
                appendLine("  3. 已经关注过该用户")
                appendLine("  4. 网络请求失败")
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testUnfollowUser() {
        val userId = getParam("userId").toLongOrNull() ?: 11L
        
        // 直接调用API，返回 UnfollowUserResponse
        val result = pixivApi.userApi.unfollowUser(userId)
        val rawJson = """{"user_id":"${result.userId}","type":"${result.type}"}"""
        
        val summary = buildString {
            // 有响应对象表示成功
            appendLine("✅ 取消关注成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("用户ID: $userId")
            appendLine("响应用户ID: ${result.userId}")
            appendLine("类型: ${result.type}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            
            appendLine("💡 提示:")
            appendLine("  - 可以在用户个人页面确认取消关注")
            appendLine("  - 可以使用 getUserFollowing 查看关注列表")
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testGetUserFollowDetail() {
        val userId = getParam("userId").toLongOrNull() ?: 58277L
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserFollowDetailBody>(
            "/ajax/following/user/details",
            mapOf("user_id" to userId)
        )
        val response = responseWithRaw.response
        val body = response.body
        
        val summary = buildString {
            if (response.error || body == null) {
                appendLine("❌ 获取关注详情失败")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("用户ID: $userId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
                appendLine()
                appendLine("⚠️ 注意：此接口只能查询已关注的用户")
                appendLine("   未关注的用户会返回错误")
            } else {
                appendLine("✅ 用户关注详情获取成功")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("用户ID: ${body.userId}")
                appendLine("用户名: ${body.userName}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                
                // 关注类型
                val restrictText = when (body.restrict) {
                    "0" -> "公开关注"
                    "1" -> "悄悄关注（私密）"
                    else -> "未知类型 (${body.restrict})"
                }
                appendLine("关注类型: $restrictText")
                
                // 标签信息
                if (body.tags.isNotEmpty()) {
                    appendLine()
                    appendLine("【关注标签】(${body.tags.size} 个)")
                    body.tags.forEach { tag ->
                        appendLine("  • $tag")
                    }
                } else {
                    appendLine("标签: 无")
                }
                
                appendLine()
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("💡 用途:")
                appendLine("  • 精确获取用户关注状态（公开/悄悄关注）")
                appendLine("  • 用于修复Discovery接口不返回关注类型的问题")
                appendLine("  • 同步全局状态缓存的关注状态")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
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
        val responseWithRaw = pixivApi.client.getWithRaw<RankingResponse>(
            "/ranking.php",
            params
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ ${mode.displayName}排行榜 获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: ${mode.value} (${mode.displayName})")
            appendLine("分类: ${if (mode.category == RankingCategory.GENERAL) "一般" else "R-18"}")
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
        val contentStr = getParam("content").ifBlank { "novel" }
        val date = getParam("date").ifBlank { null }
        
        // 转换为枚举
        val mode = RankingMode.fromValue(modeStr) ?: RankingMode.DAILY
        val content = RankingContent.fromValue(contentStr) ?: RankingContent.NOVEL
        
        // 构建请求参数（用于显示）
        val params = mutableMapOf<String, Any?>(
            "mode" to mode.value,
            "content" to content.value,
            "p" to page
        )
        if (date != null) {
            params["date"] = date
        }
        
        // 调用 API（使用 getWithRaw 获取原始 JSON）
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.ranking.NovelRankingBody>(
            "/ajax/ranking/novel",
            params
        )
        val response = responseWithRaw.response
        
        // 获取 body
        val body = response.body ?: throw IllegalStateException("小说排行榜数据为空")
        
        val summary = buildString {
            appendLine("✅ ${mode.displayName}小说排行榜 获取成功 (JSON接口)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: ${mode.value} (${mode.displayName})")
            appendLine("分类: ${when (mode.category) {
                RankingCategory.GENERAL -> "一般"
                RankingCategory.R18 -> "R-18"
                RankingCategory.R18G -> "R-18G"
            }}")
            appendLine("页码: ${body.displayA.page}")
            val hasNext = body.displayA.next != null
            val hasPrev = body.displayA.prev != null
            appendLine("分页: ${if (hasPrev) "← " else ""}第${body.displayA.page}页${if (hasNext) " →" else ""}")
            body.date?.let { appendLine("日期: $it") }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说数量: ${body.displayA.rankA.size}")
            appendLine("屏蔽数量: ${body.displayA.mutedCount}")
            appendLine("")
            
            // 显示前3条小说完整信息
            body.displayA.rankA.take(3).forEachIndexed { index, novel ->
                appendLine("【${novel.rank}位】${novel.title}")
                appendLine("  ID: ${novel.id}")
                appendLine("  作者: ${novel.userName} (ID: ${novel.userId})")
                appendLine("  创建时间: ${novel.createDate}")
                appendLine("  字数: ${novel.characterCount} (${novel.wordCount} 词)")
                appendLine("  阅读时长: ${novel.readingTime} 秒")
                appendLine("  书签: ${novel.bookmarkCount}")
                appendLine("  语言: ${novel.language}")
                appendLine("  AI类型: ${novel.aiType}")
                if (novel.seriesId != null) {
                    appendLine("  系列: ${novel.seriesTitle} (ID: ${novel.seriesId})")
                }
                appendLine("  标签: ${novel.tagA.take(5).joinToString(", ")}")
                if (novel.isBookmarked) {
                    appendLine("  ⭐ 已收藏")
                }
                if (novel.comment.isNotBlank() && novel.comment.length > 50) {
                    appendLine("  简介: ${novel.comment.take(50)}...")
                }
                if (index < 2) appendLine("")
            }
            
            if (body.displayA.rankA.size > 3) {
                appendLine("")
                appendLine("... 还有 ${body.displayA.rankA.size - 3} 部小说")
            }
            
            appendLine("")
            appendLine("💡 提示: 查看 JSON 标签页获取完整数据（含封面、完整简介等）")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== CommentApi 测试方法 ====================
    
    private suspend fun testGetIllustCommentRoots() {
        val illustId = getParam("illustId").toLongOrNull() ?: 102814610L
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 20
        
        val responseWithRaw = pixivApi.client.getWithRaw<CommentsBody>(
            "/ajax/illusts/comments/roots",
            mapOf(
                "illust_id" to illustId,
                "offset" to offset,
                "limit" to limit
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 插画评论获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("作品ID: $illustId")
            appendLine("偏移: $offset")
            appendLine("限制: $limit")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetCommentReplies() {
        val commentId = getParam("commentId")
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<CommentsBody>(
            "/ajax/illusts/comments/replies",
            mapOf(
                "comment_id" to commentId,
                "page" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 评论回复获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("评论ID: $commentId")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testPostIllustComment() {
        val illustId = getParam("illustId").toLongOrNull() ?: 102814610L
        val userId = getParam("userId").toLongOrNull() 
            ?: throw IllegalArgumentException("用户ID不能为空")
        val comment = getParam("comment").takeIf { it.isNotEmpty() }
        val stampId = getParam("stampId").toIntOrNull()
        val parentCommentId = getParam("parentCommentId").toLongOrNull()
        
        if (comment == null && stampId == null) {
            throw IllegalArgumentException("评论内容和表情ID至少需要提供一个")
        }
        
        val result = pixivApi.commentApi.postIllustComment(
            illustId = illustId,
            userId = userId,
            comment = comment,
            stampId = stampId,
            parentCommentId = parentCommentId
        )
        
        val rawJson = when (result) {
            is PostCommentResult.Success ->
                """{"error":false,"message":"","body":{"comment_id":${result.commentId}}}"""
            is PostCommentResult.Error ->
                """{"error":true,"message":"${result.message}","body":[]}"""
        }
        
        val summary = buildString {
            when (result) {
                is PostCommentResult.Success -> {
                    appendLine("✅ 插画评论发布成功")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("作品ID: $illustId")
                    appendLine("用户ID: $userId")
                    comment?.let { appendLine("评论内容: $it") }
                    stampId?.let { appendLine("表情ID: $it") }
                    parentCommentId?.let { appendLine("父评论ID: $it") }
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("评论ID: ${result.commentId}")
                }
                is PostCommentResult.Error -> {
                    appendLine("❌ 插画评论发布失败")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("作品ID: $illustId")
                    appendLine("用户ID: $userId")
                    comment?.let { appendLine("评论内容: $it") }
                    stampId?.let { appendLine("表情ID: $it") }
                    parentCommentId?.let { appendLine("父评论ID: $it") }
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("错误信息: ${result.message}")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testDeleteIllustComment() {
        val illustId = getParam("illustId").toLongOrNull() ?: 102814610L
        val commentId = getParam("commentId").toLongOrNull()
            ?: throw IllegalArgumentException("评论ID不能为空")
        
        val result = pixivApi.commentApi.deleteIllustComment(
            illustId = illustId,
            commentId = commentId
        )
        
        when (result) {
            is DeleteCommentResult.Success -> {
                val rawJson = """{"error":false,"message":"ok","body":[]}"""
                
                val summary = buildString {
                    appendLine("✅ 插画评论删除成功")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("作品ID: $illustId")
                    appendLine("评论ID: $commentId")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("状态: 成功删除")
                }
                
                updateResultWithRaw(rawJson, summary)
            }
            is DeleteCommentResult.Error -> {
                val rawJson = """{"error":true,"message":"${result.message}","body":[]}"""
                
                val summary = buildString {
                    appendLine("❌ 插画评论删除失败")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("作品ID: $illustId")
                    appendLine("评论ID: $commentId")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("错误信息: ${result.message}")
                }
                
                updateResultWithRaw(rawJson, summary)
            }
        }
    }
    
    private suspend fun testGetNovelCommentRoots() {
        val novelId = getParam("novelId").toLongOrNull() ?: 15809265L
        val offset = getParam("offset").toIntOrNull() ?: 0
        val limit = getParam("limit").toIntOrNull() ?: 20
        
        val responseWithRaw = pixivApi.client.getWithRaw<CommentsBody>(
            "/ajax/novels/comments/roots",
            mapOf(
                "novel_id" to novelId,
                "offset" to offset,
                "limit" to limit
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说评论获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("偏移: $offset")
            appendLine("限制: $limit")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelCommentReplies() {
        val commentId = getParam("commentId")
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<CommentsBody>(
            "/ajax/novels/comments/replies",
            mapOf(
                "comment_id" to commentId,
                "page" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说评论回复获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("评论ID: $commentId")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testPostNovelComment() {
        val novelId = getParam("novelId").toLongOrNull() ?: 15809265L
        val userId = getParam("userId").toLongOrNull() 
            ?: throw IllegalArgumentException("用户ID不能为空")
        val comment = getParam("comment").takeIf { it.isNotEmpty() }
        val stampId = getParam("stampId").toIntOrNull()
        val parentCommentId = getParam("parentCommentId").toLongOrNull()
        
        if (comment == null && stampId == null) {
            throw IllegalArgumentException("评论内容和表情ID至少需要提供一个")
        }
        
        val result = pixivApi.commentApi.postNovelComment(
            novelId = novelId,
            userId = userId,
            comment = comment,
            stampId = stampId,
            parentCommentId = parentCommentId
        )
        
        val rawJson = when (result) {
            is PostCommentResult.Success ->
                """{"error":false,"message":"","body":{"comment_id":${result.commentId}}}"""
            is PostCommentResult.Error ->
                """{"error":true,"message":"${result.message}","body":[]}"""
        }
        
        val summary = buildString {
            when (result) {
                is PostCommentResult.Success -> {
                    appendLine("✅ 小说评论发布成功")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说ID: $novelId")
                    appendLine("用户ID: $userId")
                    comment?.let { appendLine("评论内容: $it") }
                    stampId?.let { appendLine("表情ID: $it") }
                    parentCommentId?.let { appendLine("父评论ID: $it") }
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("评论ID: ${result.commentId}")
                }
                is PostCommentResult.Error -> {
                    appendLine("❌ 小说评论发布失败")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说ID: $novelId")
                    appendLine("用户ID: $userId")
                    comment?.let { appendLine("评论内容: $it") }
                    stampId?.let { appendLine("表情ID: $it") }
                    parentCommentId?.let { appendLine("父评论ID: $it") }
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("错误信息: ${result.message}")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testDeleteNovelComment() {
        val novelId = getParam("novelId").toLongOrNull() ?: 15809265L
        val commentId = getParam("commentId").toLongOrNull()
            ?: throw IllegalArgumentException("评论ID不能为空")
        
        val result = pixivApi.commentApi.deleteNovelComment(
            novelId = novelId,
            commentId = commentId
        )
        
        when (result) {
            is DeleteCommentResult.Success -> {
                val rawJson = """{"error":false,"message":"ok","body":[]}"""
                
                val summary = buildString {
                    appendLine("✅ 小说评论删除成功")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说ID: $novelId")
                    appendLine("评论ID: $commentId")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("状态: 成功删除")
                }
                
                updateResultWithRaw(rawJson, summary)
            }
            is DeleteCommentResult.Error -> {
                val rawJson = """{"error":true,"message":"${result.message}","body":[]}"""
                
                val summary = buildString {
                    appendLine("❌ 小说评论删除失败")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说ID: $novelId")
                    appendLine("评论ID: $commentId")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("错误信息: ${result.message}")
                }
                
                updateResultWithRaw(rawJson, summary)
            }
        }
    }
    
    // ==================== NovelApi 测试方法 ====================
    
    private suspend fun testGetNovelDetail() {
        val novelId = getParam("novelId").toLongOrNull() ?: 15809265L
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelDetailBody>(
            "/ajax/novel/$novelId"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说详情获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelBookmarkData() {
        val novelId = getParam("novelId").toLongOrNull() ?: 15809265L
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelBookmarkStatusBody>(
            "/ajax/novel/$novelId/bookmarkData"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说收藏状态获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            val body = response.body
            if (body != null) {
                appendLine("小说ID: ${body.id}")
                appendLine("可收藏: ${if (body.isBookmarkable) "是" else "否"}")
                val bookmark = body.bookmarkData
                if (bookmark != null) {
                    appendLine("收藏状态: ⭐ 已收藏")
                    appendLine("收藏ID: ${bookmark.id}")
                    appendLine("公开性: ${if (bookmark.private) "私密" else "公开"}")
                } else {
                    appendLine("收藏状态: ☆ 未收藏")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testSearchNovel() {
        val keyword = getParam("keyword")
        val searchMode = getParam("searchMode")
        val order = getParam("order")
        val mode = getParam("mode")
        val page = getParam("page").toIntOrNull() ?: 1
        
        // URL编码关键词（使用encodeURLPathPart以确保/箉特殊字符被正确编码）
        val encodedKeyword = keyword.encodeURLPathPart()
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelSearchBody>(
            "/ajax/search/novels/$encodedKeyword",
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
            appendLine("✅ 小说搜索成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("关键词: $keyword")
            appendLine("搜索模式: $searchMode")
            appendLine("排序: $order")
            appendLine("模式: $mode")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testSearchUser() {
        val keyword = getParam("keyword")
        val searchMode = getParam("searchMode")
        val hasWork = getParam("hasWork").toIntOrNull() ?: 1
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<UserSearchBody>(
            "/ajax/search/users",
            mapOf(
                "nick" to keyword,
                "s_mode" to searchMode,
                "i" to hasWork,
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 用户搜索成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("关键词: $keyword")
            appendLine("搜索模式: $searchMode (${if (searchMode == "s_usr_full") "完全一致" else "部分一致"})")
            appendLine("投稿作品: ${if (hasWork == 1) "仅有作品用户" else "全部用户"}")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("用户总数: ${body.page.total}")
                appendLine("当前页用户数: ${body.users.size}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("用户列表:")
                body.users.take(5).forEach { user ->
                    appendLine("  • ${user.name} (ID: ${user.userId})")
                    appendLine("    - Premium: ${if (user.premium) "是" else "否"}")
                    appendLine("    - 已关注: ${if (user.isFollowed) "是" else "否"}")
                    if (user.comment.isNotBlank()) {
                        val shortComment = if (user.comment.length > 50) 
                            user.comment.take(50) + "..." 
                        else 
                            user.comment
                        appendLine("    - 简介: $shortComment")
                    }
                }
                if (body.users.size > 5) {
                    appendLine("  ... 还有 ${body.users.size - 5} 位用户")
                }
                
                if (body.thumbnails.illust.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("缩略图插画数: ${body.thumbnails.illust.size}")
                    body.thumbnails.illust.take(3).forEach { illust ->
                        appendLine("  • ${illust.title} (ID: ${illust.id})")
                        appendLine("    作者: ${illust.userName} (${illust.userId})")
                    }
                }
                
                if (body.thumbnails.novel.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("缩略图小说数: ${body.thumbnails.novel.size}")
                    body.thumbnails.novel.take(3).forEach { novel ->
                        appendLine("  • ${novel.title} (ID: ${novel.id})")
                        appendLine("    作者: ${novel.userName} (${novel.userId})")
                        appendLine("    字数: ${novel.textCount}, 阅读时间: ${novel.readingTime}分")
                    }
                }
                
                if (body.page.workIds.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("用户作品信息:")
                    body.page.workIds.entries.take(3).forEach { (userId, works) ->
                        appendLine("  用户 $userId: ${works.size} 个作品")
                        works.take(2).forEach { work ->
                            appendLine("    - ${work.type}: ${work.id}")
                        }
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelDiscovery() {
        val mode = getParam("mode")
        val limit = getParam("limit").toIntOrNull() ?: 100
        
        val responseWithRaw = pixivApi.client.getWithRaw<DiscoveryBody>(
            "/ajax/discovery/novels",
            mapOf(
                "mode" to mode,
                "limit" to limit
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 发现小说成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: $mode")
            appendLine("限制: $limit")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelRecommendInit() {
        val novelId = getParam("novelId").toLongOrNull() ?: 26840082L
        val limit = getParam("limit").toIntOrNull() ?: 9
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelRecommendInitBody>(
            "/ajax/novel/$novelId/recommend/init",
            mapOf("limit" to limit)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说推荐初始化成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("基准小说ID: $novelId")
            appendLine("数量限制: $limit")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("返回小说数: ${body.novels.size}")
                appendLine("NextIds数量: ${body.nextIds.size}")
                if (body.novels.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说列表预览:")
                    body.novels.take(3).forEach { novel ->
                        appendLine("  • ${novel.title} (ID: ${novel.id})")
                        appendLine("    作者: ${novel.userName} (${novel.userId})")
                        appendLine("    字数: ${novel.textCount}, 阅读时间: ${novel.readingTime}秒")
                        appendLine("    收藏: ${novel.bookmarkCount ?: 0}, AI类型: ${novel.aiType}")
                    }
                    if (body.novels.size > 3) {
                        appendLine("  ... 还有 ${body.novels.size - 3} 部小说")
                    }
                }
                if (body.nextIds.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("NextIds: ${body.nextIds.take(5).joinToString(", ")}${if (body.nextIds.size > 5) "..." else ""}")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
            appendLine("提示: 使用 nextIds 调用 getRecommendNovels 获取后续推荐")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetRecommendNovels() {
        val novelIdsStr = getParam("novelIds")
        val novelIds = novelIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (novelIds.isEmpty()) {
            _state.update { current ->
                current.copy(
                    testResult = TestResult.Error("请提供有效的小说ID列表 (逗号分隔)")
                )
            }
            return
        }
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelRecommendBody>(
            "/ajax/novel/recommend/novels",
            mapOf("novelIds[]" to novelIds)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 推荐小说获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请求的小说ID数: ${novelIds.size}")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("返回小说数: ${body.novels.size}")
                if (body.novels.isNotEmpty()) {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("小说列表预览:")
                    body.novels.take(5).forEach { novel ->
                        appendLine("  • ${novel.title} (ID: ${novel.id})")
                        appendLine("    作者: ${novel.userName} (${novel.userId})")
                        appendLine("    字数: ${novel.textCount}, 阅读时间: ${novel.readingTime}秒")
                        appendLine("    收藏: ${novel.bookmarkCount ?: 0}, AI类型: ${novel.aiType}")
                    }
                    if (body.novels.size > 5) {
                        appendLine("  ... 还有 ${body.novels.size - 5} 部小说")
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整结果")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== FollowApi 测试方法 ====================
    
    private suspend fun testGetFollowLatestIllust() {
        val mode = getParam("mode")
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<FollowLatestBody>(
            "/ajax/follow_latest/illust",
            mapOf(
                "mode" to mode,
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 关注作者最新插画获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: $mode")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetFollowLatestNovel() {
        val mode = getParam("mode")
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<FollowLatestBody>(
            "/ajax/follow_latest/novel",
            mapOf(
                "mode" to mode,
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 关注作者最新小说获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("模式: $mode")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetWatchListManga() {
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.follow.WatchListMangaBody>(
            "/ajax/watch_list/manga",
            mapOf(
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 漫画追更列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("📊 统计信息:")
                appendLine("总数: ${body.page.total}")
                appendLine("最大页数: ${body.page.maxPage}")
                appendLine("追更系列数: ${body.page.watchedSeriesIds.size}")
                body.illustSeries?.let { series ->
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📚 系列列表 (${series.size}):")
                    series.take(5).forEach { s ->
                        appendLine("  • ${s.title} (ID: ${s.id})")
                        appendLine("    作品数: ${s.total}, 追更: ${s.isWatched}")
                    }
                    if (series.size > 5) {
                        appendLine("  ... 还有 ${series.size - 5} 个系列")
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetWatchListNovel() {
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<com.projectu.shared.data.remote.dto.follow.WatchListNovelBody>(
            "/ajax/watch_list/novel",
            mapOf(
                "p" to page
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说追更列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                appendLine("📊 统计信息:")
                appendLine("总数: ${body.page.total}")
                appendLine("最大页数: ${body.page.maxPage}")
                appendLine("追更系列数: ${body.page.watchedSeriesIds.size}")
                body.thumbnails?.novelSeries?.let { series ->
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📚 系列列表 (${series.size}):")
                    series.take(5).forEach { s ->
                        appendLine("  • ${s.title} (ID: ${s.id})")
                        appendLine("    章节数: ${s.episodeCount}, 追更: ${s.isWatched}")
                    }
                    if (series.size > 5) {
                        appendLine("  ... 还有 ${series.size - 5} 个系列")
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== MarkerApi 测试方法 ====================
    
    private suspend fun testAddNovelMarker() {
        val novelId = getParam("novelId").toLongOrNull() 
            ?: throw IllegalArgumentException("novelId 参数必须是数字")
        val userId = getParam("userId").toLongOrNull() 
            ?: throw IllegalArgumentException("userId 参数必须是数字")
        val page = getParam("page").toIntOrNull() ?: 1
        
        val result = pixivApi.markerApi.addNovelMarker(novelId, userId, page)
        
        val summary = buildString {
            appendLine("✅ 添加小说书签成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("用户ID: $userId")
            appendLine("页码: $page")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("结果: page=${result.page}")
            if (result.page == 1) {
                appendLine("状态: ✅ 已添加书签")
            } else {
                appendLine("状态: ⚠️ 书签添加失败或已存在")
            }
        }
        
        val rawJson = """{"page":${result.page}}"""
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testDeleteNovelMarker() {
        val novelId = getParam("novelId").toLongOrNull() 
            ?: throw IllegalArgumentException("novelId 参数必须是数字")
        val userId = getParam("userId").toLongOrNull() 
            ?: throw IllegalArgumentException("userId 参数必须是数字")
        
        val result = pixivApi.markerApi.deleteNovelMarker(novelId, userId)
        
        val summary = buildString {
            appendLine("✅ 删除小说书签成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("小说ID: $novelId")
            appendLine("用户ID: $userId")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("结果: page=${result.page}")
            if (result.page == 0) {
                appendLine("状态: ✅ 已删除书签")
            } else {
                appendLine("状态: ⚠️ 书签删除失败或不存在")
            }
        }
        
        val rawJson = """{"page":${result.page}}"""
        updateResultWithRaw(rawJson, summary)
    }
    
    private suspend fun testGetNovelMarkerList() {
        val result = pixivApi.markerApi.getNovelMarkerList()
        
        val summary = buildString {
            appendLine("✅ 获取小说书签列表成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("书签总数: ${result.total} 件")
            appendLine("实际解析: ${result.novels.size} 件")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            
            if (result.novels.isNotEmpty()) {
                appendLine("小说列表:")
                result.novels.forEachIndexed { index, novel ->
                    appendLine("")
                    appendLine("【${index + 1}】 ${novel.title}")
                    appendLine("  · ID: ${novel.id}")
                    appendLine("  · 作者: ${novel.userName} (${novel.userId})")
                    if (novel.coverUrl != null) {
                        appendLine("  · 封面: ${novel.coverUrl}")
                    }
                    if (novel.seriesId != null) {
                        appendLine("  · 📚 系列: ${novel.seriesTitle ?: "未知系列"} (ID: ${novel.seriesId})")
                    }
                    appendLine("  · 字数: ${novel.textCount} 字符")
                    appendLine("  · 收藏: ${novel.bookmarkCount}")
                    if (novel.tags.isNotEmpty()) {
                        appendLine("  · 标签: ${novel.tags.take(5).joinToString(", ")}")
                    }
                    if (novel.description.isNotBlank()) {
                        val desc = if (novel.description.length > 50) {
                            novel.description.take(50) + "..."
                        } else {
                            novel.description
                        }
                        appendLine("  · 简介: $desc")
                    }
                    if (novel.xRestrict > 0) {
                        appendLine("  · 🔞 R-18 作品")
                    }
                }
            } else {
                appendLine("⚠️ 暂无书签")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("提示：查看 JSON 标签页获取完整数据结构")
        }
        
        // 手动构建 JSON
        val rawJson = buildString {
            appendLine("{")
            appendLine("""  "total": ${result.total},""")
            appendLine("""  "novels": [""")
            result.novels.forEachIndexed { index, novel ->
                appendLine("    {")
                appendLine("""      "id": "${novel.id}",""")
                appendLine("""      "title": "${novel.title.replace("\"", "\\\"")}",""")
                appendLine("""      "userId": "${novel.userId}",""")
                appendLine("""      "userName": "${novel.userName.replace("\"", "\\\"")}",""")
                if (novel.coverUrl != null) {
                    appendLine("""      "coverUrl": "${novel.coverUrl}",""")
                }
                if (novel.seriesId != null) {
                    appendLine("""      "seriesId": "${novel.seriesId}",""")
                    appendLine("""      "seriesTitle": "${novel.seriesTitle?.replace("\"", "\\\"") ?: ""}",""")
                }
                appendLine("""      "textCount": ${novel.textCount},""")
                appendLine("""      "bookmarkCount": ${novel.bookmarkCount},""")
                appendLine("""      "xRestrict": ${novel.xRestrict},""")
                appendLine("""      "tags": [${novel.tags.joinToString { "\"$it\"" }}],""")
                appendLine("""      "description": "${novel.description.replace("\"", "\\\"").take(100)}" """)
                append("    }")
                if (index < result.novels.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ]")
            append("}")
        }
        updateResultWithRaw(rawJson, summary)
    }
    
    // ==================== IllustSeriesApi 测试方法 ====================
    
    private suspend fun testGetIllustSeriesDetail() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 313864L
        val page = getParam("page").toIntOrNull() ?: 1
        
        val responseWithRaw = pixivApi.client.getWithRaw<IllustSeriesBody>(
            "/ajax/series/$seriesId",
            mapOf("p" to page)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 漫画系列详情获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("系列ID: $seriesId")
            appendLine("页码: $page")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { body ->
                // 系列信息
                body.illustSeries.firstOrNull()?.let { series ->
                    appendLine("系列标题: ${series.title}")
                    appendLine("作者ID: ${series.userId}")
                    appendLine("作品总数: ${series.total}")
                    appendLine("描述: ${series.description.take(100)}...")
                    appendLine("是否已追更: ${series.isWatched}")
                    appendLine("创建时间: ${series.createDate}")
                    appendLine("更新时间: ${series.updateDate}")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                }
                
                // 分页信息
                body.page?.let { page ->
                    appendLine("当前页作品数: ${page.series.size}")
                    appendLine("总作品数: ${page.total}")
                    appendLine("是否已追更: ${page.isWatched}")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                }
                
                // 用户信息
                body.users.firstOrNull()?.let { user ->
                    appendLine("作者: ${user.name}")
                    appendLine("作者ID: ${user.userId}")
                    appendLine("是否已关注: ${user.isFollowed}")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━")
                }
                
                // 缩略图
                body.thumbnails?.let { thumbnails ->
                    appendLine("缩略图数量: ${thumbnails.illust.size}")
                }
            }
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testWatchIllustSeries() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 313864L
        
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, String>>(
            "/ajax/illust/series/$seriesId/watch",
            emptyMap()
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            if (response.error) {
                appendLine("❌ 追更漫画系列失败")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
            } else {
                appendLine("✅ 追更漫画系列成功")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("已成功添加到漫画追更列表")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整响应")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testUnwatchIllustSeries() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 313864L
        
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, String>>(
            "/ajax/illust/series/$seriesId/unwatch",
            emptyMap()
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            if (response.error) {
                appendLine("❌ 取消追更漫画系列失败")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
            } else {
                appendLine("✅ 取消追更漫画系列成功")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("已成功从漫画追更列表移除")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整响应")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== NovelSeriesApi 测试方法 ====================
    
    private suspend fun testGetNovelSeriesDetail() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 8174474L
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelSeriesBody>(
            "/ajax/novel/series/$seriesId"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 小说系列详情获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("系列ID: $seriesId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelSeriesContents() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 8174474L
        val limit = getParam("limit").toIntOrNull() ?: 30
        val orderBy = getParam("orderBy")
        
        val responseWithRaw = pixivApi.client.getWithRaw<NovelSeriesContentBody>(
            "/ajax/novel/series_content/$seriesId",
            mapOf(
                "limit" to limit,
                "order_by" to orderBy
            )
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 系列内容列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("系列ID: $seriesId")
            appendLine("数量: $limit")
            appendLine("排序: $orderBy")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetNovelSeriesTitles() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 8174474L
        
        val responseWithRaw = pixivApi.client.getWithRaw<List<NovelSeriesTitle>>(
            "/ajax/novel/series/$seriesId/content_titles"
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 系列标题列表获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("系列ID: $seriesId")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            response.body?.let { titles ->
                appendLine("标题数量: ${titles.size}")
                appendLine("\n标题列表:")
                titles.forEachIndexed { index, title ->
                    appendLine("${index + 1}. [${title.id}] ${title.title} (可用: ${title.available})")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testWatchNovelSeries() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 8174474L
        
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, String>>(
            "/ajax/novel/series/$seriesId/watch",
            emptyMap()
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            if (response.error) {
                appendLine("❌ 追更系列失败")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
            } else {
                appendLine("✅ 追更系列成功")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("已成功添加到追更列表")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整响应")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testUnwatchNovelSeries() {
        val seriesId = getParam("seriesId").toLongOrNull() ?: 8174474L
        
        val responseWithRaw = pixivApi.client.postJsonWithRaw<List<String>, Map<String, String>>(
            "/ajax/novel/series/$seriesId/unwatch",
            emptyMap()
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            if (response.error) {
                appendLine("❌ 取消追更失败")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
            } else {
                appendLine("✅ 取消追更成功")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("系列ID: $seriesId")
                appendLine("错误: ${response.error}")
                appendLine("消息: ${response.message}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━")
                appendLine("已成功从追更列表移除")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("请查看 JSON 标签页查看完整响应")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    // ==================== TagApi 测试方法 ====================
    
    private suspend fun testGetTagSuggest() {
        val keyword = getParam("keyword")
        
        val responseWithRaw = pixivApi.client.getWithRaw<TagSuggestBody>(
            "/ajax/tags/suggest_by_word",
            mapOf("word" to keyword)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 标签搜索建议获取成功 (Ajax)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("关键词: $keyword")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetSearchSuggestion() {
        val mode = getParam("mode").ifEmpty { "all" }
        
        val responseWithRaw = pixivApi.client.getWithRaw<SearchSuggestionBody>(
            "/ajax/search/suggestion",
            mapOf("mode" to mode)
        )
        val response = responseWithRaw.response
        val body = response.body ?: error("响应体为空")
        
        val summary = buildString {
            appendLine("✅ 搜索建议获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("搜索模式: $mode")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            
            // 热门标签 (插画)
            body.popularTags?.illust?.let { illustTags ->
                if (illustTags.isNotEmpty()) {
                    appendLine()
                    appendLine("【热门标签 - 插画】(${illustTags.size} 个)")
                    illustTags.take(10).forEach { popularTag ->
                        val translation = body.tagTranslation[popularTag.tag]?.zh?.takeIf { it.isNotEmpty() }
                            ?: body.tagTranslation[popularTag.tag]?.zhTw?.takeIf { it.isNotEmpty() }
                        val displayName = if (translation != null) {
                            "${popularTag.tag} ($translation)"
                        } else {
                            popularTag.tag
                        }
                        appendLine("  • $displayName - ${popularTag.ids.size} 作品")
                    }
                }
            }
            
            // 热门标签 (小说)
            body.popularTags?.novel?.let { novelTags ->
                if (novelTags.isNotEmpty()) {
                    appendLine()
                    appendLine("【热门标签 - 小说】(${novelTags.size} 个)")
                    novelTags.take(5).forEach { popularTag ->
                        val translation = body.tagTranslation[popularTag.tag]?.zh?.takeIf { it.isNotEmpty() }
                            ?: body.tagTranslation[popularTag.tag]?.zhTw?.takeIf { it.isNotEmpty() }
                        val displayName = if (translation != null) {
                            "${popularTag.tag} ($translation)"
                        } else {
                            popularTag.tag
                        }
                        appendLine("  • $displayName - ${popularTag.ids.size} 作品")
                    }
                }
            }
            
            // 推荐标签
            body.recommendTags?.illust?.let { recommendTags ->
                if (recommendTags.isNotEmpty()) {
                    appendLine()
                    appendLine("【推荐标签】(${recommendTags.size} 个)")
                    recommendTags.forEach { popularTag ->
                        val translation = body.tagTranslation[popularTag.tag]?.zh?.takeIf { it.isNotEmpty() }
                            ?: body.tagTranslation[popularTag.tag]?.zhTw?.takeIf { it.isNotEmpty() }
                        val displayName = if (translation != null) {
                            "${popularTag.tag} ($translation)"
                        } else {
                            popularTag.tag
                        }
                        appendLine("  • $displayName")
                    }
                }
            }
            
            // 基于标签推荐
            body.recommendByTags?.illust?.let { recommendByTags ->
                if (recommendByTags.isNotEmpty()) {
                    appendLine()
                    appendLine("【基于标签推荐】(${recommendByTags.size} 个)")
                    recommendByTags.take(5).forEach { popularTag ->
                        val translation = body.tagTranslation[popularTag.tag]?.zh?.takeIf { it.isNotEmpty() }
                            ?: body.tagTranslation[popularTag.tag]?.zhTw?.takeIf { it.isNotEmpty() }
                        val displayName = if (translation != null) {
                            "${popularTag.tag} ($translation)"
                        } else {
                            popularTag.tag
                        }
                        appendLine("  • $displayName - ${popularTag.ids.size} 作品")
                    }
                }
            }
            
            // 我的收藏标签
            if (body.myFavoriteTags.isNotEmpty()) {
                appendLine()
                appendLine("【我的收藏标签】(${body.myFavoriteTags.size} 个)")
                body.myFavoriteTags.forEach { favTag ->
                    val translation = body.tagTranslation[favTag]?.zh?.takeIf { it.isNotEmpty() }
                        ?: body.tagTranslation[favTag]?.zhTw?.takeIf { it.isNotEmpty() }
                    val displayName = if (translation != null) {
                        "$favTag ($translation)"
                    } else {
                        favTag
                    }
                    appendLine("  • $displayName")
                }
            }
            
            // 缩略图信息
            appendLine()
            appendLine("【缩略图预览】(${body.thumbnails.size} 张)")
            body.thumbnails.take(3).forEach { thumbnail ->
                appendLine("  • [${thumbnail.id}] ${thumbnail.title}")
                appendLine("    作者: ${thumbnail.userName}")
                appendLine("    标签: ${thumbnail.tags.take(3).joinToString(", ")}")
                appendLine()
            }
        }
        
        updateResultWithRaw(responseWithRaw.rawJson, summary)
    }
    
    private suspend fun testGetTagSearchSuggest() {
        val keyword = getParam("keyword")
        
        // 使用 getRawWithJson 获取原始响应
        val responseWithJson = pixivApi.client.getRawWithJson<TagSearchSuggestBody>(
            "/rpc/cps.php",
            mapOf("keyword" to keyword)
        )
        
        val response = responseWithJson.data
        
        val summary = buildString {
            appendLine("✅ 标签搜索建议获取成功 (RPC)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("关键词: $keyword")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("候选标签数量: ${response.candidates.size}")
            appendLine()
            response.candidates.take(5).forEachIndexed { index, candidate ->
                appendLine("【${index + 1}】 ${candidate.tagName}")
                appendLine("    访问数: ${candidate.accessCount}")
                appendLine("    类型: ${candidate.type}")
                candidate.tagTranslation?.let {
                    appendLine("    翻译: $it")
                }
                appendLine()
            }
            if (response.candidates.size > 5) {
                appendLine("... 还有 ${response.candidates.size - 5} 个候选标签")
            }
        }
        
        updateResultWithRaw(responseWithJson.rawJson, summary)
    }
    
    private suspend fun testGetTagInfo() {
        val tag = getParam("tag")
        
        val responseWithRaw = pixivApi.client.getWithRaw<TagInfoBody>(
            "/ajax/tag/info",
            mapOf("tag" to tag)
        )
        val response = responseWithRaw.response
        
        val summary = buildString {
            appendLine("✅ 标签信息获取成功")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("标签: $tag")
            appendLine("错误: ${response.error}")
            appendLine("消息: ${response.message}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("响应体类型: ${response.body?.let { it::class.simpleName }}")
            appendLine("请查看 JSON 标签页查看完整数据")
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
