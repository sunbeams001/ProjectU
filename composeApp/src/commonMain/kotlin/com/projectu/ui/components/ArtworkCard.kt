package com.projectu.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.getUrlByQuality
import com.projectu.ui.util.isPixivImageSquareCropped
import org.koin.compose.koinInject

/**
 * 作品卡片组件 - Material Design 3 风格
 * 用于瀑布流展示
 * 
 * @param artwork 作品对象
 * @param onClick 点击作品回调
 * @param onUserClick 点击用户区域回调（头像或用户名），为null时不响应用户点击
 * @param showUserInfo 是否显示用户信息（头像和用户名），默认true。用户主页场景可设为false
 * @param modifier 修饰符
 * @param settingsCache 设置缓存
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtworkCard(
    artwork: Artwork,
    onClick: () -> Unit,
    onUserClick: ((userId: String) -> Unit)? = null,
    showUserInfo: Boolean = true,
    modifier: Modifier = Modifier,
    settingsCache: SettingsCache = koinInject()
) {
    // 从缓存中读取首选图片质量（内存访问，零延迟）
    val preferredQuality = settingsCache.getPreferredImageQuality()
    val imageUrl = artwork.imageUrls.pages.firstOrNull()?.urls?.getUrlByQuality(
        preferredQuality
    ) ?: ""
    
    // 智能判断：根据URL特征判断是否为方形裁剪
    // - 关注接口（FollowApi）：URL包含 /c/250x250_80_a2/ 等标识 → 方形裁剪
    // - 排行榜接口（RankingApi）：普通URL，没有特殊裁剪标识 → 保持原始比例
    val isSquareCrop = isPixivImageSquareCropped(imageUrl)
    
    val displayAspectRatio = when {
        // 如果URL标识为方形裁剪，使用1:1
        isSquareCrop && preferredQuality != ImageQuality.MASTER_1200 -> 1f
        // 否则保持原始宽高比
        else -> artwork.width.toFloat() / artwork.height.toFloat()
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column {
            // 作品缩略图（只有上方圆角）
            Box {
                RetryableAsyncImage(
                    model = imageUrl,
                    contentDescription = artwork.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(displayAspectRatio)
                        .clip(
                            RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .combinedClickable(
                            onClick = {
                                println("ArtworkCard: 点击图片 - ID: ${artwork.id}, 标题: ${artwork.title}")
                                onClick()
                            },
                            onLongClick = {
                                println("ArtworkCard: 长按图片 - ID: ${artwork.id}, 标题: ${artwork.title}")
                            }
                        ),
                    contentScale = ContentScale.Crop,
                    showErrorDetails = false
                )
                
                // 使用统一的标记组件
                ArtworkBadges(
                    artwork = artwork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(displayAspectRatio)
                )
            }
            
            // 作品信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            println("ArtworkCard: 点击信息区域 - ID: ${artwork.id}, 作者: ${artwork.userName}")
                            onClick()
                        },
                        onLongClick = {
                            println("ArtworkCard: 长按信息区域 - ID: ${artwork.id}, 作者: ${artwork.userName}")
                        }
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 标题 + 收藏标记
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 标题（最多2行，占据剩余空间）
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 收藏状态指示器（内置收藏逻辑）
                    BookmarkIndicator(
                        artwork = artwork,
                        size = 20.dp
                    )
                }
                
                // 作者信息（头像 + 名称）- 可选显示
                if (showUserInfo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onUserClick != null) {
                                    Modifier.combinedClickable(
                                        onClick = {
                                            onUserClick(artwork.userId)
                                        }
                                    )
                                } else Modifier
                            ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 作者头像
                        AsyncImage(
                            model = artwork.userProfileImageUrl,
                            contentDescription = artwork.userName,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        // 作者名
                        Text(
                            text = artwork.userName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
