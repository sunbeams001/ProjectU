package com.projectu.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * 评论领域模型
 */
data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userProfileImageUrl: String?,
    val comment: String?,  // 文字评论内容，为 null 时表示表情评论
    val stampId: String?,  // 表情 ID
    val stampLink: String?,  // 表情链接
    val commentDate: String,
    val isEditable: Boolean,  // 当前用户是否可以编辑（删除）此评论
    val hasReplies: Boolean,  // 是否有回复
    val isDeletedUser: Boolean,  // 用户是否已被删除
    // 回复相关字段
    val commentRootId: String?,  // 根评论 ID
    val commentParentId: String?,  // 父评论 ID
    val replyToUserId: String?,  // 被回复用户 ID
    val replyToUserName: String?  // 被回复用户名
)

/**
 * 评论列表响应
 */
data class CommentListResult(
    val comments: List<Comment>,
    val hasNext: Boolean
)

/**
 * 评论内容类型
 */
@Serializable
enum class CommentContentType {
    ILLUST,  // 插画/漫画/动图
    NOVEL    // 小说
}

/**
 * 发表评论结果
 */
sealed class PostCommentResultModel {
    data class Success(val commentId: String) : PostCommentResultModel()
    data class Error(val message: String) : PostCommentResultModel()
}

/**
 * 删除评论结果
 */
sealed class DeleteCommentResultModel {
    data object Success : DeleteCommentResultModel()
    data class Error(val message: String) : DeleteCommentResultModel()
}
