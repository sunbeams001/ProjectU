package com.projectu.shared.data.remote.dto.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标题说明翻译
 */
@Serializable
data class TitleCaptionTranslation(
    val workTitle: String? = null,
    val workCaption: String? = null
)

/**
 * 广告区配置
 */
@Serializable
data class ZoneConfig(
    val responsive: ZoneConfigItem? = null,
    val rectangle: ZoneConfigItem? = null,
    @SerialName("500x500")
    val size500x500: ZoneConfigItem? = null,
    val header: ZoneConfigItem? = null,
    val footer: ZoneConfigItem? = null,
    val expandedFooter: ZoneConfigItem? = null,
    val logo: ZoneConfigItem? = null,
    @SerialName("ad_logo")
    val adLogo: ZoneConfigItem? = null,
    @SerialName("t_responsive_320_50")
    val tResponsive320x50: ZoneConfigItem? = null,
    @SerialName("t_responsive_300_250")
    val tResponsive300x250: ZoneConfigItem? = null,
    val relatedworks: ZoneConfigItem? = null
)

/**
 * 广告区项
 */
@Serializable
data class ZoneConfigItem(
    val url: String
)

/**
 * 额外元数据
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
    val title: String,
    val description: String,
    val canonical: String,
    val alternateLanguages: Map<String, String>? = null,
    val descriptionHeader: String? = null,
    val ogp: OgpData? = null,
    val twitter: TwitterData? = null
)

/**
 * OGP 元数据
 */
@Serializable
data class OgpData(
    val description: String,
    val image: String,
    val title: String,
    val type: String
)

/**
 * Twitter 卡片
 */
@Serializable
data class TwitterData(
    val description: String,
    val image: String,
    val title: String,
    val card: String
)
