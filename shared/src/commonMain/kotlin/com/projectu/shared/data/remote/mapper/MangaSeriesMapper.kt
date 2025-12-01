package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.user.MangaSeriesInfo
import com.projectu.shared.domain.model.MangaSeries

/**
 * 漫画系列 DTO 到 Domain 模型的映射器
 */

/**
 * 将 UserProfile 中的 MangaSeriesInfo 转换为 MangaSeries
 * 
 * 用于用户页面的漫画系列列表显示
 */
fun MangaSeriesInfo.toMangaSeries(): MangaSeries {
    return MangaSeries(
        id = id,
        title = title,
        description = description,
        caption = caption,
        userId = userId,
        coverUrl = url,
        isWatched = isWatched,
        isNotifying = isNotifying,
        total = total,
        watchCount = watchCount,
        createDate = createDate,
        updateDate = updateDate,
        firstIllustId = firstIllustId,
        latestIllustId = latestIllustId
    )
}
