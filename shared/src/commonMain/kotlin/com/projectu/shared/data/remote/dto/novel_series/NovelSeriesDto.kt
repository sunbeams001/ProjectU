package com.projectu.shared.data.remote.dto.novel_series

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 小说系列详情
 */
@Serializable
data class NovelSeriesBody(
    val id: String,
    val title: String,
    val caption: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("xRestrict") val xRestrict: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    @SerialName("isConcluded") val isConcluded: Boolean = false,
    @SerialName("genreId") val genreId: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("publishedContentCount") val publishedContentCount: Int,
    @SerialName("publishedTotalCharacterCount") val publishedTotalCharacterCount: Int? = null,
    @SerialName("publishedTotalWordCount") val publishedTotalWordCount: Int? = null,
    @SerialName("publishedReadingTime") val publishedReadingTime: Int? = null,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    @SerialName("lastPublishedContentTimestamp") val lastPublishedContentTimestamp: Long? = null,
    @SerialName("createdTimestamp") val createdTimestamp: Long? = null,
    @SerialName("updatedTimestamp") val updatedTimestamp: Long? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("firstNovelId") val firstNovelId: String? = null,
    @SerialName("latestNovelId") val latestNovelId: String? = null,
    @SerialName("displaySeriesContentCount") val displaySeriesContentCount: Int,
    @SerialName("shareText") val shareText: String? = null,
    @SerialName("total") val total: Int,
    @SerialName("firstEpisode") val firstEpisode: NovelSeriesFirstEpisode? = null,
    @SerialName("watchCount") val watchCount: Int? = null,
    @SerialName("maxXRestrict") val maxXRestrict: Int? = null,
    @SerialName("cover") val cover: NovelSeriesCover? = null,
    @SerialName("coverSettingData") val coverSettingData: JsonObject? = null,
    @SerialName("isWatched") val isWatched: Boolean = false,
    @SerialName("isNotifying") val isNotifying: Boolean = false,
    @SerialName("aiType") val aiType: Int = 0,
    @SerialName("hasGlossary") val hasGlossary: Boolean = false,
    @SerialName("extraData") val extraData: JsonObject? = null,
    @SerialName("zoneConfig") val zoneConfig: JsonObject? = null
)

/**
 * 小说系列首集信息
 */
@Serializable
data class NovelSeriesFirstEpisode(
    val url: String
)

/**
 * 小说系列信息
 */
@Serializable
data class NovelSeriesInfo(
    val id: Long,
    @SerialName("viewableType") val viewableType: Int = 0,
    @SerialName("contentOrder") val contentOrder: Int
)
