package com.projectu.shared.data.remote.dto.bookmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 收藏标签
 */
@Serializable
data class BookmarkTag(
    val tag: String,
    val cnt: Int
)

/**
 * 收藏标签响应
 */
@Serializable
data class BookmarkTagsResponse(
    val public: List<BookmarkTag> = emptyList(),
    val private: List<BookmarkTag> = emptyList(),
    @SerialName("tooManyBookmark") val tooManyBookmark: Boolean = false,
    @SerialName("tooManyBookmarkTags") val tooManyBookmarkTags: Boolean = false
)
