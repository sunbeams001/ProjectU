package com.projectu.shared.data.remote.dto.novel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说书签响应体
 */
@Serializable
data class NovelMarkerBody(
    val page: Int
)

/**
 * 小说书签列表响应体
 */
@Serializable
data class NovelMarkerListBody(
    val total: Int,
    val novels: List<NovelMarkerItem>
)

/**
 * 小说书签项
 */
@Serializable
data class NovelMarkerItem(
    val id: String,
    val title: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("textCount") val textCount: Int = 0,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
    val tags: List<String> = emptyList(),
    val description: String = "",
    @SerialName("xRestrict") val xRestrict: Int = 0,
    @SerialName("seriesId") val seriesId: String? = null,
    @SerialName("seriesTitle") val seriesTitle: String? = null
)
