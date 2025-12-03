package com.projectu.ui.screens.followlatest.more

import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.follow_latest_watch_list
import projectu.composeapp.generated.resources.follow_latest_good_p_friends

/**
 * 动态更多页面的内容类型
 * 用于一级导航：追更列表 / 好P友
 */
enum class FollowLatestMoreContentType(
    val displayNameRes: StringResource
) {
    /** 追更列表 - 已追更的系列 */
    WATCH_LIST(Res.string.follow_latest_watch_list),
    
    /** 好P友 - 朋友们的活动 */
    GOOD_P_FRIENDS(Res.string.follow_latest_good_p_friends);
    
    companion object {
        fun getAll(): List<FollowLatestMoreContentType> = entries.toList()
    }
}
