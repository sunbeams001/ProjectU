package com.projectu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.projectu.ui.screens.user.BookmarkTagData
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 标签筛选弹窗
 * 
 * 可复用的标签筛选组件，支持搜索和选择
 * 
 * @param tags 标签列表
 * @param selectedTag 当前选中的标签
 * @param onDismiss 关闭弹窗回调
 * @param onSelectTag 选择标签回调
 * @param isLoading 是否正在加载标签
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterDialog(
    tags: List<BookmarkTagData>,
    selectedTag: String?,
    onDismiss: () -> Unit,
    onSelectTag: (String?) -> Unit,
    isLoading: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // 筛选后的标签列表
    val filteredTags = remember(tags, searchQuery) {
        if (searchQuery.isBlank()) {
            tags
        } else {
            tags.filter { it.tag.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.bookmark_tag_filter_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.common_close)
                        )
                    }
                }
                
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { 
                        Text(stringResource(Res.string.bookmark_tag_filter_search_hint)) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.search_clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors()
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // 标签列表
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(Res.string.bookmark_tag_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    tags.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.bookmark_tag_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    filteredTags.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.bookmark_tag_filter_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    else -> {
                        val listState = rememberLazyListState()
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            // 全部标签选项
                            item(key = "all_tags") {
                                TagFilterItem(
                                    tag = null,
                                    count = null,
                                    isSelected = selectedTag == null,
                                    onClick = {
                                        onSelectTag(null)
                                        onDismiss()
                                    }
                                )
                            }
                            
                            // 标签列表
                            items(
                                items = filteredTags,
                                key = { it.tag }
                            ) { tagData ->
                                TagFilterItem(
                                    tag = tagData.tag,
                                    count = tagData.count,
                                    isSelected = selectedTag == tagData.tag,
                                    onClick = {
                                        onSelectTag(tagData.tag)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 标签筛选项
 */
@Composable
private fun TagFilterItem(
    tag: String?,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tag == null) {
                // 全部标签
                Text(
                    text = stringResource(Res.string.bookmark_tag_filter_all),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // 具体标签
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}
