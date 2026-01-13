package com.projectu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.projectu.shared.domain.model.Emoji
import com.projectu.shared.domain.model.EmojiConfig
import com.projectu.shared.domain.model.Stamp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 表情选择器弹窗
 * 
 * @param visible 是否显示
 * @param onDismiss 关闭弹窗
 * @param onEmojiSelected 选中 emoji 时的回调，返回 emoji label 以插入文本
 * @param onStampSelected 选中 stamp 时的回调，直接发送
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEmojiSelected: (Emoji) -> Unit,
    onStampSelected: (Stamp) -> Unit
) {
    if (!visible) return
    
    Popup(
        alignment = Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        EmojiPickerContent(
            onEmojiSelected = {
                onEmojiSelected(it)
                // emoji 选中后不关闭弹窗，允许继续选择
            },
            onStampSelected = {
                onStampSelected(it)
                onDismiss() // stamp 选中后关闭弹窗并直接发送
            }
        )
    }
}

/**
 * 表情选择器内容
 */
@Composable
fun EmojiPickerContent(
    onEmojiSelected: (Emoji) -> Unit,
    onStampSelected: (Stamp) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tab 栏
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(Res.string.emoji_tab_emoji)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(Res.string.emoji_tab_stamp)) }
                )
            }
            
            // 内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> EmojiGrid(
                        emojis = EmojiConfig.emojis,
                        onEmojiClick = onEmojiSelected
                    )
                    1 -> StampGrid(
                        stamps = EmojiConfig.stamps,
                        onStampClick = onStampSelected
                    )
                }
            }
        }
    }
}

/**
 * Emoji 表情网格
 */
@Composable
private fun EmojiGrid(
    emojis: List<Emoji>,
    onEmojiClick: (Emoji) -> Unit
) {
    val context = LocalPlatformContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 40.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(emojis, key = { it.label }) { emoji ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEmojiClick(emoji) }
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(emoji.url)
                        .crossfade(true)
                        .memoryCacheKey("emoji_${emoji.label}")
                        .diskCacheKey("emoji_${emoji.label}")
                        .build(),
                    contentDescription = emoji.label,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Stamp 贴图网格
 */
@Composable
private fun StampGrid(
    stamps: List<Stamp>,
    onStampClick: (Stamp) -> Unit
) {
    val context = LocalPlatformContext.current
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 72.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(stamps, key = { it.id }) { stamp ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onStampClick(stamp) }
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(stamp.url)
                        .crossfade(true)
                        .memoryCacheKey("stamp_${stamp.id}")
                        .diskCacheKey("stamp_${stamp.id}")
                        .build(),
                    contentDescription = "Stamp ${stamp.id}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
