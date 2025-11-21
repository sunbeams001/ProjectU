package com.projectu.shared.data.remote.dto.illust

import kotlinx.serialization.Serializable

/**
 * Ugoira 元数据响应体
 */
@Serializable
data class UgoiraMetaBody(
    val src: String,
    val originalSrc: String,
    val mime_type: String,
    val frames: List<UgoiraFrame>
)

@Serializable
data class UgoiraFrame(
    val file: String,
    val delay: Int
)
