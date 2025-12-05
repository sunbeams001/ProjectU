package com.projectu.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.projectu.shared.domain.model.EmojiConfig
import com.projectu.shared.domain.model.EmojiTextSegment

/**
 * 渲染包含 emoji 表情的文本
 * 
 * 解析文本中的 emoji 标签（如 "(normal)"）并显示为图片
 * 
 * @param text 包含 emoji 标签的原始文本
 * @param modifier Modifier
 * @param style 文本样式
 * @param color 文本颜色
 * @param maxLines 最大行数
 * @param overflow 文本溢出处理
 * @param emojiSize emoji 图片大小（sp）
 */
@Composable
fun EmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    emojiSize: TextUnit = 18.sp
) {
    val context = LocalPlatformContext.current
    
    // 解析文本中的 emoji
    val segments = remember(text) {
        EmojiConfig.parseEmojiText(text)
    }
    
    // 如果没有 emoji，直接显示普通文本
    if (segments.all { it is EmojiTextSegment.Text }) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }
    
    // 构建 AnnotatedString 和 InlineContent
    val (annotatedString, inlineContent) = remember(segments, emojiSize) {
        buildEmojiAnnotatedString(segments, emojiSize)
    }
    
    // 为每个 emoji 创建 InlineTextContent
    val inlineContentMap = remember(inlineContent, emojiSize, context) {
        inlineContent.associate { (id, url) ->
            id to InlineTextContent(
                placeholder = Placeholder(
                    width = emojiSize,
                    height = emojiSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .memoryCacheKey("emoji_inline_$id")
                        .diskCacheKey("emoji_inline_$id")
                        .build(),
                    contentDescription = id,
                    modifier = Modifier
                        .size(with(androidx.compose.ui.platform.LocalDensity.current) { emojiSize.toDp() })
                        .padding(horizontal = 1.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContentMap
    )
}

/**
 * 构建包含 emoji 占位符的 AnnotatedString
 */
private fun buildEmojiAnnotatedString(
    segments: List<EmojiTextSegment>,
    emojiSize: TextUnit
): Pair<AnnotatedString, List<Pair<String, String>>> {
    val inlineContent = mutableListOf<Pair<String, String>>()
    
    val annotatedString = buildAnnotatedString {
        segments.forEach { segment ->
            when (segment) {
                is EmojiTextSegment.Text -> {
                    append(segment.text)
                }
                is EmojiTextSegment.EmojiImage -> {
                    val id = segment.emoji.label
                    appendInlineContent(id, id)
                    inlineContent.add(id to segment.emoji.url)
                }
            }
        }
    }
    
    return annotatedString to inlineContent
}

/**
 * 检查文本是否包含 emoji 标签
 */
fun String.containsEmoji(): Boolean {
    return EmojiConfig.emojiPattern.containsMatchIn(this)
}
