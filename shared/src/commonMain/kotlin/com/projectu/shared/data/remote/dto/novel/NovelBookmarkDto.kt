package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkData
import kotlinx.serialization.Serializable

/**
 * 小说收藏状态响应体
 * 用于 /ajax/novel/{novelId}/bookmarkData 接口
 */
@Serializable
data class NovelBookmarkStatusBody(
    val id: Long,  // 小说ID
    val isBookmarkable: Boolean,  // 是否可收藏
    val bookmarkData: BookmarkData? = null  // 收藏数据，未收藏时为 null
)
