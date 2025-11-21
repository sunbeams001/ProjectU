package com.projectu.shared.data.remote.dto.novel_series

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说系列封面信息
 */
@Serializable
data class NovelSeriesCover(
    val urls: NovelSeriesCoverUrls
)

/**
 * 小说系列封面URL集合
 */
@Serializable
data class NovelSeriesCoverUrls(
    @SerialName("240mw") val size240mw: String? = null,
    @SerialName("480mw") val size480mw: String? = null,
    @SerialName("1200x1200") val size1200x1200: String? = null,
    @SerialName("128x128") val size128x128: String? = null,
    val original: String? = null
)
