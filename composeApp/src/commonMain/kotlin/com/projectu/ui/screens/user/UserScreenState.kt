package com.projectu.ui.screens.user

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries

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
    
    // 漫画系列数据（使用领域模型）
    val mangaSeries: List<MangaSeries> = emptyList(),

    // 小说系列数据（使用领域模型）
    val novelSeries: List<NovelSeries> = emptyList()
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
