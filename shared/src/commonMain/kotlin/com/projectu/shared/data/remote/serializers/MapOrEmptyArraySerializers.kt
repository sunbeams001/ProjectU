package com.projectu.shared.data.remote.serializers

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
