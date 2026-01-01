package com.projectu.shared.data.remote.dto.novel

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * 小说推荐响应体（用于 getRecommendNovels）
 */
@Serializable
data class NovelRecommendBody(
    val novels: List<NovelSimple>
)

/**
 * 小说推荐初始化响应体（用于 getRecommendInit）
 */
@Serializable
data class NovelRecommendInitBody(
    val novels: List<NovelSimple>,
    val nextIds: List<String>,
    @Serializable(with = RecommendMetadataMapSerializer::class)
    val details: Map<String, RecommendMetadata>? = null
)

/**
 * 推荐元数据
 */
@Serializable
data class RecommendMetadata(
    val methods: List<String>,
    val score: Double,
    val seed_novel_ids: List<String>,
    val seed_illust_ids: List<String>? = null,
    val position: Int
)

/**
 * 推荐元数据Map的自定义序列化器
 * API返回的details字段中，值是JSON字符串而不是对象，需要二次解析
 */
object RecommendMetadataMapSerializer : KSerializer<Map<String, RecommendMetadata>> {
    private val delegateSerializer = MapSerializer(String.serializer(), String.serializer())
    private val json = Json { ignoreUnknownKeys = true }
    
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor
    
    override fun deserialize(decoder: Decoder): Map<String, RecommendMetadata> {
        // 先解析为 Map<String, String>
        val stringMap = delegateSerializer.deserialize(decoder)
        
        // 然后将每个字符串值解析为 RecommendMetadata 对象
        return stringMap.mapValues { (_, jsonString) ->
            json.decodeFromString(RecommendMetadata.serializer(), jsonString)
        }
    }
    
    override fun serialize(encoder: Encoder, value: Map<String, RecommendMetadata>) {
        // 序列化时将对象转换为JSON字符串
        val stringMap = value.mapValues { (_, metadata) ->
            json.encodeToString(RecommendMetadata.serializer(), metadata)
        }
        delegateSerializer.serialize(encoder, stringMap)
    }
}
