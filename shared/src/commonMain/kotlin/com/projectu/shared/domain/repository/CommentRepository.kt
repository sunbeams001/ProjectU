package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Comment
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.model.CommentListResult
import com.projectu.shared.domain.model.DeleteCommentResultModel
import com.projectu.shared.domain.model.PostCommentResultModel

/**
 * 评论仓储接口
 */
interface CommentRepository {
    
    /**
     * 获取根级评论（一层评论）
     * @param contentId 内容ID（插画ID或小说ID）
     * @param contentType 内容类型
     * @param offset 偏移量
     * @param limit 返回数量
     */
    suspend fun getRootComments(
        contentId: Long,
        contentType: CommentContentType,
        offset: Int = 0,
        limit: Int = 20
    ): Result<CommentListResult>
    
    /**
     * 获取评论的回复（嵌套评论）
     * @param commentId 评论ID
     * @param contentType 内容类型
     * @param page 页码
     */
    suspend fun getCommentReplies(
        commentId: String,
        contentType: CommentContentType,
        page: Int = 1
    ): Result<CommentListResult>
    
    /**
     * 发表评论
     * @param contentId 内容ID（插画ID或小说ID）
     * @param contentType 内容类型
     * @param authorUserId 作品作者用户ID
     * @param comment 评论内容
     * @param parentCommentId 父评论ID（可选，回复评论时使用）
     */
    suspend fun postComment(
        contentId: Long,
        contentType: CommentContentType,
        authorUserId: Long,
        comment: String,
        parentCommentId: Long? = null
    ): PostCommentResultModel
    
    /**
     * 发表表情评论
     * @param contentId 内容ID（插画ID或小说ID）
     * @param contentType 内容类型
     * @param authorUserId 作品作者用户ID
     * @param stampId 表情ID
     * @param parentCommentId 父评论ID（可选，回复评论时使用）
     */
    suspend fun postStampComment(
        contentId: Long,
        contentType: CommentContentType,
        authorUserId: Long,
        stampId: Int,
        parentCommentId: Long? = null
    ): PostCommentResultModel
    
    /**
     * 删除评论
     * @param contentId 内容ID（插画ID或小说ID）
     * @param contentType 内容类型
     * @param commentId 评论ID
     */
    suspend fun deleteComment(
        contentId: Long,
        contentType: CommentContentType,
        commentId: Long
    ): DeleteCommentResultModel
}
