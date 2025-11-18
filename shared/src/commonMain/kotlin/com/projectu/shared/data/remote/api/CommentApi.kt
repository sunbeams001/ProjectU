package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
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
 * 评论响应体
 */
@Serializable
data class CommentsBody(
    val comments: List<CommentReply> = emptyList(),
    @SerialName("hasNext") val hasNext: Boolean = false
)

/**
 * 评论回复
 */
@Serializable
data class CommentReply(
    val id: String,
    val userId: String,
    val userName: String,
    val isDeletedUser: Boolean = false,  // 仅在根评论中存在
    val img: String? = null,
    val comment: String? = null,
    val stampId: String? = null,
    val stampLink: String? = null,       // 表情链接（仅在回复中）
    val commentDate: String,
    val commentRootId: String? = null,   // 根评论ID（仅在回复中）
    val commentParentId: String? = null,
    val commentUserId: String,
    val replyToUserId: String? = null,   // 回复目标用户ID（仅在回复中）
    val replyToUserName: String? = null, // 回复目标用户名（仅在回复中）
    val editable: Boolean = false,
    val hasReplies: Boolean = false      // 是否有回复（仅在根评论中）
)

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
    data class Success(val commentId: String) : PostCommentResult()  // 改为 String
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

/**
 * 评论 API
 * 提供插画和小说的评论功能，包括获取评论、发表评论、删除评论等
 */
class CommentApi(private val client: PixivApiClient) {

    // ==================== 插画评论 ====================

    /**
     * 获取插画评论根楼层
     * @param illustId 作品ID
     * @param offset 偏移量
     * @param limit 返回数量
     */
    suspend fun getIllustCommentRoots(
        illustId: Long,
        offset: Int = 0,
        limit: Int = 20
    ): PixivResponse<CommentsBody> {
        return client.get("/ajax/illusts/comments/roots", mapOf(
            "illust_id" to illustId,
            "offset" to offset,
            "limit" to limit
        ))
    }

    /**
     * 获取评论的回复
     * @param commentId 评论ID
     * @param page 页码
     */
    suspend fun getCommentReplies(
        commentId: String,
        page: Int = 1
    ): PixivResponse<CommentsBody> {
        return client.get("/ajax/illusts/comments/replies", mapOf(
            "comment_id" to commentId,
            "page" to page
        ))
    }

    /**
     * 发表插画评论
     * @param illustId 作品ID
     * @param userId 用户ID
     * @param comment 评论内容（可选，与stampId二选一）
     * @param stampId 表情ID（可选，与comment二选一）
     * @param parentCommentId 父评论ID（可选，回复评论时使用）
     * @return 成功时返回评论ID，失败时返回错误信息
     */
    suspend fun postIllustComment(
        illustId: Long,
        userId: Long,
        comment: String? = null,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ): PostCommentResult {
        val params = mutableMapOf<String, String>(
            "illust_id" to illustId.toString(),
            "author_user_id" to userId.toString(),
            "type" to if (stampId != null) "stamp" else "comment"
        )
        comment?.let { params["comment"] = it }
        stampId?.let { params["stamp_id"] = it.toString() }
        parentCommentId?.let { params["parent_id"] = it.toString() }

        return try {
            val responseWithRaw = client.postFormWithRaw<PostCommentBody>("/rpc/post_comment.php", params)
            val response = responseWithRaw.response
            if (response.error == false && response.body != null) {
                PostCommentResult.Success(response.body.commentId)
            } else {
                PostCommentResult.Error(response.message)
            }
        } catch (e: Exception) {
            // 如果反序列化失败（body 是空数组 []），尝试解析原始 JSON 获取错误信息
            // 原始响应格式: {"error":true,"message":"不正确的请求。","body":[]}
            try {
                // 使用正则表达式提取 message 字段
                val messagePattern = """"message":"([^"]+)"""".toRegex()
                val matchResult = messagePattern.find(e.message ?: "")
                val errorMessage = matchResult?.groupValues?.get(1) ?: "发表评论失败"
                PostCommentResult.Error(errorMessage)
            } catch (ex: Exception) {
                PostCommentResult.Error("发表评论失败")
            }
        }
    }

    /**
     * 删除插画评论
     * @param illustId 作品ID
     * @param commentId 评论ID
     * @return 成功时返回 Success，失败时返回 Error
     */
    suspend fun deleteIllustComment(
        illustId: Long,
        commentId: Long
    ): DeleteCommentResult {
        val params = mapOf(
            "i_id" to illustId.toString(),
            "del_id" to commentId.toString()
        )
        
        return try {
            val responseWithRaw = client.postFormWithRaw<String>("/rpc_delete_comment.php", params)
            val response = responseWithRaw.response
            if (response.error == false) {
                DeleteCommentResult.Success
            } else {
                DeleteCommentResult.Error(response.message)
            }
        } catch (e: Exception) {
            // 如果反序列化失败（body 是空数组 []），检查是否成功
            // 成功响应格式: {"error":false,"message":"ok","body":[]}
            try {
                val messagePattern = """"error":\s*(false)""".toRegex()
                val matchResult = messagePattern.find(e.message ?: "")
                if (matchResult != null) {
                    DeleteCommentResult.Success
                } else {
                    val errorPattern = """"message":"([^"]+)"""".toRegex()
                    val errorMatch = errorPattern.find(e.message ?: "")
                    val errorMessage = errorMatch?.groupValues?.get(1) ?: "删除评论失败"
                    DeleteCommentResult.Error(errorMessage)
                }
            } catch (ex: Exception) {
                DeleteCommentResult.Error("删除评论失败")
            }
        }
    }

    // ==================== 小说评论 ====================

    /**
     * 获取小说评论根楼层
     * @param novelId 小说ID
     * @param offset 偏移量
     * @param limit 返回数量
     */
    suspend fun getNovelCommentRoots(
        novelId: Long,
        offset: Int = 0,
        limit: Int = 20
    ): PixivResponse<CommentsBody> {
        return client.get("/ajax/novels/comments/roots", mapOf(
            "novel_id" to novelId,
            "offset" to offset,
            "limit" to limit
        ))
    }

    /**
     * 获取小说评论的回复
     * @param commentId 评论ID
     * @param page 页码
     */
    suspend fun getNovelCommentReplies(
        commentId: String,
        page: Int = 1
    ): PixivResponse<CommentsBody> {
        return client.get("/ajax/novels/comments/replies", mapOf(
            "comment_id" to commentId,
            "page" to page
        ))
    }

    /**
     * 发表小说评论
     * @param novelId 小说ID
     * @param userId 用户ID
     * @param comment 评论内容（可选，与stampId二选一）
     * @param stampId 表情ID（可选，与comment二选一）
     * @param parentCommentId 父评论ID（可选，回复评论时使用）
     * @return 成功时返回评论ID，失败时返回错误信息
     */
    suspend fun postNovelComment(
        novelId: Long,
        userId: Long,
        comment: String? = null,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ): PostCommentResult {
        val params = mutableMapOf<String, String>(
            "novel_id" to novelId.toString(),
            "author_user_id" to userId.toString(),
            "type" to if (stampId != null) "stamp" else "comment"
        )
        comment?.let { params["comment"] = it }
        stampId?.let { params["stamp_id"] = it.toString() }
        parentCommentId?.let { params["parent_id"] = it.toString() }

        return try {
            val responseWithRaw = client.postFormWithRaw<PostCommentBody>("/novel/rpc/post_comment.php", params)
            val response = responseWithRaw.response
            if (response.error == false && response.body != null) {
                PostCommentResult.Success(response.body.commentId)
            } else {
                PostCommentResult.Error(response.message)
            }
        } catch (e: Exception) {
            // 如果反序列化失败（body 是空数组 []），尝试解析原始 JSON 获取错误信息
            // 原始响应格式: {"error":true,"message":"不正确的请求。","body":[]}
            try {
                // 使用正则表达式提取 message 字段
                val messagePattern = """"message":"([^"]+)"""".toRegex()
                val matchResult = messagePattern.find(e.message ?: "")
                val errorMessage = matchResult?.groupValues?.get(1) ?: "发表评论失败"
                PostCommentResult.Error(errorMessage)
            } catch (ex: Exception) {
                PostCommentResult.Error("发表评论失败")
            }
        }
    }
    /**
     * 删除小说评论
     * @param novelId 小说ID
     * @param commentId 评论ID
     * @return 成功时返回 Success，失败时返回 Error
     */
    suspend fun deleteNovelComment(
        novelId: Long,
        commentId: Long
    ): DeleteCommentResult {
        val params = mapOf(
            "i_id" to novelId.toString(),
            "del_id" to commentId.toString()
        )
        
        return try {
            val responseWithRaw = client.postFormWithRaw<String>("/novel/rpc_delete_comment.php", params)
            val response = responseWithRaw.response
            if (response.error == false) {
                DeleteCommentResult.Success
            } else {
                DeleteCommentResult.Error(response.message)
            }
        } catch (e: Exception) {
            // 如果反序列化失败（body 是空数组 []），检查是否成功
            // 成功响应格式: {"error":false,"message":"ok","body":[]}
            try {
                val messagePattern = """"error":\s*(false)""".toRegex()
                val matchResult = messagePattern.find(e.message ?: "")
                if (matchResult != null) {
                    DeleteCommentResult.Success
                } else {
                    val errorPattern = """"message":"([^"]+)"""".toRegex()
                    val errorMatch = errorPattern.find(e.message ?: "")
                    val errorMessage = errorMatch?.groupValues?.get(1) ?: "删除评论失败"
                    DeleteCommentResult.Error(errorMessage)
                }
            } catch (ex: Exception) {
                DeleteCommentResult.Error("删除评论失败")
            }
        }
    }
}
