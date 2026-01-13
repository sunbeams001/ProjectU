package com.projectu.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 统一的双层导航组件
 * 用于发现模块和排行榜模块的导航栏
 * 
 * 设计目标:
 * 1. 减少高度占用 (vertical padding 从 8.dp 减少到 6.dp)
 * 2. 统一标签颜色逻辑 (使用 FilterChip)
 * 3. 统一背景色 (都使用 surfaceContainer)
 */

/**
 * 单层导航栏
 * 
 * @param items 导航项列表
 * @param selectedIndex 当前选中的索引
 * @param onItemClick 点击回调，参数为索引
 * @param itemContent 导航项内容，参数为索引和是否选中
 * @param trailingContent 尾部内容（可选）
 * @param modifier 修饰符
 */
@Composable
fun <T> NavigationBar(
    items: List<T>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    
    // 存储Row容器的坐标信息
    var rowCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    // 存储每个chip的坐标信息
    val chipCoordinatesList = remember { mutableStateMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>() }
    
    // 当选中项变化且布局信息可用时，进行精确滚动
    LaunchedEffect(selectedIndex, chipCoordinatesList.size) {
        if (selectedIndex >= 0 && selectedIndex < items.size) {
            // 稍微延迟以确保布局完成
            delay(50)
            
            val chipCoords = chipCoordinatesList[selectedIndex]
            val rowCoords = rowCoordinates
            
            if (chipCoords != null && chipCoords.isAttached && rowCoords != null && rowCoords.isAttached) {
                // 使用实际的布局信息
                val chipPositionInRow = rowCoords.localPositionOf(chipCoords, androidx.compose.ui.geometry.Offset.Zero)
                val chipX = chipPositionInRow.x
                val chipWidth = chipCoords.size.width.toFloat()
                val chipCenter = chipX + chipWidth / 2
                val viewportWidth = scrollState.viewportSize.toFloat()
                
                // 计算滚动位置：让chip居中显示
                val idealScrollPosition = chipCenter - viewportWidth / 2
                val scrollPosition = idealScrollPosition.coerceIn(
                    0f,
                    scrollState.maxValue.toFloat()
                ).toInt()
                
                scrollState.animateScrollTo(scrollPosition)
            } else {
                // 如果还没有布局信息，使用估算值
                with(density) {
                    val chipWidthDp = 90.dp.toPx()
                    val spacingDp = 8.dp.toPx()
                    val itemWidth = chipWidthDp + spacingDp
                    
                    val chipX = selectedIndex * itemWidth
                    val chipCenter = chipX + chipWidthDp / 2
                    val viewportWidth = scrollState.viewportSize.toFloat()
                    
                    val idealScrollPosition = chipCenter - viewportWidth / 2
                    val scrollPosition = idealScrollPosition.coerceIn(
                        0f,
                        scrollState.maxValue.toFloat()
                    ).toInt()
                    
                    scrollState.animateScrollTo(scrollPosition)
                }
            }
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：可滚动的导航项
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .onGloballyPositioned { coordinates ->
                        rowCoordinates = coordinates
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            chipCoordinatesList[index] = coordinates
                        }
                    ) {
                        itemContent(item, index == selectedIndex)
                    }
                }
            }
            
            // 右侧：尾部内容（可选）
            trailingContent?.invoke()
        }
    }
}

/**
 * 简化的单层导航栏（只有导航项，无尾部内容）
 * 
 * @param items 导航项列表
 * @param selectedIndex 当前选中的索引
 * @param onItemClick 点击回调，参数为索引
 * @param getItemLabel 获取导航项标签文本的 Composable 函数
 * @param trailingContent 尾部内容（可选）
 * @param modifier 修饰符
 */
@Composable
fun <T> SimpleNavigationBar(
    items: List<T>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    getItemLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    
    // 存储Row容器的坐标信息
    var rowCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    // 存储每个chip的坐标信息
    val chipCoordinatesList = remember { mutableStateMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>() }
    
    // 当选中项变化且布局信息可用时，进行精确滚动
    LaunchedEffect(selectedIndex, chipCoordinatesList.size) {
        if (selectedIndex >= 0 && selectedIndex < items.size) {
            // 稍微延迟以确保布局完成
            delay(50)
            
            val chipCoords = chipCoordinatesList[selectedIndex]
            val rowCoords = rowCoordinates
            
            if (chipCoords != null && chipCoords.isAttached && rowCoords != null && rowCoords.isAttached) {
                // 使用实际的布局信息
                val chipPositionInRow = rowCoords.localPositionOf(chipCoords, androidx.compose.ui.geometry.Offset.Zero)
                val chipX = chipPositionInRow.x
                val chipWidth = chipCoords.size.width.toFloat()
                val chipCenter = chipX + chipWidth / 2
                val viewportWidth = scrollState.viewportSize.toFloat()
                
                // 计算滚动位置：让chip居中显示
                val idealScrollPosition = chipCenter - viewportWidth / 2
                val scrollPosition = idealScrollPosition.coerceIn(
                    0f,
                    scrollState.maxValue.toFloat()
                ).toInt()
                
                scrollState.animateScrollTo(scrollPosition)
            } else {
                // 如果还没有布局信息，使用估算值
                with(density) {
                    val chipWidthDp = 90.dp.toPx()
                    val spacingDp = 8.dp.toPx()
                    val itemWidth = chipWidthDp + spacingDp
                    
                    val chipX = selectedIndex * itemWidth
                    val chipCenter = chipX + chipWidthDp / 2
                    val viewportWidth = scrollState.viewportSize.toFloat()
                    
                    val idealScrollPosition = chipCenter - viewportWidth / 2
                    val scrollPosition = idealScrollPosition.coerceIn(
                        0f,
                        scrollState.maxValue.toFloat()
                    ).toInt()
                    
                    scrollState.animateScrollTo(scrollPosition)
                }
            }
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：可滚动的导航项
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .onGloballyPositioned { coordinates ->
                        rowCoordinates = coordinates
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { index, item ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onItemClick(index) },
                        label = { Text(text = getItemLabel(item)) },
                        modifier = Modifier
                            .height(30.dp)
                            .onGloballyPositioned { coordinates ->
                            chipCoordinatesList[index] = coordinates
                        }
                    )
                }
            }
            
            // 右侧：尾部内容（可选）
            trailingContent?.invoke()
        }
    }
}

/**
 * 双层Tab导航栏（Tab + FilterChip）
 * 用于优化视觉层次，第一层使用 Material 3 TabRow，第二层使用 FilterChip
 * 
 * 设计特点：
 * - 第1层：TabRow（48dp），有下划线指示器，表示主要分类
 * - 第2层：FilterChip（44dp），表示筛选条件或次级分类
 * - 总高度：92dp，视觉层次清晰
 * 
 * @param primaryItems 第一层导航项列表
 * @param primarySelectedIndex 第一层当前选中的索引
 * @param onPrimaryItemClick 第一层点击回调
 * @param getPrimaryItemLabel 获取第一层导航项标签文本
 * @param secondaryItems 第二层导航项列表（可选）
 * @param secondarySelectedIndex 第二层当前选中的索引
 * @param onSecondaryItemClick 第二层点击回调
 * @param getSecondaryItemLabel 获取第二层导航项标签文本
 * @param showSecondaryNav 是否显示第二层导航
 * @param modifier 修饰符
 */
@Composable
fun <T, S> TabbedNavigationBar(
    primaryItems: List<T>,
    primarySelectedIndex: Int,
    onPrimaryItemClick: (Int) -> Unit,
    getPrimaryItemLabel: @Composable (T) -> String,
    secondaryItems: List<S>? = null,
    secondarySelectedIndex: Int = 0,
    onSecondaryItemClick: ((Int) -> Unit)? = null,
    getSecondaryItemLabel: (@Composable (S) -> String)? = null,
    showSecondaryNav: Boolean = true,
    primaryTrailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 第一层：ScrollableTabRow（支持横向滚动）+ 尾部内容
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),  // 固定高度，与 Tab 高度一致，避免尾部内容撑高
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = primarySelectedIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    primaryItems.forEachIndexed { index, item ->
                        Tab(
                            selected = primarySelectedIndex == index,
                            onClick = { onPrimaryItemClick(index) },
                            text = { 
                                Text(
                                    text = getPrimaryItemLabel(item),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(40.dp)  // 减小Tab高度（默认48dp）
                        )
                    }
                }
                
                // 尾部内容（如日期选择器）
                primaryTrailingContent?.let {
                    Box(
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        it()
                    }
                }
            }
        }
        
        // 第二层：FilterChip（条件显示）
        if (showSecondaryNav && secondaryItems != null && onSecondaryItemClick != null && getSecondaryItemLabel != null) {
            SimpleNavigationBar(
                items = secondaryItems,
                selectedIndex = secondarySelectedIndex,
                onItemClick = onSecondaryItemClick,
                getItemLabel = getSecondaryItemLabel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

