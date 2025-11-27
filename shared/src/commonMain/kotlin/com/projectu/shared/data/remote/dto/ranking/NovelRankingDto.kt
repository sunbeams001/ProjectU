package com.projectu.shared.data.remote.dto.ranking

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * 小说排行榜 JSON 响应（来自 /ajax/ranking/novel 接口）
 */
@Serializable
data class NovelRankingJsonResponse(
    @SerialName("error")
    val error: Boolean,
    
    @SerialName("body")
    val body: NovelRankingBody
)

/**
 * 小说排行榜主体数据
 */
@Serializable
data class NovelRankingBody(
    @SerialName("display_a")
    val displayA: NovelRankingDisplay,

    @SerialName("start")
    val start: String? = null,

    @SerialName("end")
    val end: String? = null,

    @SerialName("date")
    val date: String? = null,

    @SerialName("h_title")
    val hTitle: String? = null,

    @SerialName("zoneConfig")
    val zoneConfig: ZoneConfig? = null
)

@Serializable
data class ZoneConfig(
    @SerialName("header")
    val header: ZoneConfigItem? = null,
    @SerialName("footer")
    val footer: ZoneConfigItem? = null,
    @SerialName("logo")
    val logo: ZoneConfigItem? = null,
    @SerialName("ad_logo")
    val adLogo: ZoneConfigItem? = null
)

@Serializable
data class ZoneConfigItem(
    @SerialName("url")
    val url: String? = null
)

/**
 * 自定义序列化器，处理 rank_a 可能是数组或对象的情况
 * 
 * - 某些 mode（如 daily, weekly 等）返回数组格式: [{...}, {...}]
 * - 某些 mode（如 female, male_r18, female_r18）返回对象格式: {"0": {...}, "1": {...}}
 */
object RankASerializer : KSerializer<List<NovelRankingItem>> {
    private val listSerializer = ListSerializer(NovelRankingItem.serializer())
    private val mapSerializer = MapSerializer(String.serializer(), NovelRankingItem.serializer())
    
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("RankA")
    
    override fun deserialize(decoder: Decoder): List<NovelRankingItem> {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        
        return when {
            // 如果是数组格式，直接反序列化
            element is kotlinx.serialization.json.JsonArray -> {
                decoder.json.decodeFromJsonElement(listSerializer, element)
            }
            // 如果是对象格式，提取值并按键排序
            element is kotlinx.serialization.json.JsonObject -> {
                val map = decoder.json.decodeFromJsonElement(mapSerializer, element)
                map.entries
                    .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                    .map { it.value }
            }
            else -> emptyList()
        }
    }
    
    override fun serialize(encoder: Encoder, value: List<NovelRankingItem>) {
        listSerializer.serialize(encoder, value)
    }
}

/**
 * 排行榜显示数据
 */
@Serializable
data class NovelRankingDisplay(
    @SerialName("rank_a")
    @Serializable(with = RankASerializer::class)
    val rankA: List<NovelRankingItem>,

    @SerialName("mode")
    val mode: String? = null,

    @SerialName("page")
    val page: Int? = null,

    @SerialName("title")
    val title: String? = null,

    @SerialName("muted_count")
    val mutedCount: Int? = null,

    @SerialName("page_a")
    val pageA: Map<String, String>? = null,

    @SerialName("prev")
    val prev: Int? = null,

    @SerialName("next")
    val next: Int? = null,

    @SerialName("prev_date")
    val prevDate: String? = null,

    @SerialName("next_date")
    val nextDate: String? = null,

    @SerialName("x_restrict")
    val xRestrict: String? = null,

    @SerialName("is_r18_page")
    val isR18Page: Boolean? = null,

    @SerialName("header_bnr_ranking")
    val headerBnrRanking: Int? = null,

    @SerialName("meta_ogp")
    val metaOgp: MetaOgp? = null,

    @SerialName("twitter_card")
    val twitterCard: TwitterCard? = null,

    @SerialName("ranking_header")
    val rankingHeader: RankingHeader? = null
)

@Serializable
data class MetaOgp(
    @SerialName("description")
    val description: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("title")
    val title: String? = null
)

@Serializable
data class TwitterCard(
    @SerialName("card")
    val card: String? = null,
    @SerialName("site")
    val site: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("title")
    val title: String? = null
)

@Serializable
data class RankingHeader(
    @SerialName("general")
    val general: List<RankingHeaderItem>? = null,
    @SerialName("r18")
    val r18: List<RankingHeaderItem>? = null
)

@Serializable
data class RankingHeaderItem(
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("url")
    val url: String? = null
)

/**
 * 小说排行榜项
 */
@Serializable
data class NovelRankingItem(
    @SerialName("rank")
    val rank: Int,
    
    @SerialName("id")
    val id: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("create_date")
    val createDate: String? = null,
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("user_name")
    val userName: String,
    
    @SerialName("profile_img")
    val profileImg: String,
    
    @SerialName("comment")
    val comment: String,
    
    @SerialName("restrict")
    val restrict: String,
    
    @SerialName("x_restrict")
    val xRestrict: String,
    
    @SerialName("is_original")
    val isOriginal: String,
    
    @SerialName("language")
    val language: String,
    
    @SerialName("character_count")
    val characterCount: Int,
    
    @SerialName("word_count")
    val wordCount: Int,
    
    @SerialName("ai_type")
    val aiType: String,
    
    @SerialName("tag_a")
    val tagA: List<String>,
    
    @SerialName("url")
    val url: String,
    
    @SerialName("series_id")
    val seriesId: Long? = null,
    
    @SerialName("series_title")
    val seriesTitle: String? = null,
    
    @SerialName("genre")
    val genre: String,
    
    @SerialName("bookmark_count")
    val bookmarkCount: Int,
    
    @SerialName("reading_time")
    val readingTime: Int,
    
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,
    
    @SerialName("bookmarkable")
    val bookmarkable: Boolean,
    
    @SerialName("marker")
    val marker: String? = null
)
