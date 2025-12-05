package com.projectu.ui.screens.comment

import com.projectu.shared.domain.model.Comment
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.model.Emoji
import com.projectu.shared.domain.model.Stamp

/**
 * 评论项，包含根评论和已加载的回复
 */
data class CommentItem(
    val comment: Comment,
    val replies: List<Comment> = emptyList(),
    val isLoadingReplies: Boolean = false,
    val isExpanded: Boolean = false,  // 是否展开回复
    val hasMoreReplies: Boolean = false,  // 是否还有更多回复
    val repliesPage: Int = 1  // 已加载的回复页码
)

/**
 * 回复目标
 */
data class ReplyTarget(
    val commentId: Long,  // 回复的评论 ID
    val userName: String  // 被回复用户名，用于显示
)

/**
 * 评论页面状态
 */
data class CommentsScreenState(
    // 内容信息
    val contentId: String = "",
    val contentType: CommentContentType = CommentContentType.ILLUST,
    val contentTitle: String = "",  // 作品标题
    
    // 评论列表
    val comments: List<CommentItem> = emptyList(),
    
    // 加载状态
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    
    // 分页信息
    val offset: Int = 0,
    
    // 错误信息
    val error: String? = null,
    
    // 发表评论相关
    val commentInput: String = "",
    val isPosting: Boolean = false,
    val postError: String? = null,
    val replyTarget: ReplyTarget? = null,  // 回复目标，null 表示回复作品
    
    // 删除评论相关
    val isDeletingCommentId: String? = null,  // 正在删除的评论 ID
    val deleteError: String? = null,
    
    // 表情选择器相关
    val isEmojiPickerVisible: Boolean = false  // 表情选择器是否显示
) {
    /**
     * 是否正在回复某条评论
     */
    val isReplying: Boolean
        get() = replyTarget != null
    
    /**
     * 获取输入框提示文字的 key
     */
    val inputHintKey: String
        get() = if (replyTarget != null) "comment_reply_hint" else "comment_input_hint"
}

/**
 * 评论页面 Intent
 */
sealed interface CommentsIntent {
    /** 初始化页面 */
    data class Initialize(
        val contentId: String,
        val contentType: CommentContentType,
        val contentTitle: String
    ) : CommentsIntent
    
    /** 刷新评论列表 */
    data object Refresh : CommentsIntent
    
    /** 加载更多评论 */
    data object LoadMore : CommentsIntent
    
    /** 展开/收起评论回复 */
    data class ToggleReplies(val commentId: String) : CommentsIntent
    
    /** 加载更多回复 */
    data class LoadMoreReplies(val commentId: String) : CommentsIntent
    
    /** 更新评论输入 */
    data class UpdateCommentInput(val text: String) : CommentsIntent
    
    /** 设置回复目标 */
    data class SetReplyTarget(val target: ReplyTarget?) : CommentsIntent
    
    /** 发表评论 */
    data object PostComment : CommentsIntent
    
    /** 删除评论 */
    data class DeleteComment(val commentId: String) : CommentsIntent
    
    /** 清除错误 */
    data object ClearError : CommentsIntent
    
    /** 显示/隐藏表情选择器 */
    data class SetEmojiPickerVisible(val visible: Boolean) : CommentsIntent
    
    /** 插入 emoji 到输入框 */
    data class InsertEmoji(val emoji: Emoji) : CommentsIntent
    
    /** 发送 stamp 评论 */
    data class PostStampComment(val stamp: Stamp) : CommentsIntent
}
