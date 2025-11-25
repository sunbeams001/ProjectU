package com.projectu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectu.shared.domain.model.*

/**
 * BookmarkIndicator 组件预览
 * 用于测试三种收藏状态的显示效果
 */
@Composable
fun BookmarkIndicatorPreview() {
    // 创建测试用的 Artwork 对象
    val notBookmarkedArtwork = Artwork(
        id = "1",
        title = "测试作品",
        description = "",
        imageUrls = ArtworkImageUrls(pages = emptyList()),
        width = 100,
        height = 100,
        pageCount = 1,
        userId = "1",
        userName = "测试用户",
        userProfileImageUrl = "",
        tags = emptyList(),
        viewCount = 0,
        likeCount = 0,
        bookmarkCount = 0,
        commentCount = 0,
        createdTime = "",
        bookmarkStatus = BookmarkStatus.NOT_BOOKMARKED,
        bookmarkId = null,
        totalView = 0,
        totalBookmarks = 0
    )
    
    val publicBookmarkedArtwork = notBookmarkedArtwork.copy(
        id = "2",
        bookmarkStatus = BookmarkStatus.PUBLIC,
        bookmarkId = "bookmark_123"
    )
    
    val privateBookmarkedArtwork = notBookmarkedArtwork.copy(
        id = "3",
        bookmarkStatus = BookmarkStatus.PRIVATE,
        bookmarkId = "bookmark_456"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "收藏状态指示器测试",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // 未收藏状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookmarkIndicator(
                artwork = notBookmarkedArtwork,
                size = 32.dp
            )
            Column {
                Text(
                    text = "未收藏",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "空心爱心，灰色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 公开收藏状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookmarkIndicator(
                artwork = publicBookmarkedArtwork,
                size = 32.dp
            )
            Column {
                Text(
                    text = "公开收藏",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "实心爱心，粉色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 私人收藏状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookmarkIndicator(
                artwork = privateBookmarkedArtwork,
                size = 32.dp
            )
            Column {
                Text(
                    text = "私人收藏",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "实心爱心（粉色）+ 小锁图标",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 不同大小展示
        Text(
            text = "不同尺寸",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookmarkIndicator(artwork = privateBookmarkedArtwork, size = 16.dp)
            BookmarkIndicator(artwork = privateBookmarkedArtwork, size = 20.dp)
            BookmarkIndicator(artwork = privateBookmarkedArtwork, size = 24.dp)
            BookmarkIndicator(artwork = privateBookmarkedArtwork, size = 32.dp)
            BookmarkIndicator(artwork = privateBookmarkedArtwork, size = 48.dp)
        }
        
        Text(
            text = "交互说明",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "• 短按：切换收藏状态（未收藏↔公开收藏）\n• 长按未收藏：添加为私人收藏\n• 长按已收藏：切换公开/私人状态",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
