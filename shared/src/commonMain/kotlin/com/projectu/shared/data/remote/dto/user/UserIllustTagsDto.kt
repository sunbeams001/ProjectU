package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户插画标签列表响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/illusts/tags?all=1&lang=zh
 * 示例: https://www.pixiv.net/ajax/user/757415/illusts/tags?all=1&lang=zh
 * 
 * 用途：获取该用户所有插画的标签列表（按作品数量统计）
 * 
 * 注意：API 返回的 body 字段直接是标签数组
 */
typealias UserIllustTagsBody = List<UserIllustTag>

/**
 * 用户插画标签
 */
@Serializable
data class UserIllustTag(
    /**
     * 标签名称
     */
    val tag: String,
    
    /**
     * 标签翻译（可能为空）
     */
    @SerialName("tag_translation")
    val tagTranslation: String? = null,
    
    /**
     * 标签读音（日文假名，可能为空）
     */
    @SerialName("tag_yomigana")
    val tagYomigana: String? = null,
    
    /**
     * 该标签下的作品数量
     */
    val cnt: Int
)
