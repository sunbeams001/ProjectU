package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 添加作品收藏请求体
 */
@Serializable
data class BookmarkRequest(
    @SerialName("illust_id") val illustId: String,
    val restrict: Int,
    val comment: String = "",
    val tags: List<String> = emptyList()
)

/**
 * 添加小说收藏请求体
 */
@Serializable
data class NovelBookmarkRequest(
    @SerialName("novel_id") val novelId: String,
    val restrict: Int,
    val comment: String = "",
    val tags: List<String> = emptyList()
)

/**
 * 添加收藏响应
 */
@Serializable
data class BookmarkAddResponse(
    @SerialName("last_bookmark_id") val lastBookmarkId: String? = null,
    @SerialName("stacc_status_id") val staccStatusId: String? = null
)

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
