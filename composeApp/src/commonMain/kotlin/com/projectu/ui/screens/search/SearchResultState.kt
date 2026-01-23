package com.projectu.ui.screens.search

import androidx.compose.ui.text.input.TextFieldValue
import com.projectu.shared.data.remote.model.IllustSearchMode
import com.projectu.shared.data.remote.model.NovelSearchMode
import com.projectu.shared.data.remote.model.UserSearchMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.domain.model.User

/**
 * 搜索分类
 */
enum class SearchCategory(val displayNameKey: String) {
    ILLUST("search_tab_illust"),
    NOVEL("search_tab_novel"),
    USER("search_tab_user")
}

/**
 * 排序方式
 */
enum class SortOrder(
    val value: String, 
    val displayNameKey: String, 
    val requiresPremium: Boolean = false,
    val isPreviewMode: Boolean = false  // 是否为预览模式（伪热度排序）
) {
    DATE_DESC("date_d", "search_sort_date_desc"),
    DATE_ASC("date", "search_sort_date_asc"),
    // 伪热度排序（仅限非会员，使用响应体中的 popular 数据）
    POPULAR_PREVIEW("date_d", "search_sort_popular_preview", requiresPremium = false, isPreviewMode = true),
    // 以下排序方式仅限高级会员
    POPULAR_DESC("popular_d", "search_sort_popular_desc", true),
    POPULAR_MALE_DESC("popular_male_d", "search_sort_popular_male_desc", true),
    POPULAR_FEMALE_DESC("popular_female_d", "search_sort_popular_female_desc", true)
}

/**
 * 内容分级
 */
enum class ContentMode(val value: String, val displayNameKey: String) {
    ALL("all", "search_rating_all"),
    SAFE("safe", "search_rating_safe"),
    R18("r18", "R18")
}

/**
 * 日期范围
 */
data class DateRange(
    val startDate: String?,  // yyyy-MM-dd
    val endDate: String?     // yyyy-MM-dd
)

/**
 * 收藏人数筛选
 * 
 * 注意: tag参数是Pixiv API要求的固定格式，包含日文字符"入り"，不应修改
 */
@Suppress("HardcodedChinese")
enum class BookmarkCount(val tag: String, val displayNameKey: String) {
    NONE("", "bookmark_count_none"),
    USERS_500("500users入り", "bookmark_count_500"),
    USERS_1000("1000users入り", "bookmark_count_1000"),
    USERS_2000("2000users入り", "bookmark_count_2000"),
    USERS_5000("5000users入り", "bookmark_count_5000"),
    USERS_7500("7500users入り", "bookmark_count_7500"),
    USERS_10000("10000users入り", "bookmark_count_10000"),
    USERS_20000("20000users入り", "bookmark_count_20000"),
    USERS_50000("50000users入り", "bookmark_count_50000"),
    USERS_100000("100000users入り", "bookmark_count_100000")
}

/**
 * 插画搜索参数
 */
data class IllustSearchParams(
    val searchMode: IllustSearchMode = IllustSearchMode.DEFAULT,
    val order: SortOrder = SortOrder.POPULAR_PREVIEW,  // 默认值：非会员使用热度预览，会根据会员状态动态调整
    val contentMode: ContentMode = ContentMode.ALL,
    val bookmarkCount: BookmarkCount = BookmarkCount.NONE,
    val hideAi: Boolean = false,
    val dateRange: DateRange? = null
) {
    companion object {
        /**
         * 根据会员状态创建默认参数
         * 会员用户：真实热度排序
         * 非会员用户：热度预览（伪热度排序）
         */
        fun createDefault(isPremium: Boolean): IllustSearchParams {
            return IllustSearchParams(
                order = if (isPremium) SortOrder.POPULAR_DESC else SortOrder.POPULAR_PREVIEW
            )
        }
    }
}

/**
 * 小说搜索参数
 */
data class NovelSearchParams(
    val searchMode: NovelSearchMode = NovelSearchMode.DEFAULT,
    val order: SortOrder = SortOrder.DATE_DESC,  // 默认值，会根据会员状态动态调整
    val contentMode: ContentMode = ContentMode.ALL,
    val bookmarkCount: BookmarkCount = BookmarkCount.NONE,
    val hideAi: Boolean = false,
    val dateRange: DateRange? = null
) {
    companion object {
        /**
         * 根据会员状态创建默认参数
         * 会员用户：真实热度排序
         * 非会员用户：时间排序（小说接口不返回 popular 数据，无法使用热度预览）
         */
        fun createDefault(isPremium: Boolean): NovelSearchParams {
            return NovelSearchParams(
                order = if (isPremium) SortOrder.POPULAR_DESC else SortOrder.DATE_DESC
            )
        }
    }
}

/**
 * 用户搜索参数
 */
data class UserSearchParams(
    val searchMode: UserSearchMode = UserSearchMode.DEFAULT,
    val onlyWithWork: Boolean = true
)

/**
 * 搜索结果页面状态
 */
data class SearchResultState(
    // 搜索关键词
    val searchKeyword: TextFieldValue = TextFieldValue(""),
    
    // 当前分类
    val currentCategory: SearchCategory = SearchCategory.ILLUST,
    
    // 标签自动补全
    val autocompleteSuggestions: List<Tag> = emptyList(),
    val isLoadingAutocomplete: Boolean = false,
    
    // 用户会员状态
    val isPremiumUser: Boolean = false,
    
    // 筛选参数
    val illustParams: IllustSearchParams = IllustSearchParams(),
    val novelParams: NovelSearchParams = NovelSearchParams(),
    val userParams: UserSearchParams = UserSearchParams(),
    
    // 筛选参数快照（用于检测变更）
    val illustParamsSnapshot: IllustSearchParams? = null,
    val novelParamsSnapshot: NovelSearchParams? = null,
    val userParamsSnapshot: UserSearchParams? = null,
    
    // 搜索结果
    val illustResults: List<Artwork> = emptyList(),
    val novelResults: List<Novel> = emptyList(),
    val userResults: List<User> = emptyList(),
    
    // 分页状态
    val illustPage: Int = 1,
    val novelPage: Int = 1,
    val userPage: Int = 1,
    val hasMoreIllust: Boolean = true,
    val hasMoreNovel: Boolean = true,
    val hasMoreUser: Boolean = true,
    
    // 加载状态
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    
    // 筛选抽屉状态
    val isFilterDrawerOpen: Boolean = false,
    
    // 错误状态
    val error: String? = null
)
