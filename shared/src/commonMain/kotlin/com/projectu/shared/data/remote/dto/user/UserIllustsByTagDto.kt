package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.illust.IllustSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户指定标签的插画作品列表响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/illusts/tag?tag={tag}&offset={offset}&limit={limit}&sensitiveFilterMode=userSetting&lang=zh
 * 示例: https://www.pixiv.net/ajax/user/16208053/illusts/tag?tag=女の子&offset=0&limit=48&sensitiveFilterMode=userSetting&lang=zh
 * 
 * 用途：获取该用户指定标签下的插画作品列表
 */
@Serializable
data class UserIllustsByTagBody(
    /**
     * 作品列表
     */
    val works: List<IllustSimple>,
    
    /**
     * 该标签下的作品总数
     */
    val total: Int,
    
    /**
     * 广告配置
     */
    @SerialName("zoneConfig")
    val zoneConfig: Map<String, ZoneConfig>? = null,
    
    /**
     * 额外数据
     */
    val extraData: ExtraData? = null
)

/**
 * 广告区域配置
 */
@Serializable
data class ZoneConfig(
    val url: String
)

/**
 * 额外数据
 */
@Serializable
data class ExtraData(
    val meta: MetaData? = null
)

/**
 * 元数据
 */
@Serializable
data class MetaData(
    val title: String? = null,
    val description: String? = null,
    val canonical: String? = null,
    val ogp: OgpData? = null,
    val twitter: TwitterData? = null,
    @SerialName("alternateLanguages")
    val alternateLanguages: Map<String, String>? = null,
    @SerialName("descriptionHeader")
    val descriptionHeader: String? = null
)

/**
 * OGP 数据
 */
@Serializable
data class OgpData(
    val description: String? = null,
    val image: String? = null,
    val title: String? = null,
    val type: String? = null
)

/**
 * Twitter 数据
 */
@Serializable
data class TwitterData(
    val description: String? = null,
    val image: String? = null,
    val title: String? = null,
    val card: String? = null
)
