package com.projectu.ui.screens.artwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.User
import com.projectu.shared.util.DateTimeFormatter
import com.projectu.ui.components.BookmarkIndicator
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import com.projectu.ui.components.FollowIndicator

/**
 * 作品信息展示区域
 * 
 * 包含：
 * - 作品标题
 * - 系列信息（如果有）
 * - 收藏按钮
 * - 作者头像
 * - 作者名 + 投稿时间
 * - 关注按钮
 * 
 * @param artwork 作品对象
 * @param authorFollowStatus 作者关注状态
 * @param onUserClick 点击用户区域回调（头像或用户名）
 * @param onSeriesClick 点击系列回调
 * @param modifier 修饰符
 */
@Composable
fun ArtworkInfoSection(
    artwork: Artwork,
    authorFollowStatus: FollowStatus,
    onUserClick: ((userId: Long) -> Unit)? = null,
    onSeriesClick: ((seriesId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()  // 为底部导航栏添加 padding（Android）
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 第一行：标题 + 收藏按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 标题区域（占据剩余空间）
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 标题
                    Text(
                        text = artwork.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 系列信息（如果有）
                    val seriesId = artwork.seriesId
                    val seriesTitle = artwork.seriesTitle
                    if (seriesId != null && seriesTitle != null) {
                        Text(
                            text = seriesTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.then(
                                if (onSeriesClick != null) {
                                    Modifier.clickable {
                                        seriesId.toLongOrNull()?.let { onSeriesClick(it) }
                                    }
                                } else Modifier
                            )
                        )
                    }
                }

                // 收藏状态指示器（内置收藏逻辑）
                BookmarkIndicator(
                    artwork = artwork,
                    size = 28.dp
                )
            }

            // 第二行：作者信息 + 关注按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 作者头像 - 如果URL为空，显示默认图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (onUserClick != null) {
                                Modifier.clickable {
                                    artwork.userId.toLongOrNull()?.let { onUserClick(it) }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork.userProfileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = artwork.userProfileImageUrl,
                            contentDescription = artwork.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 显示默认头像图标
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = artwork.userName,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 作者名和投稿时间（占据剩余空间）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onUserClick != null) {
                                Modifier.clickable {
                                    artwork.userId.toLongOrNull()?.let { onUserClick(it) }
                                }
                            } else Modifier
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 作者名
                    Text(
                        text = artwork.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 投稿时间
                    Text(
                        text = DateTimeFormatter.formatToLocalDateTime(artwork.createdTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // 关注状态指示器（内置关注逻辑）
                FollowIndicator(
                    user = User(
                        id = artwork.userId,
                        name = artwork.userName,
                        profileImageUrl = artwork.userProfileImageUrl,
                        followStatus = authorFollowStatus
                    ),
                    size = 28.dp
                )
            }
        }
    }
}
