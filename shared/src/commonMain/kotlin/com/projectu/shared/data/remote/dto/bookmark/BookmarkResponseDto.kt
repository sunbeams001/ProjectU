package com.projectu.shared.data.remote.dto.bookmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 添加收藏响应
 */
@Serializable
data class BookmarkAddResponse(
    @SerialName("last_bookmark_id") val lastBookmarkId: String? = null,
    @SerialName("stacc_status_id") val staccStatusId: String? = null
)
