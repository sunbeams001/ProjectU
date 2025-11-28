package com.projectu.shared.data.remote.dto.ranking

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * 排行榜响应
 */
@Serializable
data class RankingResponse(
    val contents: List<RankingContent>,
    val mode: String,
    val content: String,
    val page: Int,
    @Serializable(with = BooleanOrIntSerializer::class)
    val prev: Int? = null,
    @Serializable(with = BooleanOrIntSerializer::class)
    val next: Int? = null,
    val date: String,
    @Serializable(with = BooleanOrStringSerializer::class)
    val prev_date: String? = null,
    @Serializable(with = BooleanOrStringSerializer::class)
    val next_date: String? = null,
    val rank_total: Int
)

@Serializable
data class RankingContent(
    val title: String,
    val date: String,
    val tags: List<String>,
    val url: String,
    val illust_type: String,
    val illust_book_style: String,
    val illust_page_count: String,
    val user_name: String,
    val profile_img: String,
    val illust_content_type: RankingContentType,
    @Serializable(with = BooleanOrSeriesSerializer::class)
    val illust_series: IllustSeries? = null,
    val illust_id: Long,
    val width: Int,
    val height: Int,
    val user_id: Long,
    val rank: Int,
    val yes_rank: Int = 0,
    val rating_count: Int,
    val view_count: Int,
    val illust_upload_timestamp: Long,
    val attr: String = "",
    val is_masked: Boolean = false,
    val is_bookmarked: Boolean = false,
    val bookmarkable: Boolean = true,
    val bookmark_id: String? = null,
    val bookmark_illust_restrict: String? = null
)

@Serializable
data class RankingContentType(
    val sexual: Int = 0,
    val lo: Boolean = false,
    val grotesque: Boolean = false,
    val violent: Boolean = false,
    val homosexual: Boolean = false,
    val drug: Boolean = false,
    val thoughts: Boolean = false,
    val antisocial: Boolean = false,
    val religion: Boolean = false,
    val original: Boolean = false,
    val furry: Boolean = false,
    val bl: Boolean = false,
    val yuri: Boolean = false
)

// 自定义序列化器：处理 Boolean 或 Int 的情况
object BooleanOrIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BooleanOrInt")

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This serializer can only be used with Json")
        val element = jsonDecoder.decodeJsonElement()
        
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> null
                    element.booleanOrNull == false -> null
                    else -> element.intOrNull
                }
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) {
            encoder.encodeBoolean(false)
        } else {
            encoder.encodeInt(value)
        }
    }
}

// 自定义序列化器：处理 Boolean 或 String 的情况
object BooleanOrStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BooleanOrString")

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This serializer can only be used with Json")
        val element = jsonDecoder.decodeJsonElement()
        
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.booleanOrNull == false -> null
                    else -> element.contentOrNull
                }
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeBoolean(false)
        } else {
            encoder.encodeString(value)
        }
    }
}

// 自定义序列化器：处理 Boolean 或 Object 的情况
object BooleanOrSeriesSerializer : KSerializer<IllustSeries?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BooleanOrSeries")

    override fun deserialize(decoder: Decoder): IllustSeries? {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("This serializer can only be used with Json")
        val element = jsonDecoder.decodeJsonElement()
        
        return when (element) {
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(IllustSeries.serializer(), element)
            is JsonPrimitive -> if (element.booleanOrNull == false) null else null
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: IllustSeries?) {
        if (value == null) {
            encoder.encodeBoolean(false)
        } else {
            encoder.encodeSerializableValue(IllustSeries.serializer(), value)
        }
    }
}
