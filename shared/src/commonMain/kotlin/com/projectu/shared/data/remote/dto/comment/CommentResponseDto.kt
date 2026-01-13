package com.projectu.shared.data.remote.dto.comment

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * 灵活的字符串序列化器，可以处理 String/Int/Long/Boolean/null
 */
@OptIn(ExperimentalSerializationApi::class)
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        } else {
            encoder.encodeNull()
        }
    }
    
    override fun deserialize(decoder: Decoder): String? {
        return when (decoder) {
            is JsonDecoder -> {
                val element = decoder.decodeJsonElement()
                when (element) {
                    is JsonPrimitive -> {
                        when {
                            element.isString -> element.content
                            element.intOrNull != null -> element.content
                            element.longOrNull != null -> element.content
                            element.booleanOrNull != null -> null  // 将 false/true 转换为 null
                            else -> null
                        }
                    }
                    else -> null
                }
            }
            else -> decoder.decodeString()
        }
    }
}

/**
 * 发表评论响应
 */
@Serializable
data class PostCommentBody(
    @SerialName("comment_id") val commentId: String,
    @SerialName("comment") val comment: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("stamp_id") val stampId: String? = null,  // 可能是字符串 "303" 或 null
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("parent_id") val parentId: String? = null  // 可能是字符串、null 或 false
)

/**
 * 发表评论响应结果
 * 成功时返回 PostCommentBody，失败时返回空字符串
 */
sealed class PostCommentResult {
    data class Success(val commentId: String) : PostCommentResult()
    data class Error(val message: String) : PostCommentResult()
}

/**
 * 删除评论响应结果
 * 成功时返回 true，失败时返回错误信息
 */
sealed class DeleteCommentResult {
    object Success : DeleteCommentResult()
    data class Error(val message: String) : DeleteCommentResult()
}
