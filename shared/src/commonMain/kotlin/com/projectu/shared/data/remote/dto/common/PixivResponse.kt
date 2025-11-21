package com.projectu.shared.data.remote.dto.common

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

/**
 * 包含原始 JSON 的 Pixiv API 响应包装类
 * 用于 API 测试工具，保存原始响应体以便调试
 */
data class PixivResponseWithRaw<T>(
    val response: PixivResponse<T>,
    val rawJson: String
)
