package com.projectu.shared.data.remote.dto.common

import kotlinx.serialization.Serializable

/**
 * 收藏数范围（搜索时使用）
 */
@Serializable
data class BookmarkRange(
    val min: Int?,
    val max: Int?
)
