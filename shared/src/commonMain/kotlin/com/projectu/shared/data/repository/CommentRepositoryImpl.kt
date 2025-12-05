package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.dto.comment.CommentReply
import com.projectu.shared.data.remote.dto.comment.DeleteCommentResult
import com.projectu.shared.data.remote.dto.comment.PostCommentResult
import com.projectu.shared.domain.model.Comment
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.model.CommentListResult
import com.projectu.shared.domain.model.DeleteCommentResultModel
import com.projectu.shared.domain.model.EmojiConfig
import com.projectu.shared.domain.model.PostCommentResultModel
import com.projectu.shared.domain.repository.CommentRepository

/**
 * 评论仓库实现
 */
class CommentRepositoryImpl(
    private val pixivApi: PixivApi
) : CommentRepository {
    
    override suspend fun getRootComments(
        contentId: Long,
        contentType: CommentContentType,
        offset: Int,
        limit: Int
    ): Result<CommentListResult> {
        return try {
            val response = when (contentType) {
                CommentContentType.ILLUST -> pixivApi.commentApi.getIllustCommentRoots(
                    illustId = contentId,
                    offset = offset,
                    limit = limit
                )
                CommentContentType.NOVEL -> pixivApi.commentApi.getNovelCommentRoots(
                    novelId = contentId,
                    offset = offset,
                    limit = limit
                )
            }
            
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "Failed to get comments"))
            } else {
                val comments = response.body!!.comments.map { it.toComment() }
                Result.success(CommentListResult(
                    comments = comments,
                    hasNext = response.body!!.hasNext
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCommentReplies(
        commentId: String,
        contentType: CommentContentType,
        page: Int
    ): Result<CommentListResult> {
        return try {
            val response = when (contentType) {
                CommentContentType.ILLUST -> pixivApi.commentApi.getCommentReplies(
                    commentId = commentId,
                    page = page
                )
                CommentContentType.NOVEL -> pixivApi.commentApi.getNovelCommentReplies(
                    commentId = commentId,
                    page = page
                )
            }
            
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "Failed to get comment replies"))
            } else {
                val comments = response.body!!.comments.map { it.toComment() }
                Result.success(CommentListResult(
                    comments = comments,
                    hasNext = response.body!!.hasNext
                ))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun postComment(
        contentId: Long,
        contentType: CommentContentType,
        authorUserId: Long,
        comment: String,
        parentCommentId: Long?
    ): PostCommentResultModel {
        return try {
            val result = when (contentType) {
                CommentContentType.ILLUST -> pixivApi.commentApi.postIllustComment(
                    illustId = contentId,
                    userId = authorUserId,
                    comment = comment,
                    parentCommentId = parentCommentId
                )
                CommentContentType.NOVEL -> pixivApi.commentApi.postNovelComment(
                    novelId = contentId,
                    userId = authorUserId,
                    comment = comment,
                    parentCommentId = parentCommentId
                )
            }
            
            when (result) {
                is PostCommentResult.Success -> PostCommentResultModel.Success(result.commentId)
                is PostCommentResult.Error -> PostCommentResultModel.Error(result.message)
            }
        } catch (e: Exception) {
            PostCommentResultModel.Error(e.message ?: "Failed to post comment")
        }
    }
    
    override suspend fun postStampComment(
        contentId: Long,
        contentType: CommentContentType,
        authorUserId: Long,
        stampId: Int,
        parentCommentId: Long?
    ): PostCommentResultModel {
        return try {
            val result = when (contentType) {
                CommentContentType.ILLUST -> pixivApi.commentApi.postIllustComment(
                    illustId = contentId,
                    userId = authorUserId,
                    stampId = stampId,
                    parentCommentId = parentCommentId
                )
                CommentContentType.NOVEL -> pixivApi.commentApi.postNovelComment(
                    novelId = contentId,
                    userId = authorUserId,
                    stampId = stampId,
                    parentCommentId = parentCommentId
                )
            }
            
            when (result) {
                is PostCommentResult.Success -> PostCommentResultModel.Success(result.commentId)
                is PostCommentResult.Error -> PostCommentResultModel.Error(result.message)
            }
        } catch (e: Exception) {
            PostCommentResultModel.Error(e.message ?: "Failed to post stamp comment")
        }
    }
    
    override suspend fun deleteComment(
        contentId: Long,
        contentType: CommentContentType,
        commentId: Long
    ): DeleteCommentResultModel {
        return try {
            val result = when (contentType) {
                CommentContentType.ILLUST -> pixivApi.commentApi.deleteIllustComment(
                    illustId = contentId,
                    commentId = commentId
                )
                CommentContentType.NOVEL -> pixivApi.commentApi.deleteNovelComment(
                    novelId = contentId,
                    commentId = commentId
                )
            }
            
            when (result) {
                is DeleteCommentResult.Success -> DeleteCommentResultModel.Success
                is DeleteCommentResult.Error -> DeleteCommentResultModel.Error(result.message)
            }
        } catch (e: Exception) {
            DeleteCommentResultModel.Error(e.message ?: "Failed to delete comment")
        }
    }
    
    /**
     * 将 DTO 转换为领域模型
     */
    private fun CommentReply.toComment(): Comment {
        // 如果 stampLink 为 null 但 stampId 存在，根据 stampId 生成 URL
        val resolvedStampLink = stampLink ?: stampId?.toIntOrNull()?.let { id ->
            EmojiConfig.getStampUrl(id)
        }
        
        return Comment(
            id = id,
            userId = userId,
            userName = userName,
            userProfileImageUrl = img,
            comment = comment,
            stampId = stampId,
            stampLink = resolvedStampLink,
            commentDate = commentDate,
            isEditable = editable,
            hasReplies = hasReplies,
            isDeletedUser = isDeletedUser,
            commentRootId = commentRootId,
            commentParentId = commentParentId,
            replyToUserId = replyToUserId,
            replyToUserName = replyToUserName
        )
    }
}
