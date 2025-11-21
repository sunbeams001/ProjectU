package com.projectu.shared.data.remote.dto.common

import kotlinx.serialization.Serializable

/**
 * 收藏数据（插画和小说共用）
 */
@Serializable
data class BookmarkData(
    val id: String? = null,  // 字符串类型
    val private: Boolean = false
)
