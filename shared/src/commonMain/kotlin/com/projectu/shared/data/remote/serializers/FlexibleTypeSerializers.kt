package com.projectu.shared.data.remote.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * 处理 Pixiv API 中字段类型不一致的情况
 * 
 * 某些被删除/隐藏的作品会返回异常类型的值：
 * - genre: 正常返回 "fantasy"，被删除作品返回 0 (整数)
 * - userId: 正常返回 "12345"，被删除作品返回 0 (整数)
 * - bookmarkCount: 正常返回 100，被删除作品返回 null
 */

/**
 * 灵活的字符串序列化器
 * 
 * 可以处理：
 * - 字符串值: "fantasy" -> "fantasy"
 * - 整数值: 0 -> "0"
 * - null 值: null -> ""
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.intOrNull != null -> element.intOrNull.toString()
                    else -> element.content
                }
            }
            else -> ""
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

/**
 * 可空的灵活整数序列化器
 * 
 * 可以处理：
 * - 整数值: 100 -> 100
 * - null 值: null -> null
 * - 字符串数字: "100" -> 100
 */
object FlexibleIntOrNullSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleIntOrNull", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return try { decoder.decodeInt() } catch (e: Exception) { null }
        
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                element.intOrNull ?: element.content.toIntOrNull()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value != null) {
            encoder.encodeInt(value)
        } else {
            encoder.encodeNull()
        }
    }
}
