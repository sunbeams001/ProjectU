package com.projectu.ui.screens.apitest

/**
 * API 测试模块枚举
 */
enum class ApiModule(val displayName: String) {
    ILLUST("插画 API (IllustApi)"),
    USER("用户 API (UserApi)"),
    BOOKMARK("收藏 API (BookmarkApi)"),
    RANKING("排行榜 API (RankingApi)"),
    COMMENT("评论 API (CommentApi)"),
    NOVEL("小说 API (NovelApi)"),
    NOVEL_SERIES("小说系列 API (NovelSeriesApi)"),
    TAG("标签 API (TagApi)"),
    MARKER("书签 API (MarkerApi)")
}

/**
 * API 测试方法定义
 */
sealed class ApiMethod(
    val module: ApiModule,
    val methodName: String,
    val displayName: String,
    val parameters: List<ApiParameter>,
    val priority: Int // 0=P0, 1=P1, 2=P2
) {
    // ==================== IllustApi ====================
    
    object GetIllustDetail : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getIllustDetail",
        displayName = "获取作品详情",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "102814610", required = true)
        ),
        priority = 0
    )
    
    object SearchIllust : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "searchIllust",
        displayName = "搜索作品",
        parameters = listOf(
            ApiParameter("keyword", "关键词", "初音ミク", required = true),
            ApiParameter("mode", "模式", "safe", required = false, 
                options = listOf("safe", "r18")),
            ApiParameter("order", "排序", "date_d", required = false,
                options = listOf("date_d", "date_asc", "popular_d")),
            ApiParameter("sMode", "搜索模式", "s_tag", required = false,
                options = listOf("s_tag", "s_tc")),
            ApiParameter("type", "类型", "all", required = false,
                options = listOf("all", "illust", "manga", "ugoira")),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 0
    )
    
    object GetRecommendInit : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getRecommendInit",
        displayName = "推荐初始化",
        parameters = listOf(
            ApiParameter("pid", "基准作品ID", "102814610", required = true),
            ApiParameter("limit", "数量", "18", required = false)
        ),
        priority = 1
    )
    
    object GetRecommendIllusts : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getRecommendIllusts",
        displayName = "获取推荐作品",
        parameters = listOf(
            ApiParameter("illustIds", "作品ID列表(逗号分隔)", "", required = true)
        ),
        priority = 1
    )
    
    object GetDiscoveryIllust : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getDiscoveryIllust",
        displayName = "发现作品",
        parameters = listOf(
            ApiParameter("mode", "模式", "all", required = false,
                options = listOf("all", "safe", "r18"))
        ),
        priority = 1
    )
    
    object GetUgoiraMetadata : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getUgoiraMetadata",
        displayName = "动图元数据",
        parameters = listOf(
            ApiParameter("illustId", "动图ID", "44298467", required = true)
        ),
        priority = 2
    )
    
    object GetIllustPages : ApiMethod(
        module = ApiModule.ILLUST,
        methodName = "getIllustPages",
        displayName = "获取多页作品详情",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "137776727", required = true)
        ),
        priority = 0
    )
    
    // ==================== UserApi ====================
    
    object GetUserInfo : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserInfo",
        displayName = "获取用户信息",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true)
        ),
        priority = 0
    )
    
    object GetUserFullInfo : ApiMethod(
        module = ApiModule.USER,
        methodName = "getProfileAll",
        displayName = "用户作品概况",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true)
        ),
        priority = 0
    )
    
    object GetUserIllusts : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserIllusts",
        displayName = "用户作品列表",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("limit", "数量", "20", required = false)
        ),
        priority = 1
    )
    
    object GetUserBookmarks : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserBookmarks",
        displayName = "用户收藏",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("tag", "标签", "", required = false),
            ApiParameter("rest", "公开性", "show", required = false,
                options = listOf("show", "hide")),
            ApiParameter("offset", "偏移", "0", required = false),
            ApiParameter("limit", "数量", "48", required = false)
        ),
        priority = 1
    )
    
    object GetUserFollowing : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserFollowing",
        displayName = "关注列表",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("offset", "偏移", "0", required = false),
            ApiParameter("limit", "数量", "24", required = false)
        ),
        priority = 2
    )
    
    object GetUserFollowers : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserFollowers",
        displayName = "粉丝列表",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("offset", "偏移", "0", required = false),
            ApiParameter("limit", "数量", "24", required = false)
        ),
        priority = 2
    )
    
    object GetRecommendUsers : ApiMethod(
        module = ApiModule.USER,
        methodName = "getRecommendUsers",
        displayName = "推荐用户(针对特定用户)",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("userNum", "推荐数量", "20", required = false),
            ApiParameter("workNum", "作品数量", "3", required = false),
            ApiParameter("isR18", "包含R18", "false", required = false,
                options = listOf("true", "false"))
        ),
        priority = 2
    )
    
    object GetDiscoveryUsers : ApiMethod(
        module = ApiModule.USER,
        methodName = "getDiscoveryUsers",
        displayName = "发现用户(总体推荐)",
        parameters = listOf(
            ApiParameter("limit", "推荐数量", "20", required = false)
        ),
        priority = 1
    )
    
    object FollowUser : ApiMethod(
        module = ApiModule.USER,
        methodName = "followUser",
        displayName = "关注用户 ⚠️",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true),
            ApiParameter("tag", "标签", "", required = false),
            ApiParameter("restrict", "公开性", "0", required = false,
                options = listOf("0", "1"))
        ),
        priority = 3
    )
    
    object UnfollowUser : ApiMethod(
        module = ApiModule.USER,
        methodName = "unfollowUser",
        displayName = "取消关注 ⚠️",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true)
        ),
        priority = 3
    )
    
    object GetUserFollowDetail : ApiMethod(
        module = ApiModule.USER,
        methodName = "getUserFollowDetail",
        displayName = "用户关注详情（公开/悄悄关注）",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "58277", required = true)
        ),
        priority = 1
    )
    
    // ==================== BookmarkApi ====================
    
    // 插画收藏
    object AddBookmark : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "addBookmark",
        displayName = "添加插画收藏 ⚠️",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "102814610", required = true),
            ApiParameter("restrict", "公开性", "0", required = false,
                options = listOf("0", "1")),
            ApiParameter("comment", "评论", "", required = false),
            ApiParameter("tags", "标签", "", required = false)
        ),
        priority = 0
    )
    
    object DeleteBookmark : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "deleteBookmark",
        displayName = "删除插画收藏 ⚠️",
        parameters = listOf(
            ApiParameter("bookmarkId", "收藏ID", "", required = true)
        ),
        priority = 0
    )
    
    object DeleteBookmarks : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "deleteBookmarks",
        displayName = "批量删除插画收藏 ⚠️",
        parameters = listOf(
            ApiParameter("bookmarkIds", "收藏ID列表(逗号分隔)", "", required = true)
        ),
        priority = 1
    )
    
    object GetIllustBookmarkTags : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "getIllustBookmarkTags",
        displayName = "插画收藏标签",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true)
        ),
        priority = 2
    )
    
    // 小说收藏
    object AddNovelBookmark : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "addNovelBookmark",
        displayName = "添加小说收藏 ⚠️",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "", required = true),
            ApiParameter("restrict", "公开性", "0", required = false,
                options = listOf("0", "1")),
            ApiParameter("comment", "评论", "", required = false),
            ApiParameter("tags", "标签", "", required = false)
        ),
        priority = 1
    )
    
    object DeleteNovelBookmark : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "deleteNovelBookmark",
        displayName = "删除小说收藏 ⚠️",
        parameters = listOf(
            ApiParameter("bookId", "收藏ID", "", required = true)
        ),
        priority = 1
    )
    
    object DeleteNovelBookmarks : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "deleteNovelBookmarks",
        displayName = "批量删除小说收藏 ⚠️",
        parameters = listOf(
            ApiParameter("bookmarkIds", "收藏ID列表(逗号分隔)", "", required = true)
        ),
        priority = 2
    )
    
    object GetNovelBookmarkTags : ApiMethod(
        module = ApiModule.BOOKMARK,
        methodName = "getNovelBookmarkTags",
        displayName = "小说收藏标签",
        parameters = listOf(
            ApiParameter("userId", "用户ID", "11", required = true)
        ),
        priority = 2
    )
    
    // ==================== RankingApi ====================
    
    object GetIllustRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getIllustRanking",
        displayName = "获取插画排行榜",
        parameters = listOf(
            ApiParameter("mode", "排行榜模式", "daily", required = true,
                options = listOf(
                    // 一般排行榜
                    "daily",      // 今日
                    "weekly",     // 本周
                    "monthly",    // 本月
                    "rookie",     // 新人
                    "original",   // 原创
                    "daily_ai",   // AI生成
                    "male",       // 男性向
                    "female",     // 女性向
                    // R-18 排行榜
                    "daily_r18",     // 今日R-18
                    "weekly_r18",    // 本周R-18
                    "daily_r18_ai",  // AI生成R-18
                    "male_r18",      // 男性向R-18
                    "female_r18",    // 女性向R-18
                    // R-18G 排行榜
                    "r18g"           // R-18G（猎奇向）
                )
            ),
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("content", "内容类型", "all", required = false,
                options = listOf("all", "illust", "manga", "ugoira")
            ),
            ApiParameter("date", "日期(yyyyMMdd)", "", required = false)
        ),
        priority = 0
    )
    
    object GetNovelRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getNovelRanking",
        displayName = "获取小说排行榜(JSON)",
        parameters = listOf(
            ApiParameter("mode", "排行榜模式", "daily", required = true,
                options = listOf(
                    // 一般排行榜
                    "daily",      // 今日
                    "weekly",     // 本周
                    "monthly",    // 本月
                    "rookie",     // 新人
                    "male",       // 男性向
                    "female",     // 女性向
                    // 小说专属排行榜
                    "weekly_original",  // 本周原创（小说专属）
                    "weekly_ai",        // 本周AI（小说专属）
                    // R-18 排行榜
                    "daily_r18",     // 今日R-18
                    "weekly_r18",    // 本周R-18
                    "male_r18",      // 男性向R-18
                    "female_r18",    // 女性向R-18
                    "weekly_r18_ai", // 本周R-18 AI（小说专属）
                    // R-18G 排行榜
                    "r18g"           // R-18G（猎奇向）
                )
            ),
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("content", "内容类型", "novel", required = false,
                options = listOf("novel")
            ),
            ApiParameter("date", "日期(yyyyMMdd)", "", required = false)
        ),
        priority = 1
    )
    
    // ==================== CommentApi ====================
    
    object GetIllustCommentRoots : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "getIllustCommentRoots",
        displayName = "获取插画评论",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "102814610", required = true),
            ApiParameter("offset", "偏移", "0", required = false),
            ApiParameter("limit", "数量", "20", required = false)
        ),
        priority = 1
    )
    
    object GetCommentReplies : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "getCommentReplies",
        displayName = "获取评论回复",
        parameters = listOf(
            ApiParameter("commentId", "评论ID", "", required = true),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 2
    )
    
    object PostIllustComment : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "postIllustComment",
        displayName = "发布插画评论",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "102814610", required = true),
            ApiParameter("userId", "用户ID", "", required = true),
            ApiParameter("comment", "评论内容", "", required = false),
            ApiParameter("stampId", "表情ID", "", required = false),
            ApiParameter("parentCommentId", "父评论ID", "", required = false)
        ),
        priority = 3
    )
    
    object DeleteIllustComment : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "deleteIllustComment",
        displayName = "删除插画评论",
        parameters = listOf(
            ApiParameter("illustId", "作品ID", "102814610", required = true),
            ApiParameter("commentId", "评论ID", "", required = true)
        ),
        priority = 4
    )
    
    object GetNovelCommentRoots : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "getNovelCommentRoots",
        displayName = "获取小说评论",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true),
            ApiParameter("offset", "偏移", "0", required = false),
            ApiParameter("limit", "数量", "20", required = false)
        ),
        priority = 5
    )
    
    object GetNovelCommentReplies : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "getNovelCommentReplies",
        displayName = "获取小说评论回复",
        parameters = listOf(
            ApiParameter("commentId", "评论ID", "50155161", required = true),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 6
    )
    
    object PostNovelComment : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "postNovelComment",
        displayName = "发布小说评论",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true),
            ApiParameter("userId", "用户ID", "", required = true),
            ApiParameter("comment", "评论内容", "", required = false),
            ApiParameter("stampId", "表情ID", "", required = false),
            ApiParameter("parentCommentId", "父评论ID", "", required = false)
        ),
        priority = 7
    )
    
    object DeleteNovelComment : ApiMethod(
        module = ApiModule.COMMENT,
        methodName = "deleteNovelComment",
        displayName = "删除小说评论",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true),
            ApiParameter("commentId", "评论ID", "", required = true)
        ),
        priority = 8
    )
    
    // ==================== NovelApi ====================
    
    object GetNovelDetail : ApiMethod(
        module = ApiModule.NOVEL,
        methodName = "getNovelDetail",
        displayName = "获取小说详情",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true)
        ),
        priority = 0
    )
    
    object GetNovelBookmarkData : ApiMethod(
        module = ApiModule.NOVEL,
        methodName = "getNovelBookmarkData",
        displayName = "小说收藏状态",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true)
        ),
        priority = 1
    )
    
    object SearchNovel : ApiMethod(
        module = ApiModule.NOVEL,
        methodName = "searchNovel",
        displayName = "搜索小说",
        parameters = listOf(
            ApiParameter("keyword", "关键词", "初音ミク", required = true),
            ApiParameter("searchMode", "搜索模式", "s_tag", required = false,
                options = listOf("s_tag", "s_tag_full", "s_tc")),
            ApiParameter("order", "排序", "date_d", required = false,
                options = listOf("date_d", "date")),
            ApiParameter("mode", "模式", "all", required = false,
                options = listOf("all", "safe", "r18")),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 0
    )
    
    object GetNovelDiscovery : ApiMethod(
        module = ApiModule.NOVEL,
        methodName = "getNovelDiscovery",
        displayName = "发现小说",
        parameters = listOf(
            ApiParameter("mode", "模式", "all", required = false,
                options = listOf("all", "safe", "r18")),
            ApiParameter("limit", "数量", "100", required = false)
        ),
        priority = 1
    )
    
    object GetNovelFollowLatest : ApiMethod(
        module = ApiModule.NOVEL,
        methodName = "getNovelFollowLatest",
        displayName = "关注作者最新小说",
        parameters = listOf(
            ApiParameter("mode", "模式", "all", required = false,
                options = listOf("all", "r18")),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 2
    )
    
    // ==================== NovelSeriesApi ====================
    
    object GetNovelSeriesDetail : ApiMethod(
        module = ApiModule.NOVEL_SERIES,
        methodName = "getNovelSeriesDetail",
        displayName = "小说系列详情",
        parameters = listOf(
            ApiParameter("seriesId", "系列ID", "8174474", required = true)
        ),
        priority = 1
    )
    
    object GetNovelSeriesContents : ApiMethod(
        module = ApiModule.NOVEL_SERIES,
        methodName = "getNovelSeriesContents",
        displayName = "系列内容列表",
        parameters = listOf(
            ApiParameter("seriesId", "系列ID", "8174474", required = true),
            ApiParameter("limit", "数量", "30", required = false),
            ApiParameter("orderBy", "排序", "asc", required = false,
                options = listOf("asc", "desc"))
        ),
        priority = 1
    )
    
    object GetNovelSeriesTitles : ApiMethod(
        module = ApiModule.NOVEL_SERIES,
        methodName = "getNovelSeriesTitles",
        displayName = "系列标题列表",
        parameters = listOf(
            ApiParameter("seriesId", "系列ID", "8174474", required = true)
        ),
        priority = 2
    )
    
    object WatchNovelSeries : ApiMethod(
        module = ApiModule.NOVEL_SERIES,
        methodName = "watch",
        displayName = "追更系列",
        parameters = listOf(
            ApiParameter("seriesId", "系列ID", "8174474", required = true)
        ),
        priority = 3
    )
    
    object UnwatchNovelSeries : ApiMethod(
        module = ApiModule.NOVEL_SERIES,
        methodName = "unwatch",
        displayName = "取消追更",
        parameters = listOf(
            ApiParameter("seriesId", "系列ID", "8174474", required = true)
        ),
        priority = 3
    )
    
    // ==================== TagApi ====================
    
    object GetTagSuggest : ApiMethod(
        module = ApiModule.TAG,
        methodName = "getTagSuggest",
        displayName = "标签搜索建议 (Ajax)",
        parameters = listOf(
            ApiParameter("keyword", "关键词", "RO635", required = true)
        ),
        priority = 1
    )
    
    object GetSearchSuggestion : ApiMethod(
        module = ApiModule.TAG,
        methodName = "getSearchSuggestion",
        displayName = "搜索建议（点击搜索框）",
        parameters = listOf(
            ApiParameter("mode", "模式", "all", required = false, options = listOf("all", "r18"))
        ),
        priority = 1
    )
    
    object GetTagSearchSuggest : ApiMethod(
        module = ApiModule.TAG,
        methodName = "getTagSearchSuggest",
        displayName = "标签搜索建议 (RPC)",
        parameters = listOf(
            ApiParameter("keyword", "关键词", "RO635", required = true)
        ),
        priority = 1
    )
    
    object GetTagInfo : ApiMethod(
        module = ApiModule.TAG,
        methodName = "getTagInfo",
        displayName = "标签信息",
        parameters = listOf(
            ApiParameter("tag", "标签名", "初音ミク", required = true)
        ),
        priority = 2
    )
    
    // ==================== MarkerApi ====================
    
    object AddNovelMarker : ApiMethod(
        module = ApiModule.MARKER,
        methodName = "addNovelMarker",
        displayName = "添加小说书签（稍后再读）",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true),
            ApiParameter("userId", "用户ID", "4966721", required = true),
            ApiParameter("page", "页码", "1", required = false)
        ),
        priority = 1
    )
    
    object DeleteNovelMarker : ApiMethod(
        module = ApiModule.MARKER,
        methodName = "deleteNovelMarker",
        displayName = "删除小说书签（取消稍后再读）",
        parameters = listOf(
            ApiParameter("novelId", "小说ID", "15809265", required = true),
            ApiParameter("userId", "用户ID", "4966721", required = true)
        ),
        priority = 1
    )
    
    object GetNovelMarkerList : ApiMethod(
        module = ApiModule.MARKER,
        methodName = "getNovelMarkerList",
        displayName = "获取小说书签列表",
        parameters = emptyList(),
        priority = 2
    )
    
    companion object {
        /**
         * 获取所有 API 方法
         */
        fun getAllMethods(): List<ApiMethod> = listOf(
            // IllustApi
            GetIllustDetail, SearchIllust, GetRecommendInit, 
            GetRecommendIllusts, GetDiscoveryIllust, GetUgoiraMetadata, GetIllustPages,
            // UserApi
            GetUserInfo, GetUserFullInfo, GetUserIllusts, 
            GetUserBookmarks, GetUserFollowing, GetUserFollowers,
            GetRecommendUsers, GetDiscoveryUsers, FollowUser, UnfollowUser,
            GetUserFollowDetail,
            // BookmarkApi
            AddBookmark, DeleteBookmark, DeleteBookmarks, GetIllustBookmarkTags,
            AddNovelBookmark, DeleteNovelBookmark, DeleteNovelBookmarks, GetNovelBookmarkTags,
            // RankingApi
            GetIllustRanking, GetNovelRanking,
            // CommentApi
            GetIllustCommentRoots, GetCommentReplies, PostIllustComment, DeleteIllustComment,
            GetNovelCommentRoots, GetNovelCommentReplies, PostNovelComment, DeleteNovelComment,
            // NovelApi
            GetNovelDetail, GetNovelBookmarkData, SearchNovel, 
            GetNovelDiscovery, GetNovelFollowLatest,
            // NovelSeriesApi
            GetNovelSeriesDetail, GetNovelSeriesContents, GetNovelSeriesTitles,
            WatchNovelSeries, UnwatchNovelSeries,
            // TagApi
            GetTagSuggest, GetSearchSuggestion, GetTagSearchSuggest, GetTagInfo,
            // MarkerApi
            AddNovelMarker, DeleteNovelMarker, GetNovelMarkerList
        )
        
        /**
         * 根据模块获取方法列表
         */
        fun getMethodsByModule(module: ApiModule): List<ApiMethod> =
            getAllMethods().filter { it.module == module }
    }
}

/**
 * API 参数定义
 */
data class ApiParameter(
    val name: String,
    val displayName: String,
    val defaultValue: String,
    val required: Boolean,
    val options: List<String>? = null
)

/**
 * 测试结果
 */
sealed class TestResult {
    data object Idle : TestResult()
    data object Loading : TestResult()
    data class Success(
        val rawJson: String,
        val summary: String,
        val duration: Long // 毫秒
    ) : TestResult()
    data class Error(
        val message: String,
        val stackTrace: String? = null
    ) : TestResult()
}
