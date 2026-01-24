package com.projectu.ui.components.share

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkShareType
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 作品分享选项底部表单
 * 
 * @param artwork 作品信息
 * @param onDismiss 关闭回调
 * @param onShareSelected 分享选项选择回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkShareBottomSheet(
    artwork: Artwork,
    onDismiss: () -> Unit,
    onShareSelected: (ArtworkShareType, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(Res.string.share_options),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 分享链接
            ShareOptionItem(
                icon = Icons.Default.Link,
                title = stringResource(Res.string.share_link),
                description = stringResource(Res.string.share_link_description),
                onClick = {
                    onShareSelected(ArtworkShareType.LINK_ONLY, 0)
                    onDismiss()
                }
            )
            
            // 分享文字+图片
            ShareOptionItem(
                icon = Icons.AutoMirrored.Filled.Article,
                title = stringResource(Res.string.share_text_with_image),
                description = stringResource(Res.string.share_text_with_image_description),
                onClick = {
                    onShareSelected(ArtworkShareType.TEXT_WITH_IMAGE, 0)
                    onDismiss()
                }
            )
            
            // 分享第一张图片
            ShareOptionItem(
                icon = Icons.Default.Image,
                title = stringResource(Res.string.share_first_image),
                description = stringResource(Res.string.share_first_image_description),
                onClick = {
                    onShareSelected(ArtworkShareType.IMAGE_ONLY, 0)
                    onDismiss()
                }
            )
            
            // 如果是多页作品，显示选择特定页的选项
            if (artwork.pageCount > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = stringResource(Res.string.share_specific_page),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(artwork.pageCount) { index ->
                        PageThumbnailCard(
                            pageIndex = index,
                            imageUrl = artwork.imageUrls.pages.getOrNull(index)?.urls?.squareMedium ?: "",
                            onClick = {
                                onShareSelected(ArtworkShareType.IMAGE_SPECIFIC_PAGE, index)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 分享选项项
 */
@Composable
private fun ShareOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 页面缩略图卡片
 */
@Composable
private fun PageThumbnailCard(
    pageIndex: Int,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(80.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 显示缩略图
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "P${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 页码标签（覆盖在图片上方）
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = "P${pageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
