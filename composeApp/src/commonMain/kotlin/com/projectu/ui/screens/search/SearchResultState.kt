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
enum class SearchCategory(val displayName: String) {
    ILLUST("插画+漫画+动图"),
    NOVEL("小说"),
    USER("用户")
}

/**
 * 排序方式
 */
enum class SortOrder(val value: String, val displayName: String) {
    DATE_DESC("date_d", "从新到旧"),
    DATE_ASC("date", "从旧到新")
}

/**
 * 内容分级
 */
enum class ContentMode(val value: String, val displayName: String) {
    ALL("all", "全部"),
    SAFE("safe", "全年龄"),
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
 * 插画搜索参数
 */
data class IllustSearchParams(
    val searchMode: IllustSearchMode = IllustSearchMode.DEFAULT,
    val order: SortOrder = SortOrder.DATE_DESC,
    val contentMode: ContentMode = ContentMode.ALL,
    val hideAi: Boolean = false,
    val dateRange: DateRange? = null
)

/**
 * 小说搜索参数
 */
data class NovelSearchParams(
    val searchMode: NovelSearchMode = NovelSearchMode.DEFAULT,
    val order: SortOrder = SortOrder.DATE_DESC,
    val contentMode: ContentMode = ContentMode.ALL,
    val hideAi: Boolean = false,
    val dateRange: DateRange? = null
)

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
