package com.projectu.ui.screens.userrelations

import com.projectu.shared.domain.model.User
import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.*

/**
 * 用户关系类型（一级导航）
 */
enum class UserRelationType(val displayNameRes: StringResource) {
    FOLLOWING(Res.string.user_relations_following),   // 已关注
    MY_PIXIV(Res.string.user_relations_mypixiv),      // 好P友
    FOLLOWERS(Res.string.user_relations_followers)    // 粉丝
}

/**
 * 关注类型的可见性（二级导航，仅用于 FOLLOWING）
 */
enum class FollowingVisibility(val displayNameRes: StringResource, val apiValue: String) {
    PUBLIC(Res.string.user_relations_public, "show"),    // 公开
    PRIVATE(Res.string.user_relations_private, "hide")   // 私人
}

/**
 * 用于 HorizontalPager 的页面定义
 * 组合一级导航和二级导航，支持左右滑动浏览全部页面
 */
sealed class RelationPage(
    val relationType: UserRelationType,
    val visibility: FollowingVisibility? = null
) {
    /** 公开关注 */
    data object FollowingPublic : RelationPage(UserRelationType.FOLLOWING, FollowingVisibility.PUBLIC)
    
    /** 私人关注 */
    data object FollowingPrivate : RelationPage(UserRelationType.FOLLOWING, FollowingVisibility.PRIVATE)
    
    /** 好P友 */
    data object MyPixiv : RelationPage(UserRelationType.MY_PIXIV)
    
    /** 粉丝 */
    data object Followers : RelationPage(UserRelationType.FOLLOWERS)
    
    /**
     * 唯一标识，用于缓存 key
     */
    val key: String
        get() = when (this) {
            is FollowingPublic -> "following_public"
            is FollowingPrivate -> "following_private"
            is MyPixiv -> "mypixiv"
            is Followers -> "followers"
        }
    
    companion object {
        /**
         * 根据 key 获取对应的 RelationPage
         */
        fun fromKey(key: String): RelationPage = when (key) {
            "following_public" -> FollowingPublic
            "following_private" -> FollowingPrivate
            "mypixiv" -> MyPixiv
            "followers" -> Followers
            else -> FollowingPublic
        }
        
        /**
         * 获取当前登录用户可见的所有页面（包含粉丝）
         */
        fun getAllPagesForSelf(): List<RelationPage> = listOf(
            FollowingPublic,
            FollowingPrivate,
            MyPixiv,
            Followers
        )
        
        /**
         * 获取查看其他用户时可见的页面（不包含粉丝和私人关注）
         */
        fun getAllPagesForOther(): List<RelationPage> = listOf(
            FollowingPublic,
            MyPixiv
        )
        
        /**
         * 根据一级导航和二级导航索引获取页面
         */
        fun fromIndices(
            primaryIndex: Int,
            secondaryIndex: Int,
            isSelf: Boolean
        ): RelationPage {
            return when (primaryIndex) {
                0 -> {
                    // 关注列表：自己可以看公开/私人，其他用户只能看公开
                    if (isSelf && secondaryIndex == 1) FollowingPrivate else FollowingPublic
                }
                1 -> MyPixiv
                2 -> if (isSelf) Followers else MyPixiv
                else -> FollowingPublic
            }
        }
        
        /**
         * 获取页面在列表中的索引
         */
        fun RelationPage.getPageIndex(isSelf: Boolean): Int {
            val pages = if (isSelf) getAllPagesForSelf() else getAllPagesForOther()
            return pages.indexOf(this).coerceAtLeast(0)
        }
    }
}



/**
 * 每个页面的数据状态
 */
data class RelationPageData(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val offset: Int = 0,
    val total: Int = 0
)

/**
 * 用户关系页面状态
 */
data class UserRelationsScreenState(
    // 目标用户ID
    val userId: String = "",
    
    // 是否为当前登录用户
    val isSelf: Boolean = false,
    
    // 当前登录用户ID
    val currentUserId: String = "",
    
    // 用户名（用于标题显示）
    val userName: String = "",
    
    // 可用的页面列表
    val availablePages: List<RelationPage> = emptyList(),
    
    // 当前选中的页面
    val currentPage: RelationPage = RelationPage.FollowingPublic,
    
    // 各页面的数据缓存
    val pageDataCache: Map<String, RelationPageData> = emptyMap(),
    
    // 滚动目标索引（用于从详情页返回时恢复滚动位置）
    val scrollTargets: Map<String, Int> = emptyMap()
) {
    /**
     * 获取当前页面的数据
     */
    val currentPageData: RelationPageData
        get() = pageDataCache[currentPage.key] ?: RelationPageData()
    
    /**
     * 当前一级导航索引
     */
    val currentPrimaryIndex: Int
        get() = when (currentPage.relationType) {
            UserRelationType.FOLLOWING -> 0
            UserRelationType.MY_PIXIV -> 1
            UserRelationType.FOLLOWERS -> 2
        }
    
    /**
     * 当前二级导航索引（仅对 FOLLOWING 有效）
     */
    val currentSecondaryIndex: Int
        get() = when (currentPage) {
            is RelationPage.FollowingPublic -> 0
            is RelationPage.FollowingPrivate -> 1
            else -> 0
        }
    
    /**
     * 获取当前一级导航下的可用二级导航项
     * 只有当查看自己的关注列表时才显示公开/私人选项
     */
    val currentSecondaryItems: List<FollowingVisibility>
        get() = when {
            currentPage.relationType == UserRelationType.FOLLOWING && isSelf -> 
                listOf(FollowingVisibility.PUBLIC, FollowingVisibility.PRIVATE)
            else -> emptyList()
        }
    
    /**
     * 当前一级导航是否有二级导航
     * 只有当查看自己的关注列表时才显示二级导航
     */
    val hasSecondaryNavigation: Boolean
        get() = currentPage.relationType == UserRelationType.FOLLOWING && isSelf
}
