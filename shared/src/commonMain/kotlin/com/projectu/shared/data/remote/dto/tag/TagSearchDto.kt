package com.projectu.shared.data.remote.dto.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标签搜索建议响应体（来自 /rpc/cps.php）
 */
@Serializable
data class TagSearchSuggestBody(
    val candidates: List<TagSearchCandidate> = emptyList()
)

/**
 * 标签搜索候选项
 */
@Serializable
data class TagSearchCandidate(
    @SerialName("tag_name") val tagName: String,
    @SerialName("access_count") val accessCount: String = "0",
    val type: String,
    @SerialName("tag_translation") val tagTranslation: String? = null
)
