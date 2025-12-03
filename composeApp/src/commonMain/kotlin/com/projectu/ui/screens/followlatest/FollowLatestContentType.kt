package com.projectu.ui.screens.followlatest

import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.follow_latest_illusts
import projectu.composeapp.generated.resources.follow_latest_novels

/**
 * 动态页面内容类型
 */
enum class FollowLatestContentType(val displayNameRes: StringResource) {
    ILLUSTS(Res.string.follow_latest_illusts),
    NOVELS(Res.string.follow_latest_novels);
    
    companion object {
        fun getAll(): List<FollowLatestContentType> = listOf(ILLUSTS, NOVELS)
    }
}
