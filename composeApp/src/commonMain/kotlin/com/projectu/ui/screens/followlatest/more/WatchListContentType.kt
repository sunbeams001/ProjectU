package com.projectu.ui.screens.followlatest.more

import org.jetbrains.compose.resources.StringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.follow_latest_manga
import projectu.composeapp.generated.resources.follow_latest_novels

/**
 * 追更列表的子内容类型
 * 用于二级导航：漫画 / 小说
 */
enum class WatchListContentType(
    val displayNameRes: StringResource
) {
    /** 漫画追更 */
    MANGA(Res.string.follow_latest_manga),
    
    /** 小说追更 */
    NOVELS(Res.string.follow_latest_novels);
    
    companion object {
        fun getAll(): List<WatchListContentType> = entries.toList()
    }
}
