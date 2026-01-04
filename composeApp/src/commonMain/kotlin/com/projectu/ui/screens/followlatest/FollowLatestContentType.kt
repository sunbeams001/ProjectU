package com.projectu.ui.screens.followlatest

import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.follow_latest_illusts
import projectu.composeapp.generated.resources.follow_latest_novels
import projectu.composeapp.generated.resources.follow_latest_watch_list
import projectu.composeapp.generated.resources.follow_latest_good_p_friends

/**
 * 动态页面内容类型
 * 包含：插画·漫画、小说、追更列表、好P友
 */
enum class FollowLatestContentType(val displayNameRes: StringResource) {
    ILLUSTS(Res.string.follow_latest_illusts),
    NOVELS(Res.string.follow_latest_novels),
    WATCH_LIST(Res.string.follow_latest_watch_list),
    GOOD_P_FRIENDS(Res.string.follow_latest_good_p_friends);
    
    companion object {
        fun getAll(): List<FollowLatestContentType> = listOf(
            ILLUSTS, 
            NOVELS, 
            WATCH_LIST, 
            GOOD_P_FRIENDS
        )
    }
}
