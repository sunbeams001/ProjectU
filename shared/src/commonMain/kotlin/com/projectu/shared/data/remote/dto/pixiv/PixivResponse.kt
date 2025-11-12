package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

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

/**
 * 用于接收空数组响应的类型
 * 某些 Pixiv API 在成功时返回 "body": [] 而不是 null 或对象
 */
typealias EmptyArrayResponse = PixivResponse<JsonArray>

