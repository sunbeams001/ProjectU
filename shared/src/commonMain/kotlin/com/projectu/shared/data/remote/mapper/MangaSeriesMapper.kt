package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.follow.WatchedIllustSeries
import com.projectu.shared.data.remote.dto.user.MangaSeriesInfo
import com.projectu.shared.domain.model.MangaSeries

/**
 * 漫画系列 DTO 到 Domain 模型的映射器
 */

/**
 * 将 UserProfile 中的 MangaSeriesInfo 转换为 MangaSeries
 * 
 * 用于用户页面的漫画系列列表显示
 * 注意: 此 DTO 不包含作者名，需要单独从用户页面获取
 * 
 * @param userName 作者名（从用户页面获取）
 * @param profileImageUrl 作者头像URL
 * @param isFollowed 是否关注作者
 */
fun MangaSeriesInfo.toMangaSeries(
    userName: String = "",
    profileImageUrl: String? = null,
    isFollowed: Boolean = false
): MangaSeries {
    return MangaSeries(
        id = id,
        title = title,
        description = description ?: "",
        caption = caption ?: "",
        userId = userId ?: "",
        userName = userName,
        profileImageUrl = profileImageUrl,
        isFollowed = isFollowed,
        coverUrl = url,
        isWatched = isWatched,
        isNotifying = isNotifying,
        total = total,
        watchCount = watchCount,
        createDate = createDate ?: "",
        updateDate = updateDate ?: "",
        firstIllustId = firstIllustId,
        latestIllustId = latestIllustId
    )
}

/**
 * 将追更列表的 WatchedIllustSeries 转换为 MangaSeries
 * 
 * 用于追更列表页面的漫画系列显示
 * 注意: 此 DTO 不包含作者名
 * 
 * @param userName 作者名（可选，从其他途径获取）
 * @param profileImageUrl 作者头像URL
 * @param isFollowed 是否关注作者
 */
fun WatchedIllustSeries.toMangaSeries(
    userName: String = "",
    profileImageUrl: String? = null,
    isFollowed: Boolean = false
): MangaSeries {
    return MangaSeries(
        id = id,
        title = title,
        description = description ?: "",
        caption = caption ?: "",
        userId = userId,
        userName = userName,
        profileImageUrl = profileImageUrl,
        isFollowed = isFollowed,
        coverUrl = url,
        isWatched = isWatched,
        isNotifying = isNotifying,
        total = total,
        watchCount = watchCount,
        createDate = createDate ?: "",
        updateDate = updateDate ?: "",
        firstIllustId = firstIllustId,
        latestIllustId = latestIllustId
    )
}
