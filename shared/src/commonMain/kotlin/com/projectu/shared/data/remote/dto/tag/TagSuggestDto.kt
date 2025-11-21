package com.projectu.shared.data.remote.dto.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标签建议响应体
 */
@Serializable
data class TagSuggestBody(
    val candidates: List<TagCandidate> = emptyList()
)

/**
 * 标签候选项
 */
@Serializable
data class TagCandidate(
    @SerialName("tag_name") val tagName: String,
    @SerialName("illust_count") val illustCount: Long = 0,
    @SerialName("total_count") val totalCount: Long = 0,
    @SerialName("suggest_type") val suggestType: String? = null
)
