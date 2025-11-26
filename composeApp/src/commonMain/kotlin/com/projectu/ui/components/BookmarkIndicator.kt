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
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.usecase.BookmarkArtworkUseCase
import com.projectu.shared.domain.usecase.UnbookmarkArtworkUseCase
import com.projectu.ui.components.icons.PixivBookmarkIcons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 收藏状态指示器组件（带内置收藏逻辑）
 * 
 * 视觉效果：
 * - 未收藏：空心爱心（灰色）
 * - 公开收藏：实心爱心（粉色）
 * - 私人收藏：实心爱心（粉色）+ 小锁图标叠加在右下角
 * 
 * 交互逻辑：
 * - 短按：切换收藏状态（未收藏→公开收藏→未收藏）
 * - 长按：如果已收藏，切换公开/私人状态；如果未收藏，添加为私人收藏
 * 
 * @param artwork 作品对象（需要包含完整的收藏信息）
 * @param size 组件大小
 * @param onStatusChanged 状态变化回调（可选，用于更新UI）
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkIndicator(
    artwork: Artwork,
    size: Dp = 24.dp,
    onStatusChanged: ((BookmarkStatus) -> Unit)? = null,
    modifier: Modifier = Modifier,
    stateCacheManager: StateCacheManager = koinInject(),
    bookmarkUseCase: BookmarkArtworkUseCase = koinInject(),
    unbookmarkUseCase: UnbookmarkArtworkUseCase = koinInject()
) {
    var currentStatus by remember { mutableStateOf(artwork.bookmarkStatus) }
    var isProcessing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 监听全局状态变化
    LaunchedEffect(artwork.id) {
        stateCacheManager.getArtworkState(artwork.id).collectLatest { state ->
            if (state != null) {
                currentStatus = state.bookmarkStatus
            }
        }
    }
    
    // 监听外部状态变化，同步内部状态
    LaunchedEffect(artwork.bookmarkStatus) {
        currentStatus = artwork.bookmarkStatus
    }
    
    // 根据状态选择图标
    val bookmarkIcon = when (currentStatus) {
        BookmarkStatus.NOT_BOOKMARKED -> PixivBookmarkIcons.NotBookmarked
        BookmarkStatus.PUBLIC -> PixivBookmarkIcons.PublicBookmarked
        BookmarkStatus.PRIVATE -> PixivBookmarkIcons.PrivateBookmarked
    }
    
    Icon(
        imageVector = bookmarkIcon,
        contentDescription = when (currentStatus) {
            BookmarkStatus.NOT_BOOKMARKED -> "未收藏"
            BookmarkStatus.PUBLIC -> "公开收藏"
            BookmarkStatus.PRIVATE -> "私人收藏"
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
                                    // 添加公开收藏
                                    println("📌 BookmarkIndicator: 添加公开收藏 - 作品ID: ${artwork.id}")
                                    bookmarkUseCase(
                                        artworkId = artwork.id.toLong(),
                                        isPrivate = false
                                    ).onSuccess {
                                        onStatusChanged?.invoke(BookmarkStatus.PUBLIC)
                                        println("✅ 收藏成功：公开收藏")
                                    }.onFailure { e ->
                                        println("❌ 收藏失败: ${e.message}")
                                    }
                                }
                                BookmarkStatus.PUBLIC, BookmarkStatus.PRIVATE -> {
                                    // 取消收藏
                                    println("📌 BookmarkIndicator: 取消收藏 - 作品ID: ${artwork.id}")
                                    unbookmarkUseCase(artwork.id.toLong())
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
                                    // 添加私人收藏
                                    println("📌 BookmarkIndicator: 添加私人收藏 - 作品ID: ${artwork.id}")
                                    bookmarkUseCase(
                                        artworkId = artwork.id.toLong(),
                                        isPrivate = true
                                    ).onSuccess {
                                        onStatusChanged?.invoke(BookmarkStatus.PRIVATE)
                                        println("✅ 收藏成功：私人收藏")
                                    }.onFailure { e ->
                                        println("❌ 私人收藏失败: ${e.message}")
                                    }
                                }
                                BookmarkStatus.PUBLIC -> {
                                    // 先取消，再添加为私人收藏
                                    println("📌 BookmarkIndicator: 公开→私人 - 作品ID: ${artwork.id}")
                                    unbookmarkUseCase(artwork.id.toLong())
                                        .onSuccess {
                                            println("🔸 步骤1完成：已取消公开收藏")
                                            bookmarkUseCase(
                                                artworkId = artwork.id.toLong(),
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
                                    println("📌 BookmarkIndicator: 私人→公开 - 作品ID: ${artwork.id}")
                                    unbookmarkUseCase(artwork.id.toLong())
                                        .onSuccess {
                                            bookmarkUseCase(
                                                artworkId = artwork.id.toLong(),
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
}
