package com.projectu.shared.data.remote.dto.illust

import kotlinx.serialization.Serializable

/**
 * 推荐作品响应体（用于 getRecommendIllusts）
 */
@Serializable
data class IllustRecommendBody(
    val illusts: List<IllustSimple>
)

/**
 * 推荐初始化响应体（用于 getRecommendInit）
 */
@Serializable
data class IllustRecommendInitBody(
    val illusts: List<IllustSimple>,
    val nextIds: List<Long>,
    val details: Map<String, RecommendMetadata>? = null
)

/**
 * 推荐元数据
 */
@Serializable
data class RecommendMetadata(
    val methods: List<String>,
    val score: Double,
    val seedIllustIds: List<Long>,
    val banditInfo: String,
    val recommendListId: String
)
