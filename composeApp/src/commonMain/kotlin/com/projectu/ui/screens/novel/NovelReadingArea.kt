package com.projectu.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.projectu.shared.domain.model.NovelEmbeddedImageInfo
import com.projectu.ui.util.NovelContentParser
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 小说阅读区域
 * 
 * 上方区域用于展示小说内容，支持翻页操作
 * 
 * @param pages 解析后的页面列表
 * @param currentPage 当前页码（从1开始）
 * @param embeddedImages 内嵌图片映射（imageId -> 图片信息）
 * @param displayMode 显示模式（原文/翻译/对照）
 * @param pageTranslations 页面翻译缓存（页码 -> 翻译内容）
 * @param translatingPages 正在翻译的页面集合
 * @param onPreviousPage 上一页回调
 * @param onNextPage 下一页回调
 * @param onToggleInfo 切换信息区域回调
 * @param savedScrollPosition 保存的滚动位置（firstVisibleItemIndex, firstVisibleItemScrollOffset）
 * @param onScrollPositionChanged 滚动位置变化回调
 * @param modifier 修饰符
 */
@Composable
fun NovelReadingArea(
    pages: List<NovelContentParser.NovelPage>,
    currentPage: Int,
    embeddedImages: Map<String, NovelEmbeddedImageInfo> = emptyMap(),
    displayMode: NovelDisplayMode = NovelDisplayMode.ORIGINAL,
    pageTranslations: Map<Int, String> = emptyMap(),
    translatingPages: Set<Int> = emptySet(),
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleInfo: () -> Unit,
    savedScrollPosition: Pair<Int, Int>? = null,
    onScrollPositionChanged: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 读取阅读设置
    val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository = org.koin.compose.koinInject()
    val settings by settingsRepository.getSettings().collectAsState(com.projectu.shared.data.local.AppSettings.DEFAULT)
    
    // 计算最终使用的颜色
    val finalBackgroundColor = remember(settings.novelBackgroundColor, settings.novelBackgroundScheme) {
        when {
            settings.novelBackgroundScheme == com.projectu.shared.data.local.NovelBackgroundScheme.CUSTOM && settings.novelBackgroundColor != null -> 
                parseColor(settings.novelBackgroundColor)
            settings.novelBackgroundScheme.backgroundColor != null -> 
                parseColor(settings.novelBackgroundScheme.backgroundColor)
            else -> null
        }
    }
    
    val finalTextColor = remember(settings.novelTextColor, settings.novelBackgroundScheme) {
        when {
            settings.novelBackgroundScheme == com.projectu.shared.data.local.NovelBackgroundScheme.CUSTOM && settings.novelTextColor != null -> 
                parseColor(settings.novelTextColor)
            settings.novelBackgroundScheme.textColor != null -> 
                parseColor(settings.novelBackgroundScheme.textColor)
            else -> null
        }
    }
    
    val totalPages = pages.size.coerceAtLeast(1)
    val hasMultiplePages = totalPages > 1
    val canGoPrevious = currentPage > 1
    val canGoNext = currentPage < totalPages
    
    val currentPageContent = pages.getOrNull(currentPage - 1)
    
    // 使用保存的滚动位置或默认位置
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollPosition?.first ?: 0,
        initialFirstVisibleItemScrollOffset = savedScrollPosition?.second ?: 0
    )
    
    // 页面变化时恢复该页面的滚动位置
    LaunchedEffect(currentPage) {
        val position = savedScrollPosition
        if (position != null) {
            listState.scrollToItem(position.first, position.second)
        } else {
            listState.scrollToItem(0)
        }
    }
    
    // 监听滚动位置变化并保存
    LaunchedEffect(listState) {
        snapshotFlow { 
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset 
        }.collect { (index, offset) ->
            onScrollPositionChanged?.invoke(index, offset)
        }
    }
    
    // 计算当前页面的滚动进度（0~1）
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            
            if (totalItemsCount == 0 || visibleItems.isEmpty()) {
                0f
            } else {
                // 计算内容的总高度和已滚动高度
                val firstItem = visibleItems.first()
                val lastItem = visibleItems.last()
                
                // 当只有一屏或更少内容时，直接显示100%
                if (firstItem.index == 0 && lastItem.index == totalItemsCount - 1) {
                    1f
                } else {
                    // 估算总内容高度（假设每个item高度相近）
                    val averageItemHeight = visibleItems.sumOf { it.size } / visibleItems.size.toFloat()
                    val estimatedTotalHeight = averageItemHeight * totalItemsCount
                    
                    // 计算视口高度
                    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    
                    // 最大可滚动距离
                    val maxScrollDistance = (estimatedTotalHeight - viewportHeight).coerceAtLeast(1f)
                    
                    // 当前滚动位置
                    val scrolledDistance = firstItem.index * averageItemHeight + 
                        (if (firstItem.index == 0) 0f else -firstItem.offset.toFloat())
                    
                    // 当最后一项可见时，使用更精确的计算
                    if (lastItem.index == totalItemsCount - 1) {
                        // 检查最后一项是否完全可见
                        val lastItemBottom = lastItem.offset + lastItem.size
                        val viewportEnd = layoutInfo.viewportEndOffset
                        if (lastItemBottom <= viewportEnd) {
                            1f
                        } else {
                            (scrolledDistance / maxScrollDistance).coerceIn(0f, 1f)
                        }
                    } else {
                        (scrolledDistance / maxScrollDistance).coerceIn(0f, 1f)
                    }
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(finalBackgroundColor ?: MaterialTheme.colorScheme.surface)
    ) {
        // 主内容区域（可点击切换区域）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canGoPrevious, canGoNext) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val tapX = offset.x
                        
                        when {
                            // 点击左侧 1/3 区域 - 上一页
                            tapX < width / 3 && canGoPrevious -> onPreviousPage()
                            // 点击右侧 1/3 区域 - 下一页
                            tapX > width * 2 / 3 && canGoNext -> onNextPage()
                            // 点击中间区域 - 显示/隐藏信息
                            else -> onToggleInfo()
                        }
                    }
                }
        ) {
            if (currentPageContent != null) {
                // 根据显示模式显示内容
                when (displayMode) {
                    NovelDisplayMode.ORIGINAL -> {
                        // 仅显示原文
                        NovelPageContent(
                            page = currentPageContent,
                            embeddedImages = embeddedImages,
                            listState = listState,
                            fontSize = settings.novelFontSize.sp.sp,
                            textColor = finalTextColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    NovelDisplayMode.TRANSLATED -> {
                        // 仅显示翻译
                        val isTranslating = currentPage in translatingPages
                        val translation = pageTranslations[currentPage]
                        
                        if (isTranslating) {
                            TranslatingIndicator()
                        } else if (translation != null) {
                            TranslatedPageContent(
                                translatedText = translation,
                                listState = listState,
                                fontSize = settings.novelFontSize.sp.sp,
                                textColor = finalTextColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            EmptyTranslationPlaceholder()
                        }
                    }
                    
                    NovelDisplayMode.BILINGUAL -> {
                        // 对照显示
                        val isTranslating = currentPage in translatingPages
                        val translation = pageTranslations[currentPage]
                        
                        BilingualPageContent(
                            page = currentPageContent,
                            translation = translation,
                            isTranslating = isTranslating,
                            embeddedImages = embeddedImages,
                            listState = listState,
                            fontSize = settings.novelFontSize.sp.sp,
                            textColor = finalTextColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // 空内容提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.novel_no_content),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 右侧阅读进度条（基于综合滚动进度）
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .padding(top = 48.dp, bottom = 16.dp)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(scrollProgress.coerceIn(0f, 1f))
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
        
        // 右下角页码显示（仅多页时显示）
        if (hasMultiplePages) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 单页内容展示
 */
@Composable
private fun NovelPageContent(
    page: NovelContentParser.NovelPage,
    embeddedImages: Map<String, NovelEmbeddedImageInfo>,
    listState: LazyListState,
    fontSize: androidx.compose.ui.unit.TextUnit,
    textColor: Color?,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    
    // 使用 SelectionContainer 支持长按选择文本
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 16.dp),
            // 顶部增加额外的内边距，避免被左上角返回按钮遮挡
            // 右侧进度条是覆盖层，不影响文字布局，所以左右对称
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(page.elements) { element ->
                ContentElementDisplay(
                    element = element,
                    embeddedImages = embeddedImages,
                    fontSize = fontSize,
                    textColor = textColor,
                    onLinkClick = { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // 忽略无法打开的链接
                        }
                    }
                )
            }
        }
    }
}

// URL正则表达式
private val urlRegex = Regex("""https?://[\w\-._~:/?#\[\]@!$&'()*+,;=%]+""")

/**
 * 将文本中的URL转换为可点击的AnnotatedString
 */
private fun buildClickableText(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
    linkColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        urlRegex.findAll(text).forEach { match ->
            // 添加URL之前的普通文本
            if (match.range.first > lastIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex, match.range.first))
                }
            }
            // 添加URL链接
            pushStringAnnotation(tag = "URL", annotation = match.value)
            withStyle(SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )) {
                append(match.value)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        // 添加最后一部分普通文本
        if (lastIndex < text.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(lastIndex))
            }
        }
    }
}

/**
 * 内容元素显示
 */
@Composable
private fun ContentElementDisplay(
    element: NovelContentParser.ContentElement,
    embeddedImages: Map<String, NovelEmbeddedImageInfo>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    textColor: Color?,
    onLinkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (element) {
        is NovelContentParser.ContentElement.Text -> {
            val linkColor = MaterialTheme.colorScheme.primary
            val displayTextColor = textColor ?: MaterialTheme.colorScheme.onSurface
            val annotatedString = remember(element.content, displayTextColor, linkColor) {
                buildClickableText(element.content, displayTextColor, linkColor)
            }
            
            // 检查是否包含链接
            val hasLinks = annotatedString.getStringAnnotations("URL", 0, annotatedString.length).isNotEmpty()
            
            if (hasLinks) {
                @Suppress("DEPRECATION")
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.75f
                    ),
                    modifier = modifier.fillMaxWidth(),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                onLinkClick(annotation.item)
                            }
                    }
                )
            } else {
                Text(
                    text = element.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.75f
                    ),
                    color = displayTextColor,
                    modifier = modifier.fillMaxWidth()
                )
            }
        }
        
        is NovelContentParser.ContentElement.ChapterTitle -> {
            Text(
                text = element.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
        
        is NovelContentParser.ContentElement.PixivImage -> {
            // Pixiv 插画引用 - 显示占位符
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.novel_pixiv_illust, element.illustId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        is NovelContentParser.ContentElement.UploadedImage -> {
            // 上传的内嵌图片 - 从 embeddedImages 获取 URL 加载
            val imageInfo = embeddedImages[element.imageId]
            val imageUrl = imageInfo?.largeUrl ?: imageInfo?.mediumUrl ?: imageInfo?.originalUrl
            
            if (imageUrl != null) {
                // 有图片 URL，加载图片
                var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
                
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(Res.string.novel_illust_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 400.dp),
                        contentScale = ContentScale.FillWidth,
                        onState = { imageState = it }
                    )
                    
                    // 加载状态
                    when (imageState) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(Res.string.error_image_load_failed),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        else -> { /* Success or Empty - do nothing */ }
                    }
                }
            } else {
                // 没有图片 URL，显示占位符
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.novel_illust, element.imageId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        is NovelContentParser.ContentElement.JumpLink -> {
            Text(
                text = element.displayText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { 
                        onLinkClick(element.url)
                    }
            )
        }
        
        is NovelContentParser.ContentElement.Ruby -> {
            // Ruby注音使用标注字符串
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 16.sp)) {
                        append(element.text)
                    }
                    withStyle(SpanStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append("(${element.reading})")
                    }
                },
                modifier = modifier
            )
        }
        
        NovelContentParser.ContentElement.EmptyLine -> {
            Spacer(modifier = modifier.height(16.dp))
        }
    }
}

/**
 * 解析十六进制颜色字符串为Color对象
 */
private fun parseColor(hex: String?): Color? {
    return try {
        if (hex != null && hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            val colorInt = hex.substring(1).toLong(16).toInt()
            Color(0xFF000000 or colorInt.toLong())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 翻译进度指示器
 */
@Composable
private fun TranslatingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.translating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空翻译占位符
 */
@Composable
private fun EmptyTranslationPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.translation_not_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 纯翻译页面内容
 */
@Composable
private fun TranslatedPageContent(
    translatedText: String,
    listState: LazyListState,
    fontSize: androidx.compose.ui.unit.TextUnit,
    textColor: Color?,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = translatedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.8f,
                        color = textColor ?: MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 对照页面内容（原文+翻译）
 */
@Composable
private fun BilingualPageContent(
    page: NovelContentParser.NovelPage,
    translation: String?,
    isTranslating: Boolean,
    embeddedImages: Map<String, NovelEmbeddedImageInfo>,
    listState: LazyListState,
    fontSize: androidx.compose.ui.unit.TextUnit,
    textColor: Color?,
    modifier: Modifier = Modifier
) {
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 原文部分
            item {
                Text(
                    text = stringResource(Res.string.description_original),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(page.elements) { element ->
                ContentElementDisplay(
                    element = element,
                    embeddedImages = embeddedImages,
                    fontSize = fontSize,
                    textColor = textColor,
                    onLinkClick = { }
                )
            }
            
            // 分隔线
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            
            // 翻译部分
            item {
                Text(
                    text = stringResource(Res.string.description_translated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                if (isTranslating) {
                    TranslatingIndicator()
                } else if (translation != null) {
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.8f,
                            color = textColor ?: MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    EmptyTranslationPlaceholder()
                }
            }
        }
    }
}
