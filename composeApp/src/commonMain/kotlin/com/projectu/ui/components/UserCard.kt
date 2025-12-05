package com.projectu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.model.getUrlByQuality
import org.koin.compose.koinInject

/**
 * 用户卡片组件
 * 展示用户头像、用户名、前3个作品缩略图、关注状态
 * 
 * @param user 用户对象（包含作品列表）
 * @param onUserClick 用户点击回调
 * @param onArtworkClick 作品点击回调，参数为 (Artwork, 全局索引)
 * @param artworkStartIndex 该用户作品在全局作品列表中的起始索引（用于列表导航）
 * @param modifier 修饰符
 */
@Composable
fun UserCard(
    user: User,
    onUserClick: (User) -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    artworkStartIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUserClick(user) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部：用户信息和关注按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户头像和名称
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 头像
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 用户信息
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 简介（如果有）
                        user.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // 关注按钮
                FollowIndicator(
                    user = user,
                    size = 32.dp
                )
            }
            
            // 作品缩略图（最多显示3个）
            if (user.illusts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp) // 移除分隔线
                ) {
                    user.illusts.take(3).forEachIndexed { index, artwork ->
                        ArtworkThumbnail(
                            artwork = artwork,
                            onClick = { onArtworkClick(artwork, artworkStartIndex + index) },
                            modifier = Modifier.weight(1f),
                            position = when (index) {
                                0 -> ThumbnailPosition.START // 最左侧
                                user.illusts.take(3).size - 1 -> ThumbnailPosition.END // 最右侧
                                else -> ThumbnailPosition.MIDDLE // 中间
                            }
                        )
                    }
                    
                    // 如果不足3个，填充空白
                    repeat(3 - user.illusts.take(3).size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 缩略图位置枚举（用于控制圆角）
 */
private enum class ThumbnailPosition {
    START,   // 最左侧：左侧圆角
    MIDDLE,  // 中间：无圆角
    END      // 最右侧：右侧圆角
}

/**
 * 作品缩略图组件（用于用户卡片）
 */
@Composable
private fun ArtworkThumbnail(
    artwork: Artwork,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: ThumbnailPosition = ThumbnailPosition.START,
    settingsCache: SettingsCache = koinInject()
) {
    // 使用和 ArtworkCard 相同的图片质量选择方式
    val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.getUrlByQuality(
        settingsCache.getPreferredImageQuality()
    ) ?: ""
    
    // 根据位置确定圆角
    val shape = when (position) {
        ThumbnailPosition.START -> RoundedCornerShape(
            topStart = 8.dp,
            bottomStart = 8.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        )
        ThumbnailPosition.END -> RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = 8.dp,
            bottomEnd = 8.dp
        )
        ThumbnailPosition.MIDDLE -> RoundedCornerShape(0.dp) // 无圆角
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = artwork.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 使用统一的标记组件
        ArtworkBadges(
            artwork = artwork,
            modifier = Modifier.fillMaxSize()
        )
    }
}
