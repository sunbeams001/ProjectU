package com.projectu.shared.data.remote.dto.common

import kotlinx.serialization.Serializable

/**
 * Pixiv 标签信息（插画和小说通用）
 * 注意：小说API返回的标签不包含 translation 和 romaji 字段
 */
@Serializable
data class PixivTag(
    val tag: String,
    val locked: Boolean = false,
    val deletable: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val translation: Map<String, String>? = null,  // 插画专用：标签翻译（小说API不返回）
    val romaji: String? = null  // 插画专用：罗马音（小说API不返回）
)
