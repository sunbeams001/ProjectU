package com.projectu.ui.screens.user

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel

/**
 * 用户页面Tab类型
 */
enum class UserProfileTab(val displayName: String) {
    ILLUSTS("插画"),
    MANGA("漫画"),
    NOVELS("小说"),
    MANGA_SERIES("漫画系列"),
    NOVEL_SERIES("小说系列"),
    BOOKMARKS("收藏")
}

/**
 * 用户基本信息
 */
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val image: String = "",
    val imageBig: String = "",
    val premium: Boolean = false,
    val isFollowed: Boolean = false,
    val following: Int = 0,
    val comment: String? = null,
    val backgroundUrl: String? = null
)

/**
 * 每个Tab的数据状态
 */
data class TabData(
    val allIds: List<String> = emptyList(),     // 所有作品ID
    val loadedIds: List<String> = emptyList(),  // 已加载的ID
    val artworks: List<Artwork> = emptyList(),  // 插画/漫画列表
    val novels: List<Novel> = emptyList(),      // 小说列表
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

/**
 * 漫画系列信息
 */
data class MangaSeriesItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val caption: String? = null,
    val total: Int = 0,             // 总篇数
    val coverUrl: String? = null,   // 封面URL
    val isWatched: Boolean = false, // 是否已追更
    val updateDate: String? = null  // 更新日期
)

/**
 * 小说系列信息
 */
data class NovelSeriesItem(
    val id: String,
    val title: String,
    val caption: String? = null,
    val contentCount: Int = 0,
    val coverUrl: String? = null,
    val tags: List<String> = emptyList(),
    val totalCharacterCount: Int = 0,  // 总字数
    val totalWordCount: Int = 0,       // 总单词数
    val readingTime: Int = 0,          // 预计阅读时间（分钟）
    val xRestrict: Int = 0,            // 年龄限制：0=全年龄, 1=R-18, 2=R-18G
    val isOriginal: Boolean = false,   // 是否原创
    val isConcluded: Boolean = false   // 是否已完结
)

/**
 * 用户页面状态
 */
data class UserScreenState(
    // 用户基本信息
    val userProfile: UserProfile = UserProfile(),
    val isLoadingProfile: Boolean = false,
    val profileError: String? = null,
    
    // 可用的Tab列表（根据用户作品情况动态生成）
    val availableTabs: List<UserProfileTab> = emptyList(),
    
    // 当前选中的Tab
    val currentTab: UserProfileTab = UserProfileTab.ILLUSTS,
    
    // 各Tab的数据缓存
    val tabDataCache: Map<UserProfileTab, TabData> = emptyMap(),
    
    // 漫画系列数据
    val mangaSeries: List<MangaSeriesItem> = emptyList(),
    
    // 小说系列数据
    val novelSeries: List<NovelSeriesItem> = emptyList()
) {
    /**
     * 获取当前Tab的数据
     */
    val currentTabData: TabData
        get() = tabDataCache[currentTab] ?: TabData()
    
    /**
     * 当前Tab是否有作品
     */
    val hasCurrentTabContent: Boolean
        get() = currentTabData.allIds.isNotEmpty()
}
