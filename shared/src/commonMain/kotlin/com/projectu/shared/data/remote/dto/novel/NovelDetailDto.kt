package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.PixivTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说标签信息
 */
@Serializable
data class NovelTagInfo(
    @SerialName("authorId") val authorId: String,
    @SerialName("isLocked") val isLocked: Boolean,
    val tags: List<PixivTag> = emptyList(),  // 使用通用的 PixivTag，小说API不返回 translation 和 romaji 字段
    val writable: Boolean
)

/**
 * 小说详情响应体
 */
@Serializable
data class NovelDetailBody(
    val id: String,
    val title: String,
    val content: String,
    @SerialName("createDate") val createDate: String,
    @SerialName("uploadDate") val uploadDate: String,
    val description: String,
    @SerialName("bookmarkCount") val bookmarkCount: Int,
    @SerialName("likeCount") val likeCount: Int,
    @SerialName("viewCount") val viewCount: Int,
    @SerialName("commentCount") val commentCount: Int,
    @SerialName("markerCount") val markerCount: Int,
    @SerialName("pageCount") val pageCount: Int,
    @SerialName("isOriginal") val isOriginal: Boolean,
    @SerialName("isBungei") val isBungei: Boolean,
    @SerialName("xRestrict") val xRestrict: Int,
    val restrict: Int,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    val tags: NovelTagInfo
)
