package com.projectu.ui.screens.comment

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Comment
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.domain.model.DeleteCommentResultModel
import com.projectu.shared.domain.model.Emoji
import com.projectu.shared.domain.model.PostCommentResultModel
import com.projectu.shared.domain.model.Stamp
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.CommentRepository
import com.projectu.shared.domain.usecase.TranslateTextUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 评论页面 ViewModel
 */
class CommentsViewModel(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository,
    private val translateTextUseCase: TranslateTextUseCase,
    private val settingsCache: SettingsCache
) : ScreenModel {
    
    private val _state = MutableStateFlow(CommentsScreenState())
    val state: StateFlow<CommentsScreenState> = _state.asStateFlow()
    
    companion object {
        private const val PAGE_SIZE = 20
    }
    
    /**
     * 处理 Intent
     */
    fun handleIntent(intent: CommentsIntent) {
        when (intent) {
            is CommentsIntent.Initialize -> initialize(
                intent.contentId,
                intent.contentType,
                intent.contentTitle
            )
            is CommentsIntent.Refresh -> refresh()
            is CommentsIntent.LoadMore -> loadMore()
            is CommentsIntent.ToggleReplies -> toggleReplies(intent.commentId)
            is CommentsIntent.LoadMoreReplies -> loadMoreReplies(intent.commentId)
            is CommentsIntent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog(intent.comment)
            is CommentsIntent.CancelDelete -> cancelDelete()
            is CommentsIntent.UpdateCommentInput -> updateCommentInput(intent.text)
            is CommentsIntent.SetReplyTarget -> setReplyTarget(intent.target)
            is CommentsIntent.PostComment -> postComment()
            is CommentsIntent.DeleteComment -> deleteComment(intent.commentId)
            is CommentsIntent.ClearError -> clearError()
            is CommentsIntent.SetEmojiPickerVisible -> setEmojiPickerVisible(intent.visible)
            is CommentsIntent.InsertEmoji -> insertEmoji(intent.emoji)
            is CommentsIntent.PostStampComment -> postStampComment(intent.stamp)
            is CommentsIntent.TranslateComment -> translateComment(intent.commentId)
            is CommentsIntent.ClearCommentTranslation -> clearCommentTranslation(intent.commentId)
        }
    }
    
    /**
     * 初始化
     */
    private fun initialize(
        contentId: String,
        contentType: CommentContentType,
        contentTitle: String
    ) {
        // 如果已初始化相同内容，则跳过
        if (_state.value.contentId == contentId && _state.value.comments.isNotEmpty()) {
            return
        }
        
        _state.update {
            it.copy(
                contentId = contentId,
                contentType = contentType,
                contentTitle = contentTitle,
                isLoading = true,
                error = null
            )
        }
        
        loadComments(refresh = true)
    }
    
    /**
     * 刷新评论列表
     */
    private fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        loadComments(refresh = true)
    }
    
    /**
     * 加载更多评论
     */
    private fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMore) return
        
        _state.update { it.copy(isLoadingMore = true) }
        loadComments(refresh = false)
    }
    
    /**
     * 加载评论
     */
    private fun loadComments(refresh: Boolean) {
        val currentState = _state.value
        val offset = if (refresh) 0 else currentState.offset + currentState.comments.size
        val contentIdLong = currentState.contentId.toLongOrNull() ?: return
        
        screenModelScope.launch {
            commentRepository.getRootComments(
                contentId = contentIdLong,
                contentType = currentState.contentType,
                offset = offset,
                limit = PAGE_SIZE
            ).onSuccess { result ->
                _state.update { state ->
                    val newComments = result.comments.map { comment ->
                        CommentItem(
                            comment = comment,
                            replies = emptyList()
                        )
                    }
                    val updatedComments = if (refresh) {
                        newComments
                    } else {
                        val existingIds = state.comments.map { it.comment.id }.toSet()
                        val uniqueNew = newComments.filter { it.comment.id !in existingIds }
                        state.comments + uniqueNew
                    }
                    
                    state.copy(
                        comments = updatedComments,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasMore = result.hasNext,
                        offset = if (refresh) 0 else state.offset,
                        error = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = error.message ?: "Failed to load comments"
                    )
                }
            }
        }
    }
    
    /**
     * 展开/收起评论回复
     */
    private fun toggleReplies(commentId: String) {
        val currentState = _state.value
        val commentIndex = currentState.comments.indexOfFirst { it.comment.id == commentId }
        if (commentIndex < 0) return
        
        val commentItem = currentState.comments[commentIndex]
        
        if (commentItem.isExpanded) {
            // 收起
            _state.update { state ->
                state.copy(
                    comments = state.comments.toMutableList().apply {
                        this[commentIndex] = commentItem.copy(isExpanded = false)
                    }
                )
            }
        } else {
            // 展开
            if (commentItem.replies.isEmpty() && commentItem.comment.hasReplies) {
                // 需要加载回复
                _state.update { state ->
                    state.copy(
                        comments = state.comments.toMutableList().apply {
                            this[commentIndex] = commentItem.copy(
                                isExpanded = true,
                                isLoadingReplies = true
                            )
                        }
                    )
                }
                loadReplies(commentId, page = 1)
            } else {
                // 已有回复，直接展开
                _state.update { state ->
                    state.copy(
                        comments = state.comments.toMutableList().apply {
                            this[commentIndex] = commentItem.copy(isExpanded = true)
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 加载更多回复
     */
    private fun loadMoreReplies(commentId: String) {
        val currentState = _state.value
        val commentIndex = currentState.comments.indexOfFirst { it.comment.id == commentId }
        if (commentIndex < 0) return
        
        val commentItem = currentState.comments[commentIndex]
        if (commentItem.isLoadingReplies || !commentItem.hasMoreReplies) return
        
        _state.update { state ->
            state.copy(
                comments = state.comments.toMutableList().apply {
                    this[commentIndex] = commentItem.copy(isLoadingReplies = true)
                }
            )
        }
        
        loadReplies(commentId, commentItem.repliesPage + 1)
    }
    
    /**
     * 加载回复
     */
    private fun loadReplies(commentId: String, page: Int) {
        val currentState = _state.value
        
        screenModelScope.launch {
            commentRepository.getCommentReplies(
                commentId = commentId,
                contentType = currentState.contentType,
                page = page
            ).onSuccess { result ->
                _state.update { state ->
                    val commentIndex = state.comments.indexOfFirst { it.comment.id == commentId }
                    if (commentIndex < 0) return@update state
                    
                    val commentItem = state.comments[commentIndex]
                    val updatedReplies = if (page == 1) {
                        result.comments.map { ReplyItem(comment = it) }
                    } else {
                        val existingIds = commentItem.replies.map { it.comment.id }.toSet()
                        val uniqueNew = result.comments
                            .filter { it.id !in existingIds }
                            .map { ReplyItem(comment = it) }
                        commentItem.replies + uniqueNew
                    }
                    
                    state.copy(
                        comments = state.comments.toMutableList().apply {
                            this[commentIndex] = commentItem.copy(
                                replies = updatedReplies,
                                isLoadingReplies = false,
                                hasMoreReplies = result.hasNext,
                                repliesPage = page
                            )
                        }
                    )
                }
            }.onFailure { error ->
                _state.update { state ->
                    val commentIndex = state.comments.indexOfFirst { it.comment.id == commentId }
                    if (commentIndex < 0) return@update state
                    
                    val commentItem = state.comments[commentIndex]
                    state.copy(
                        comments = state.comments.toMutableList().apply {
                            this[commentIndex] = commentItem.copy(isLoadingReplies = false)
                        },
                        error = error.message
                    )
                }
            }
        }
    }
    
    /**
     * 更新评论输入
     */
    private fun updateCommentInput(text: String) {
        _state.update { it.copy(commentInput = text, postError = null) }
    }
    
    /**
     * 设置回复目标
     */
    private fun setReplyTarget(target: ReplyTarget?) {
        _state.update { it.copy(replyTarget = target, postError = null) }
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(comment: Comment) {
        _state.update { it.copy(commentToDelete = comment) }
    }
    
    /**
     * 取消删除
     */
    private fun cancelDelete() {
        _state.update { it.copy(commentToDelete = null) }
    }
    
    /**
     * 发表评论
     */
    private fun postComment() {
        val currentState = _state.value
        val comment = currentState.commentInput.trim()
        
        if (comment.isEmpty()) {
            _state.update { it.copy(postError = "comment_empty_error") }
            return
        }
        
        val contentIdLong = currentState.contentId.toLongOrNull() ?: return
        
        _state.update { it.copy(isPosting = true, postError = null) }
        
        screenModelScope.launch {
            // 获取当前登录用户ID
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _state.update { it.copy(isPosting = false, postError = "login_required") }
                return@launch
            }
            
            val result = commentRepository.postComment(
                contentId = contentIdLong,
                contentType = currentState.contentType,
                authorUserId = currentUserId,
                comment = comment,
                parentCommentId = currentState.replyTarget?.commentId
            )
            
            when (result) {
                is PostCommentResultModel.Success -> {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            commentInput = "",
                            replyTarget = null,
                            postError = null
                        )
                    }
                    // 刷新评论列表以显示新评论
                    refresh()
                }
                is PostCommentResultModel.Error -> {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            postError = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 删除评论
     */
    private fun deleteComment(commentId: String) {
        val currentState = _state.value
        val contentIdLong = currentState.contentId.toLongOrNull() ?: return
        
        _state.update { it.copy(isDeletingCommentId = commentId, deleteError = null) }
        
        screenModelScope.launch {
            val result = commentRepository.deleteComment(
                contentId = contentIdLong,
                contentType = currentState.contentType,
                commentId = commentId.toLongOrNull() ?: return@launch
            )
            
            when (result) {
                is DeleteCommentResultModel.Success -> {
                    _state.update { state ->
                        // 从列表中移除评论
                        val updatedComments = state.comments.mapNotNull { item ->
                            if (item.comment.id == commentId) {
                                null
                            } else {
                                // 也检查回复中是否有被删除的评论
                                val updatedReplies = item.replies.filter { it.comment.id != commentId }
                                if (updatedReplies.size != item.replies.size) {
                                    item.copy(replies = updatedReplies)
                                } else {
                                    item
                                }
                            }
                        }
                        
                        state.copy(
                            comments = updatedComments,
                            isDeletingCommentId = null,
                            commentToDelete = null,
                            deleteError = null
                        )
                    }
                }
                is DeleteCommentResultModel.Error -> {
                    _state.update {
                        it.copy(
                            isDeletingCommentId = null,
                            commentToDelete = null,
                            deleteError = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        _state.update {
            it.copy(
                error = null,
                postError = null,
                deleteError = null
            )
        }
    }
    
    /**
     * 显示/隐藏表情选择器
     */
    private fun setEmojiPickerVisible(visible: Boolean) {
        _state.update { it.copy(isEmojiPickerVisible = visible) }
    }
    
    /**
     * 插入 emoji 到输入框
     */
    private fun insertEmoji(emoji: Emoji) {
        _state.update { 
            it.copy(commentInput = it.commentInput + emoji.label)
        }
    }
    
    /**
     * 发送 stamp 评论
     */
    private fun postStampComment(stamp: Stamp) {
        val currentState = _state.value
        val contentIdLong = currentState.contentId.toLongOrNull() ?: return
        
        _state.update { it.copy(isPosting = true, postError = null, isEmojiPickerVisible = false) }
        
        screenModelScope.launch {
            // 获取当前登录用户ID
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _state.update { it.copy(isPosting = false, postError = "login_required") }
                return@launch
            }
            
            val result = commentRepository.postStampComment(
                contentId = contentIdLong,
                contentType = currentState.contentType,
                authorUserId = currentUserId,
                stampId = stamp.id,
                parentCommentId = currentState.replyTarget?.commentId
            )
            
            when (result) {
                is PostCommentResultModel.Success -> {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            commentInput = "",
                            replyTarget = null,
                            postError = null
                        )
                    }
                    // 刷新评论列表以显示新评论
                    refresh()
                }
                is PostCommentResultModel.Error -> {
                    _state.update {
                        it.copy(
                            isPosting = false,
                            postError = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 翻译评论（包括主评论和回复）
     */
    private fun translateComment(commentId: String) {
        if (!settingsCache.isTranslationEnabled()) return
        
        val currentState = _state.value
        
        // 先检查是否是主评论
        val commentItem = currentState.comments.find { it.comment.id == commentId }
        val commentText = if (commentItem != null) {
            commentItem.comment.comment
        } else {
            // 如果不是主评论，在所有回复中查找
            var foundText: String? = null
            for (item in currentState.comments) {
                val reply = item.replies.find { it.comment.id == commentId }
                if (reply != null) {
                    foundText = reply.comment.comment
                    break
                }
            }
            foundText
        }
        
        // 只有包含文本内容的评论才能翻译
        if (commentText.isNullOrBlank()) return
        
        screenModelScope.launch {
            // 更新状态为翻译中
            _state.update { state ->
                state.copy(
                    comments = state.comments.map { item ->
                        if (item.comment.id == commentId) {
                            // 主评论
                            item.copy(
                                isTranslating = true,
                                translationError = null
                            )
                        } else {
                            // 检查回复
                            val updatedReplies = item.replies.map { reply ->
                                if (reply.comment.id == commentId) {
                                    reply.copy(
                                        isTranslating = true,
                                        translationError = null
                                    )
                                } else {
                                    reply
                                }
                            }
                            if (updatedReplies != item.replies) {
                                item.copy(replies = updatedReplies)
                            } else {
                                item
                            }
                        }
                    }
                )
            }
            
            try {
                val result = translateTextUseCase(
                    text = commentText,
                    targetLanguage = settingsCache.getTranslationTargetLanguage(),
                    engine = settingsCache.getTranslationEngine()
                )
                
                result.onSuccess { translation ->
                    _state.update { state ->
                        state.copy(
                            comments = state.comments.map { item ->
                                if (item.comment.id == commentId) {
                                    // 主评论
                                    item.copy(
                                        translatedText = translation.translatedText,
                                        isTranslating = false,
                                        translationError = null
                                    )
                                } else {
                                    // 检查回复
                                    val updatedReplies = item.replies.map { reply ->
                                        if (reply.comment.id == commentId) {
                                            reply.copy(
                                                translatedText = translation.translatedText,
                                                isTranslating = false,
                                                translationError = null
                                            )
                                        } else {
                                            reply
                                        }
                                    }
                                    if (updatedReplies != item.replies) {
                                        item.copy(replies = updatedReplies)
                                    } else {
                                        item
                                    }
                                }
                            }
                        )
                    }
                }.onFailure { error ->
                    _state.update { state ->
                        state.copy(
                            comments = state.comments.map { item ->
                                if (item.comment.id == commentId) {
                                    // 主评论
                                    item.copy(
                                        isTranslating = false,
                                        translationError = error.message
                                    )
                                } else {
                                    // 检查回复
                                    val updatedReplies = item.replies.map { reply ->
                                        if (reply.comment.id == commentId) {
                                            reply.copy(
                                                isTranslating = false,
                                                translationError = error.message
                                            )
                                        } else {
                                            reply
                                        }
                                    }
                                    if (updatedReplies != item.replies) {
                                        item.copy(replies = updatedReplies)
                                    } else {
                                        item
                                    }
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { state ->
                    state.copy(
                        comments = state.comments.map { item ->
                            if (item.comment.id == commentId) {
                                // 主评论
                                item.copy(
                                    isTranslating = false,
                                    translationError = e.message
                                )
                            } else {
                                // 检查回复
                                val updatedReplies = item.replies.map { reply ->
                                    if (reply.comment.id == commentId) {
                                        reply.copy(
                                            isTranslating = false,
                                            translationError = e.message
                                        )
                                    } else {
                                        reply
                                    }
                                }
                                if (updatedReplies != item.replies) {
                                    item.copy(replies = updatedReplies)
                                } else {
                                    item
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 清除评论翻译（包括主评论和回复）
     */
    private fun clearCommentTranslation(commentId: String) {
        _state.update { state ->
            state.copy(
                comments = state.comments.map { item ->
                    if (item.comment.id == commentId) {
                        // 主评论
                        item.copy(
                            translatedText = null,
                            translationError = null
                        )
                    } else {
                        // 检查回复
                        val updatedReplies = item.replies.map { reply ->
                            if (reply.comment.id == commentId) {
                                reply.copy(
                                    translatedText = null,
                                    translationError = null
                                )
                            } else {
                                reply
                            }
                        }
                        if (updatedReplies != item.replies) {
                            item.copy(replies = updatedReplies)
                        } else {
                            item
                        }
                    }
                }
            )
        }
    }
}
