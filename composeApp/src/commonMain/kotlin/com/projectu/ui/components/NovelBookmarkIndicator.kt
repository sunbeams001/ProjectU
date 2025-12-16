package com.projectu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.BookmarkAction
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.shared.domain.usecase.BookmarkNovelUseCase
import com.projectu.shared.domain.usecase.UnbookmarkNovelUseCase
import com.projectu.ui.components.icons.PixivBookmarkIcons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 小说收藏状态指示器组件（带内置收藏逻辑）
 * 
 * 视觉效果：
 * - 未收藏：空心爱心（灰色）
 * - 公开收藏：实心爱心（粉色）
 * - 私人收藏：实心爱心（粉色）+ 小锁图标叠加在右下角
 * 
 * 交互逻辑：
 * - 短按：根据设置中的 clickBookmarkAction 配置执行对应行为
 * - 长按：根据设置中的 longPressBookmarkAction 配置执行对应行为
 * - 如果配置为 WITH_TAGS，则弹出标签选择对话框（目前暂未实现，会提示用户）
 * 
 * @param novel 小说对象（需要包含完整的收藏信息）
 * @param size 组件大小
 * @param onStatusChanged 状态变化回调（可选，用于更新UI）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelBookmarkIndicator(
    novel: Novel,
    size: Dp = 24.dp,
    onStatusChanged: ((BookmarkStatus) -> Unit)? = null,
    modifier: Modifier = Modifier,
    stateCacheManager: StateCacheManager = koinInject(),
    bookmarkUseCase: BookmarkNovelUseCase = koinInject(),
    unbookmarkUseCase: UnbookmarkNovelUseCase = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
    var currentStatus by remember { mutableStateOf(novel.bookmarkStatus) }
    var isProcessing by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagDialogTrigger by remember { mutableStateOf<BookmarkAction?>(null) }
    val scope = rememberCoroutineScope()
    
    // 从设置中读取收藏行为配置
    val settings by settingsRepository.getSettings().collectAsState(
        initial = com.projectu.shared.data.local.AppSettings.DEFAULT
    )
    val clickAction = settings.clickBookmarkAction
    val longPressAction = settings.longPressBookmarkAction
    
    // 监听全局状态变化
    LaunchedEffect(novel.id) {
        stateCacheManager.getNovelState(novel.id).collectLatest { state ->
            if (state != null) {
                currentStatus = state.bookmarkStatus
            }
        }
    }
    
    // 监听外部状态变化，同步内部状态
    LaunchedEffect(novel.bookmarkStatus) {
        currentStatus = novel.bookmarkStatus
    }
    
    // 根据状态选择图标
    val bookmarkIcon = when (currentStatus) {
        BookmarkStatus.NOT_BOOKMARKED -> PixivBookmarkIcons.notBookmarked()
        BookmarkStatus.PUBLIC -> PixivBookmarkIcons.PublicBookmarked
        BookmarkStatus.PRIVATE -> PixivBookmarkIcons.PrivateBookmarked
    }
    
    Icon(
        imageVector = bookmarkIcon,
        contentDescription = when (currentStatus) {
            BookmarkStatus.NOT_BOOKMARKED -> stringResource(Res.string.bookmark_not_bookmarked)
            BookmarkStatus.PUBLIC -> stringResource(Res.string.bookmark_public)
            BookmarkStatus.PRIVATE -> stringResource(Res.string.bookmark_private)
        },
        tint = androidx.compose.ui.graphics.Color.Unspecified, // 使用图标自带颜色
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickable(
                enabled = !isProcessing,
                onClick = {
                    if (isProcessing) return@combinedClickable
                    
                    scope.launch {
                        isProcessing = true
                        try {
                            when (currentStatus) {
                                BookmarkStatus.NOT_BOOKMARKED -> {
                                    // 根据配置执行收藏行为
                                    when (clickAction) {
                                        BookmarkAction.PUBLIC -> {
                                            println("📌 NovelBookmarkIndicator: 添加公开收藏（配置） - 小说ID: ${novel.id}")
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = false
                                            ).onSuccess {
                                                onStatusChanged?.invoke(BookmarkStatus.PUBLIC)
                                                println("✅ 收藏成功：公开收藏")
                                            }.onFailure { e ->
                                                println("❌ 收藏失败: ${e.message}")
                                            }
                                        }
                                        BookmarkAction.PRIVATE -> {
                                            println("📌 NovelBookmarkIndicator: 添加私人收藏（配置） - 小说ID: ${novel.id}")
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = true
                                            ).onSuccess {
                                                onStatusChanged?.invoke(BookmarkStatus.PRIVATE)
                                                println("✅ 收藏成功：私人收藏")
                                            }.onFailure { e ->
                                                println("❌ 收藏失败: ${e.message}")
                                            }
                                        }
                                        BookmarkAction.WITH_TAGS -> {
                                            // 显示标签选择对话框
                                            println("📌 NovelBookmarkIndicator: 打开标签收藏对话框 - 小说ID: ${novel.id}")
                                            tagDialogTrigger = clickAction
                                            showTagDialog = true
                                        }
                                    }
                                }
                                BookmarkStatus.PUBLIC, BookmarkStatus.PRIVATE -> {
                                    // 取消收藏
                                    println("📌 NovelBookmarkIndicator: 取消收藏 - 小说ID: ${novel.id}")
                                    unbookmarkUseCase(novel.id.toLong())
                                        .onSuccess {
                                            onStatusChanged?.invoke(BookmarkStatus.NOT_BOOKMARKED)
                                            println("✅ 已取消收藏")
                                        }.onFailure { e ->
                                            println("❌ 取消收藏失败: ${e.message}")
                                        }
                                }
                            }
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                onLongClick = {
                    if (isProcessing) return@combinedClickable
                    
                    scope.launch {
                        isProcessing = true
                        try {
                            when (currentStatus) {
                                BookmarkStatus.NOT_BOOKMARKED -> {
                                    // 根据配置执行收藏行为
                                    when (longPressAction) {
                                        BookmarkAction.PUBLIC -> {
                                            println("📌 NovelBookmarkIndicator: 添加公开收藏（长按配置） - 小说ID: ${novel.id}")
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = false
                                            ).onSuccess {
                                                onStatusChanged?.invoke(BookmarkStatus.PUBLIC)
                                                println("✅ 收藏成功：公开收藏")
                                            }.onFailure { e ->
                                                println("❌ 收藏失败: ${e.message}")
                                            }
                                        }
                                        BookmarkAction.PRIVATE -> {
                                            println("📌 NovelBookmarkIndicator: 添加私人收藏（长按配置） - 小说ID: ${novel.id}")
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = true
                                            ).onSuccess {
                                                onStatusChanged?.invoke(BookmarkStatus.PRIVATE)
                                                println("✅ 收藏成功：私人收藏")
                                            }.onFailure { e ->
                                                println("❌ 私人收藏失败: ${e.message}")
                                            }
                                        }
                                        BookmarkAction.WITH_TAGS -> {
                                            // 显示标签选择对话框
                                            println("📌 NovelBookmarkIndicator: 长按打开标签收藏对话框 - 小说ID: ${novel.id}")
                                            tagDialogTrigger = longPressAction
                                            showTagDialog = true
                                        }
                                    }
                                }
                                BookmarkStatus.PUBLIC -> {
                                    // 先取消，再添加为私人收藏
                                    println("📌 NovelBookmarkIndicator: 公开→私人 - 小说ID: ${novel.id}")
                                    unbookmarkUseCase(novel.id.toLong())
                                        .onSuccess {
                                            println("🔸 步骤1完成：已取消公开收藏")
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = true
                                            ).onSuccess {
                                                println("🔸 步骤2完成：已添加私人收藏")
                                                onStatusChanged?.invoke(BookmarkStatus.PRIVATE)
                                                println("✅ 已切换为私人收藏")
                                            }.onFailure { e ->
                                                println("❌ 切换为私人收藏失败: ${e.message}")
                                            }
                                        }.onFailure { e ->
                                            println("❌ 取消收藏失败: ${e.message}")
                                        }
                                }
                                BookmarkStatus.PRIVATE -> {
                                    // 先取消，再添加为公开收藏
                                    println("📌 NovelBookmarkIndicator: 私人→公开 - 小说ID: ${novel.id}")
                                    unbookmarkUseCase(novel.id.toLong())
                                        .onSuccess {
                                            bookmarkUseCase(
                                                novelId = novel.id.toLong(),
                                                isPrivate = false
                                            ).onSuccess {
                                                onStatusChanged?.invoke(BookmarkStatus.PUBLIC)
                                                println("✅ 已切换为公开收藏")
                                            }.onFailure { e ->
                                                println("❌ 切换为公开收藏失败: ${e.message}")
                                            }
                                        }.onFailure { e ->
                                            println("❌ 取消收藏失败: ${e.message}")
                                        }
                                }
                            }
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            )
    )
    
    // 标签收藏对话框
    if (showTagDialog) {
        BookmarkWithTagsDialog(
            onDismiss = {
                showTagDialog = false
                tagDialogTrigger = null
            },
            onConfirm = { tags, isPrivate ->
                scope.launch {
                    isProcessing = true
                    try {
                        println("📌 NovelBookmarkIndicator: 按标签收藏 - 小说ID: ${novel.id}, 标签: $tags, 私人: $isPrivate")
                        bookmarkUseCase(
                            novelId = novel.id.toLong(),
                            isPrivate = isPrivate,
                            tags = tags  // ✅ 传递标签参数
                        ).onSuccess {
                            onStatusChanged?.invoke(if (isPrivate) BookmarkStatus.PRIVATE else BookmarkStatus.PUBLIC)
                            println("✅ 收藏成功：${if (isPrivate) "私人" else "公开"}收藏，标签: $tags")
                            // ✅ 只在成功时关闭对话框
                            showTagDialog = false
                            tagDialogTrigger = null
                        }.onFailure { e ->
                            println("❌ 收藏失败: ${e.message}")
                            // ❌ 失败时不关闭对话框，让用户可以重试
                        }
                    } finally {
                        isProcessing = false
                    }
                }
            },
            suggestedTags = novel.tags.map { it.name }, // 使用小说标签作为建议
            initialPrivate = false
        )
    }
}


