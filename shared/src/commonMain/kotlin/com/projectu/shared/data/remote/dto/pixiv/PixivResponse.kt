package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.Serializable

/**
 * Pixiv API 响应基础结构
 * @param T 响应体类型
 */
@Serializable
data class PixivResponse<T>(
    val body: T? = null,
    val error: Boolean = false,
    val message: String = ""
)

