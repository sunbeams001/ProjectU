package com.projectu.shared.data.remote.dto.pixiv

/**
 * 包含原始 JSON 的 Pixiv API 响应包装类
 * 用于 API 测试工具，保存原始响应体以便调试
 */
data class PixivResponseWithRaw<T>(
    val response: PixivResponse<T>,
    val rawJson: String
)
