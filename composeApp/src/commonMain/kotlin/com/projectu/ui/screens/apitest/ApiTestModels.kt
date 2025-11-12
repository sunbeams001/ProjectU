package com.projectu.ui.screens.apitest

/**
 * API 测试模块枚举
 */
enum class ApiModule(val displayName: String) {
    ILLUST("插画 API (IllustApi)"),
    USER("用户 API (UserApi)"),
    BOOKMARK("收藏 API (BookmarkApi)"),
    RANKING("排行榜 API (RankingApi)")
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
        methodName = "getUserFullInfo",
        displayName = "完整用户信息",
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
    
    object GetDailyRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getDailyRanking",
        displayName = "日榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 0
    )
    
    object GetWeeklyRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getWeeklyRanking",
        displayName = "周榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 1
    )
    
    object GetMonthlyRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getMonthlyRanking",
        displayName = "月榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 1
    )
    
    object GetRookieRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getRookieRanking",
        displayName = "新人榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 2
    )
    
    object GetOriginalRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getOriginalRanking",
        displayName = "原创榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 2
    )
    
    object GetMaleRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getMaleRanking",
        displayName = "男性向",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 2
    )
    
    object GetFemaleRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getFemaleRanking",
        displayName = "女性向",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 2
    )
    
    object GetR18DailyRanking : ApiMethod(
        module = ApiModule.RANKING,
        methodName = "getR18DailyRanking",
        displayName = "R18 日榜",
        parameters = listOf(
            ApiParameter("page", "页码", "1", required = false),
            ApiParameter("date", "日期", "", required = false)
        ),
        priority = 2
    )
    
    companion object {
        /**
         * 获取所有 API 方法
         */
        fun getAllMethods(): List<ApiMethod> = listOf(
            // IllustApi
            GetIllustDetail, SearchIllust, GetRecommendInit, 
            GetRecommendIllusts, GetDiscoveryIllust, GetUgoiraMetadata,
            // UserApi
            GetUserInfo, GetUserFullInfo, GetUserIllusts, 
            GetUserBookmarks, GetUserFollowing, GetUserFollowers,
            // BookmarkApi
            AddBookmark, DeleteBookmark, DeleteBookmarks, GetIllustBookmarkTags,
            AddNovelBookmark, DeleteNovelBookmark, DeleteNovelBookmarks, GetNovelBookmarkTags,
            // RankingApi
            GetDailyRanking, GetWeeklyRanking, GetMonthlyRanking,
            GetRookieRanking, GetOriginalRanking, GetMaleRanking,
            GetFemaleRanking, GetR18DailyRanking
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
