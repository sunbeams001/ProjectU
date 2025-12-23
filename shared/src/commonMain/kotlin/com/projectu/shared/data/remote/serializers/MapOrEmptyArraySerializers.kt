package com.projectu.shared.data.remote.serializers

import com.projectu.shared.util.AppJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * 处理 Pixiv API 返回值不一致的情况：
 * - 有数据时返回 Map 对象: {"key": "value"}
 * - 无数据时返回空数组: []
 * 
 * 此序列化器将空数组统一转换为空 Map，保证类型一致性
 */
object MapOrEmptyArraySerializer : KSerializer<Map<String, String?>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MapOrEmptyArray")

    override fun deserialize(decoder: Decoder): Map<String, String?>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, value) -> 
                    if (value is JsonNull) null else value.jsonPrimitive.contentOrNull
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String?>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as JsonEncoder
            val jsonObject = buildJsonObject {
                value.forEach { (key, v) ->
                    put(key, if (v == null) JsonNull else JsonPrimitive(v))
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

/**
 * 处理嵌套 Map 的情况：Map<String, Map<String, String>>
 * - 有数据时返回嵌套对象: {"outer": {"inner": "value"}}
 * - 无数据时返回空数组: []
 */
object NestedMapOrEmptyArraySerializer : KSerializer<Map<String, Map<String, String>>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NestedMapOrEmptyArray")

    override fun deserialize(decoder: Decoder): Map<String, Map<String, String>>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, outerValue) -> 
                    when (outerValue) {
                        is JsonObject -> {
                            outerValue.mapValues { (_, innerValue) ->
                                innerValue.jsonPrimitive.content
                            }
                        }
                        else -> emptyMap()
                    }
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, Map<String, String>>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as JsonEncoder
            val jsonObject = buildJsonObject {
                value.forEach { (outerKey, innerMap) ->
                    put(outerKey, buildJsonObject {
                        innerMap.forEach { (innerKey, innerValue) ->
                            put(innerKey, JsonPrimitive(innerValue))
                        }
                    })
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

/**
 * 处理 SocialLinks 对象的情况：
 * - 有数据时返回对象: {"twitter": {"url": "..."}}
 * - 无数据时返回空数组: []
 * 
 * 此序列化器将空数组转换为 null，对象则正常解析为 SocialLinks
 */
object SocialLinksOrEmptyArraySerializer : KSerializer<com.projectu.shared.data.remote.dto.user.SocialLinks?> {
    private val delegateSerializer = com.projectu.shared.data.remote.dto.user.SocialLinks.serializer()
    
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SocialLinksOrEmptyArray")

    override fun deserialize(decoder: Decoder): com.projectu.shared.data.remote.dto.user.SocialLinks? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                // 对象类型，使用全局统一的 json 配置解析
                AppJson.decodeFromJsonElement(delegateSerializer, element)
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回 null
                null
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: com.projectu.shared.data.remote.dto.user.SocialLinks?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(delegateSerializer, value)
        }
    }
}

/**
 * 处理 bookmarkTags 的情况：Map<String, List<String>>
 * - 有数据时返回对象: {"tag": ["id1", "id2"]}
 * - 无数据时返回空数组: []
 */
object MapStringListOrEmptyArraySerializer : KSerializer<Map<String, List<String>>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MapStringListOrEmptyArray")

    override fun deserialize(decoder: Decoder): Map<String, List<String>>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, value) -> 
                    when (value) {
                        is JsonArray -> value.map { it.jsonPrimitive.content }
                        else -> emptyList()
                    }
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, List<String>>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as JsonEncoder
            val jsonObject = buildJsonObject {
                value.forEach { (key, list) ->
                    put(key, buildJsonArray {
                        list.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

/**
 * 处理 planTranslationTitle 的情况：
 * - 有数据时返回对象: {"en": {"planTitle": "...", "planTitleLang": "en"}, ...}
 * - 无数据时返回空数组: []
 */
object PlanTitleTranslationOrEmptyArraySerializer : KSerializer<Map<String, PlanTitleTranslationItem>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlanTitleTranslationOrEmptyArray")

    override fun deserialize(decoder: Decoder): Map<String, PlanTitleTranslationItem>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, value) -> 
                    when (value) {
                        is JsonObject -> PlanTitleTranslationItem(
                            planTitle = value["planTitle"]?.jsonPrimitive?.contentOrNull,
                            planTitleLang = value["planTitleLang"]?.jsonPrimitive?.contentOrNull
                        )
                        else -> PlanTitleTranslationItem()
                    }
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, PlanTitleTranslationItem>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as JsonEncoder
            val jsonObject = buildJsonObject {
                value.forEach { (key, item) ->
                    put(key, buildJsonObject {
                        item.planTitle?.let { put("planTitle", JsonPrimitive(it)) }
                        item.planTitleLang?.let { put("planTitleLang", JsonPrimitive(it)) }
                    })
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

/**
 * planTranslationTitle 中的项
 */
@kotlinx.serialization.Serializable
data class PlanTitleTranslationItem(
    val planTitle: String? = null,
    val planTitleLang: String? = null
)

/**
 * 处理用户搜索中的 workIds 字段：Map<String, List<UserWorkInfo>>
 * - 有数据时返回对象: {"userId": [{"id": "...", "type": "illust"}]}
 * - 无数据时返回空数组: []
 */
object StringToListUserWorkInfoSerializer : KSerializer<Map<String, List<com.projectu.shared.data.remote.dto.user.UserWorkInfo>>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringToListUserWorkInfo")

    override fun deserialize(decoder: Decoder): Map<String, List<com.projectu.shared.data.remote.dto.user.UserWorkInfo>> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyMap()
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, value) -> 
                    when (value) {
                        is JsonArray -> value.mapNotNull { item ->
                            if (item is JsonObject) {
                                try {
                                    AppJson.decodeFromJsonElement(
                                        com.projectu.shared.data.remote.dto.user.UserWorkInfo.serializer(),
                                        item
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                        }
                        else -> emptyList()
                    }
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> emptyMap()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, List<com.projectu.shared.data.remote.dto.user.UserWorkInfo>>) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonObject = buildJsonObject {
            value.forEach { (key, list) ->
                put(key, buildJsonArray {
                    list.forEach { workInfo ->
                        add(AppJson.encodeToJsonElement(
                            com.projectu.shared.data.remote.dto.user.UserWorkInfo.serializer(),
                            workInfo
                        ))
                    }
                })
            }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}
