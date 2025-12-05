package com.projectu.shared.data.remote.dto.comment

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
    val commentUserId: String? = null,   // 评论用户ID（回复API中存在，小说根评论API不存在）
    val replyToUserId: String? = null,   // 回复目标用户ID（仅在回复中）
    val replyToUserName: String? = null, // 回复目标用户名（仅在回复中）
    val editable: Boolean = false,
    val hasReplies: Boolean = false      // 是否有回复（仅在根评论中）
)
