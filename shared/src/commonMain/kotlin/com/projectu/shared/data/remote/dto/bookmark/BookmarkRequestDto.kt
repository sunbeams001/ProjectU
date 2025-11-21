package com.projectu.shared.data.remote.dto.bookmark

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
