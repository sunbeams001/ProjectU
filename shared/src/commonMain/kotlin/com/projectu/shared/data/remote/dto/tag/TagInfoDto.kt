package com.projectu.shared.data.remote.dto.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标签翻译信息
 */
@Serializable
data class TagTranslation(
    val tag: String? = null,
    @SerialName("abstract") val abstract: String? = null,
    val url: String? = null
)

/**
 * 标签信息响应体
 */
@Serializable
data class TagInfoBody(
    val tag: String,
    @SerialName("abstract") val abstract: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    val en: TagTranslation? = null,
    @SerialName("en_new") val enNew: TagTranslation? = null,
    val ja: TagTranslation? = null,
    @SerialName("ja_new") val jaNew: TagTranslation? = null,
    @SerialName("is_view_lead_wire") val isViewLeadWire: Boolean = false
)

/**
 * 添加标签响应体
 */
@Serializable
data class AddTagBody(
    val success: Boolean = false,
    val message: String? = null
)
