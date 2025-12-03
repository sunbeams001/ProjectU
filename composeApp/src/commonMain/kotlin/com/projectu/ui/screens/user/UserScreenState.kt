package com.projectu.ui.screens.user

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.data.remote.dto.user.UserInfoBody
import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.*

/**
 * 用户页面Tab类型
 */
enum class UserProfileTab(val displayNameRes: StringResource) {
    ILLUSTS(Res.string.user_tab_illusts),
    MANGA(Res.string.user_tab_manga),
    NOVELS(Res.string.user_tab_novels),
    MANGA_SERIES(Res.string.user_tab_manga_series),
    NOVEL_SERIES(Res.string.user_tab_novel_series),
    BOOKMARK_ILLUSTS_PUBLIC(Res.string.user_tab_bookmark_illusts_public),
    BOOKMARK_ILLUSTS_PRIVATE(Res.string.user_tab_bookmark_illusts_private),
    BOOKMARK_NOVELS_PUBLIC(Res.string.user_tab_bookmark_novels_public),
    BOOKMARK_NOVELS_PRIVATE(Res.string.user_tab_bookmark_novels_private),
    USER_INFO(Res.string.user_tab_info)  // 用户详情（放在最后）
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
 * 用户详细信息（完整的 full=1 信息）
 */
data class UserDetailInfo(
    // 基本信息
    val userId: String = "",
    val name: String = "",
    val image: String = "",
    val imageBig: String = "",
    val premium: Boolean = false,
    val isFollowed: Boolean = false,
    val isMypixiv: Boolean = false,
    val isBlocking: Boolean = false,
    val backgroundUrl: String? = null,
    val official: Boolean = false,
    
    // 社交数据
    val following: Int = 0,
    val mypixivCount: Int = 0,
    val followedBack: Boolean = false,
    val canSendMessage: Boolean = false,
    
    // 个人简介
    val comment: String? = null,
    val commentHtml: String? = null,
    val webpage: String? = null,
    
    // 社交媒体链接
    val twitterUrl: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val tumblrUrl: String? = null,
    val pawooUrl: String? = null,
    val circlemsUrl: String? = null,
    
    // 个人属性
    val region: String? = null,
    val age: String? = null,
    val birthDay: String? = null,
    val gender: String? = null,
    val job: String? = null,
    
    // 工作环境
    val workspacePc: String? = null,
    val workspaceMonitor: String? = null,
    val workspaceTool: String? = null,
    val workspaceScanner: String? = null,
    val workspaceTablet: String? = null,
    val workspaceMouse: String? = null,
    val workspacePrinter: String? = null,
    val workspaceDesktop: String? = null,
    val workspaceMusic: String? = null,
    val workspaceDesk: String? = null,
    val workspaceChair: String? = null,
    val workspaceComment: String? = null,
    val workspaceImageUrl: String? = null,
    val workspaceImageBigUrl: String? = null,
    
    // 接稿状态
    val commissionRequestStatus: String? = null,
    val commissionFanRequestStatus: String? = null,
    
    // 群组
    val groups: List<UserGroupInfo> = emptyList()
)

/**
 * 用户群组信息
 */
data class UserGroupInfo(
    val id: String,
    val title: String,
    val iconUrl: String? = null
)

/**
 * 每个Tab的数据状态
 * 
 * 支持两种分页模式：
 * 1. ID模式：用于用户作品列表（allIds + loadedIds）
 * 2. Offset模式：用于收藏列表（offset + total）
 */
data class TabData(
    val allIds: List<String> = emptyList(),     // 所有作品ID（用于用户作品）
    val loadedIds: List<String> = emptyList(),  // 已加载的ID（用于用户作品）
    val artworks: List<Artwork> = emptyList(),  // 插画/漫画列表
    val novels: List<Novel> = emptyList(),      // 小说列表
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,          // 下拉刷新中
    val hasMore: Boolean = true,
    val error: String? = null,
    // 收藏列表分页相关
    val offset: Int = 0,                        // 当前偏移量（用于收藏列表）
    val total: Int = 0                          // 总数量（用于收藏列表）
)

/**
 * 用户页面状态
 */
data class UserScreenState(
    // 用户基本信息
    val userProfile: UserProfile = UserProfile(),
    val isLoadingProfile: Boolean = false,
    val profileError: String? = null,
    
    // 用户详细信息（full=1）
    val userDetailInfo: UserDetailInfo? = null,
    
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
