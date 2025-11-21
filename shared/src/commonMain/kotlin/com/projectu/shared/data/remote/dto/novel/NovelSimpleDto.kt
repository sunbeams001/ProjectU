package com.projectu.shared.data.remote.dto.novel

import kotlinx.serialization.Serializable

/**
 * 小说系列简要信息（用于 Thumbnails）
 */
@Serializable
data class NovelSeriesSimple(
    val id: String,
    val title: String
)
