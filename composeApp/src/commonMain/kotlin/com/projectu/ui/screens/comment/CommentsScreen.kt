package com.projectu.ui.screens.comment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.projectu.shared.domain.model.Comment
import com.projectu.shared.domain.model.CommentContentType
import com.projectu.shared.util.DateTimeFormatter
import com.projectu.ui.components.EmojiPickerPopup
import com.projectu.ui.components.EmojiText
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.screens.user.UserScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 评论页面
 * 
 * @param contentId 内容ID（插画ID或小说ID）
 * @param contentType 内容类型
 * @param contentTitle 作品标题
 */
@Serializable
data class CommentsScreen(
    private val contentId: String,
    private val contentType: CommentContentType,
    private val contentTitle: String
) : Screen {
    
    override val key: ScreenKey = "CommentsScreen_${contentType.name}_$contentId"
    
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CommentsViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        LaunchedEffect(contentId) {
            viewModel.handleIntent(
                CommentsIntent.Initialize(
                    contentId = contentId,
                    contentType = contentType,
                    contentTitle = contentTitle
                )
            )
        }
        
        CommentsContent(
            state = state,
            onIntent = viewModel::handleIntent,
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsContent(
    state: CommentsScreenState,
    onIntent: (CommentsIntent) -> Unit,
    onUserClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // 监听滚动加载更多
    LaunchedEffect(listState, state.comments.size, state.isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !state.isLoadingMore && state.hasMore) {
                onIntent(CommentsIntent.LoadMore)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.comments_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.contentTitle.isNotEmpty()) {
                            Text(
                                text = state.contentTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            CommentInputBar(
                state = state,
                onIntent = onIntent
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.comments.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.comments.isEmpty() -> {
                    ErrorDisplay(
                        message = state.error,
                        onRetry = { onIntent(CommentsIntent.Refresh) },
                        modifier = Modifier.align(Alignment.Center),
                        isFullScreen = true
                    )
                }
                state.comments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.comments_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onIntent(CommentsIntent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.comments,
                                key = { it.comment.id }
                            ) { commentItem ->
                                CommentItemView(
                                    commentItem = commentItem,
                                    isDeletingId = state.isDeletingCommentId,
                                    onToggleReplies = { onIntent(CommentsIntent.ToggleReplies(commentItem.comment.id)) },
                                    onLoadMoreReplies = { onIntent(CommentsIntent.LoadMoreReplies(commentItem.comment.id)) },
                                    onReply = { comment ->
                                        onIntent(CommentsIntent.SetReplyTarget(
                                            ReplyTarget(
                                                commentId = comment.id.toLongOrNull() ?: 0L,
                                                userName = comment.userName
                                            )
                                        ))
                                    },
                                    onDelete = { comment ->
                                        onIntent(CommentsIntent.ShowDeleteConfirmDialog(comment))
                                    },
                                    onUserClick = onUserClick
                                )
                            }
                            
                            // 加载更多指示器
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 显示错误 Snackbar
    state.deleteError?.let { error ->
        LaunchedEffect(error) {
            // 显示错误后清除
            onIntent(CommentsIntent.ClearError)
        }
    }
}

/**
 * 评论输入栏
 */
@Composable
fun CommentInputBar(
    state: CommentsScreenState,
    onIntent: (CommentsIntent) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // 回复提示
                AnimatedVisibility(
                    visible = state.replyTarget != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    state.replyTarget?.let { target ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.comments_replying_to, target.userName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onIntent(CommentsIntent.SetReplyTarget(null)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.comments_cancel_reply),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 输入框
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 表情按钮
                    IconButton(
                        onClick = { onIntent(CommentsIntent.SetEmojiPickerVisible(!state.isEmojiPickerVisible)) }
                    ) {
                        Icon(
                            imageVector = if (state.isEmojiPickerVisible) Icons.Default.KeyboardArrowDown else Icons.Default.EmojiEmotions,
                            contentDescription = stringResource(Res.string.emoji_button),
                            tint = if (state.isEmojiPickerVisible) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    OutlinedTextField(
                        value = state.commentInput,
                        onValueChange = { onIntent(CommentsIntent.UpdateCommentInput(it)) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = if (state.replyTarget != null) {
                                    stringResource(Res.string.comments_reply_hint)
                                } else {
                                    stringResource(Res.string.comments_input_hint)
                                }
                            )
                        },
                        maxLines = 3,
                        singleLine = false,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    
                    // 发送按钮
                    IconButton(
                        onClick = { onIntent(CommentsIntent.PostComment) },
                        enabled = state.commentInput.isNotBlank() && !state.isPosting
                    ) {
                        if (state.isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(Res.string.comments_send),
                                tint = if (state.commentInput.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
                
                // 发表错误提示
                state.postError?.let { error ->
                    Text(
                        text = if (error == "comment_empty_error") {
                            stringResource(Res.string.comments_empty_error)
                        } else {
                            error
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // 表情选择器弹窗
        EmojiPickerPopup(
            visible = state.isEmojiPickerVisible,
            onDismiss = { onIntent(CommentsIntent.SetEmojiPickerVisible(false)) },
            onEmojiSelected = { emoji ->
                onIntent(CommentsIntent.InsertEmoji(emoji))
            },
            onStampSelected = { stamp ->
                onIntent(CommentsIntent.PostStampComment(stamp))
            }
        )
        
        // 删除确认对话框
        if (state.commentToDelete != null) {
            DeleteConfirmDialog(
                comment = state.commentToDelete,
                onConfirm = {
                    onIntent(CommentsIntent.DeleteComment(state.commentToDelete.id))
                    onIntent(CommentsIntent.CancelDelete)
                },
                onDismiss = { onIntent(CommentsIntent.CancelDelete) }
            )
        }
        
        // 删除确认对话框
        if (state.commentToDelete != null) {
            DeleteConfirmDialog(
                comment = state.commentToDelete,
                onConfirm = {
                    onIntent(CommentsIntent.DeleteComment(state.commentToDelete.id))
                    onIntent(CommentsIntent.CancelDelete)
                },
                onDismiss = { onIntent(CommentsIntent.CancelDelete) }
            )
        }
    }
}

/**
 * 删除评论确认对话框
 */
@Composable
fun DeleteConfirmDialog(
    comment: Comment,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.comments_delete_confirm_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(Res.string.comments_delete_confirm_message))
                
                // 显示评论预览
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = comment.userName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        comment.comment?.let { commentText ->
                            Text(
                                text = commentText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (comment.comment == null && comment.stampLink != null) {
                            Text(
                                text = "[${stringResource(Res.string.comments_stamp)}]",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(Res.string.comments_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.comments_delete_cancel))
            }
        }
    )
}

/**
 * 单条评论视图
 */
@Composable
fun CommentItemView(
    commentItem: CommentItem,
    isDeletingId: String?,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onReply: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    onUserClick: (String) -> Unit
) {
    val comment = commentItem.comment
    val isDeleting = isDeletingId == comment.id
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 主评论
        CommentContent(
            comment = comment,
            isDeleting = isDeleting,
            hasReplies = comment.hasReplies,
            isExpanded = commentItem.isExpanded,
            isLoadingReplies = commentItem.isLoadingReplies && commentItem.replies.isEmpty(),
            onToggleReplies = onToggleReplies,
            onReply = { onReply(comment) },
            onDelete = { onDelete(comment) },
            onUserClick = onUserClick
        )
        
        // 回复列表
        AnimatedVisibility(
            visible = commentItem.isExpanded && commentItem.replies.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                commentItem.replies.asReversed().forEach { reply ->
                    CommentContent(
                        comment = reply,
                        isDeleting = isDeletingId == reply.id,
                        isReply = true,
                        onReply = { onReply(reply) },
                        onDelete = { onDelete(reply) },
                        onUserClick = onUserClick
                    )
                }
                
                // 加载更多回复按钮
                if (commentItem.hasMoreReplies) {
                    TextButton(
                        onClick = onLoadMoreReplies,
                        enabled = !commentItem.isLoadingReplies
                    ) {
                        if (commentItem.isLoadingReplies) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(Res.string.comments_load_more_replies),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * 评论内容
 */
@Composable
fun CommentContent(
    comment: Comment,
    isDeleting: Boolean,
    isReply: Boolean = false,
    hasReplies: Boolean = false,
    isExpanded: Boolean = false,
    isLoadingReplies: Boolean = false,
    onToggleReplies: (() -> Unit)? = null,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onUserClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(if (isReply) 32.dp else 40.dp)
                .clip(CircleShape)
                .clickable { onUserClick(comment.userId) }
        ) {
            if (comment.userProfileImageUrl != null) {
                AsyncImage(
                    model = comment.userProfileImageUrl,
                    contentDescription = comment.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = comment.userName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isReply) 16.dp else 20.dp)
                    )
                }
            }
        }
        
        // 评论内容
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 用户名和时间
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (comment.isDeletedUser) {
                        stringResource(Res.string.comments_deleted_user)
                    } else {
                        comment.userName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (comment.isDeletedUser) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.clickable(enabled = !comment.isDeletedUser) {
                        onUserClick(comment.userId)
                    }
                )
                
                Text(
                    text = DateTimeFormatter.formatJapanTimeToLocal(comment.commentDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 回复对象提示
            if (isReply && comment.replyToUserName != null) {
                Text(
                    text = stringResource(Res.string.comments_reply_to, comment.replyToUserName!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 评论文本或表情
            if (comment.stampLink != null) {
                // Stamp 贴图（优先显示，因为 stamp 评论的 comment 可能是空字符串）
                val context = LocalPlatformContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comment.stampLink)
                        .crossfade(true)
                        .memoryCacheKey("stamp_comment_${comment.id}")
                        .diskCacheKey("stamp_comment_${comment.id}")
                        .build(),
                    contentDescription = stringResource(Res.string.comments_stamp),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (!comment.comment.isNullOrEmpty()) {
                // 使用 EmojiText 渲染包含表情的文本，支持长按选中和复制
                androidx.compose.foundation.text.selection.SelectionContainer {
                    EmojiText(
                        text = comment.comment!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 操作按钮（右对齐）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 回复按钮
                TextButton(
                    onClick = onReply,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = stringResource(Res.string.comments_reply),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.comments_reply),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 展开/收起回复按钮（仅当有回复且非回复本身时显示）
                if (!isReply && hasReplies && onToggleReplies != null) {
                    TextButton(
                        onClick = onToggleReplies,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isLoadingReplies) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) {
                                stringResource(Res.string.comments_hide_replies)
                            } else {
                                stringResource(Res.string.comments_show_replies)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // 删除按钮（仅对自己的评论显示）
                if (comment.isEditable) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isDeleting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.comments_delete),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.comments_delete),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
