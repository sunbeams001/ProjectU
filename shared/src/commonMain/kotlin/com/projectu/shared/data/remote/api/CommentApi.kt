package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val comment: String,
    @SerialName("stampId") val stampId: String? = null,
    @SerialName("commentUserId") val commentUserId: String,
    @SerialName("commentUserName") val commentUserName: String,
    @SerialName("commentDate") val commentDate: String,
    @SerialName("hasReplies") val hasReplies: Boolean = false,
    @SerialName("editable") val editable: Boolean = false
)

/**
 * 发表评论响应
 */
@Serializable
data class PostCommentBody(
    @SerialName("comment_id") val commentId: Long
)

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
     */
    suspend fun postIllustComment(
        illustId: Long,
        userId: Long,
        comment: String? = null,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ): PixivResponse<PostCommentBody> {
        val params = mutableMapOf<String, String>(
            "illust_id" to illustId.toString(),
            "user_id" to userId.toString()
        )
        comment?.let { params["comment"] = it }
        stampId?.let { params["stamp_id"] = it.toString() }
        parentCommentId?.let { params["parent_id"] = it.toString() }

        return client.postForm("/rpc/post_comment.php", params)
    }

    /**
     * 删除插画评论
     * @param illustId 作品ID
     * @param commentId 评论ID
     */
    suspend fun deleteIllustComment(
        illustId: Long,
        commentId: Long
    ): PixivResponse<String> {
        return client.postForm("/rpc_delete_comment.php", mapOf(
            "i_id" to illustId.toString(),
            "del_id" to commentId.toString()
        ))
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
     * 发表小说评论
     * @param novelId 小说ID
     * @param userId 用户ID
     * @param comment 评论内容（可选，与stampId二选一）
     * @param stampId 表情ID（可选，与comment二选一）
     * @param parentCommentId 父评论ID（可选，回复评论时使用）
     */
    suspend fun postNovelComment(
        novelId: Long,
        userId: Long,
        comment: String? = null,
        stampId: Int? = null,
        parentCommentId: Long? = null
    ): PixivResponse<PostCommentBody> {
        val params = mutableMapOf<String, String>(
            "novel_id" to novelId.toString(),
            "user_id" to userId.toString()
        )
        comment?.let { params["comment"] = it }
        stampId?.let { params["stamp_id"] = it.toString() }
        parentCommentId?.let { params["parent_id"] = it.toString() }

        return client.postForm("/rpc/post_comment.php", params)
    }

    /**
     * 删除小说评论
     * @param novelId 小说ID
     * @param commentId 评论ID
     */
    suspend fun deleteNovelComment(
        novelId: Long,
        commentId: Long
    ): PixivResponse<String> {
        return client.postForm("/rpc_delete_comment.php", mapOf(
            "n_id" to novelId.toString(),
            "del_id" to commentId.toString()
        ))
    }
}
