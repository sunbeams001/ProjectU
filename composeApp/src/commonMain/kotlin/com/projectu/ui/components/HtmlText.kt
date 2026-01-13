package com.projectu.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import io.ktor.http.decodeURLPart

/**
 * 解析简单HTML文本，支持 <br />, <a> 标签
 * 
 * @param html HTML文本
 * @param linkColor 链接颜色
 * @param onLinkClick 链接点击回调
 * @return 解析后的 AnnotatedString
 */
fun parseSimpleHtml(
    html: String, 
    linkColor: Color,
    onLinkClick: ((String) -> Unit)? = null
): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val text = html
        
        while (currentIndex < text.length) {
            // 查找下一个标签
            val tagStart = text.indexOf('<', currentIndex)
            
            if (tagStart == -1) {
                // 没有更多标签，添加剩余文本
                val remainingText = decodeHtmlEntities(text.substring(currentIndex))
                append(remainingText)
                break
            }
            
            // 添加标签前的文本
            if (tagStart > currentIndex) {
                val beforeTag = decodeHtmlEntities(text.substring(currentIndex, tagStart))
                append(beforeTag)
            }
            
            // 查找标签结束位置
            val tagEnd = text.indexOf('>', tagStart)
            if (tagEnd == -1) {
                // 无效标签，添加剩余文本
                val remainingText = decodeHtmlEntities(text.substring(currentIndex))
                append(remainingText)
                break
            }
            
            val tag = text.substring(tagStart, tagEnd + 1)
            
            when {
                // 处理 <br> 或 <br /> 标签
                tag.lowercase().matches(Regex("<br\\s*/?>")) -> {
                    append("\n")
                    currentIndex = tagEnd + 1
                }
                
                // 处理 <a> 标签
                tag.lowercase().startsWith("<a ") -> {
                    // 提取href
                    val hrefMatch = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        .find(tag)
                    val rawHref = hrefMatch?.groupValues?.get(1) ?: ""
                    // 处理 Pixiv 链接格式（跳转链接、相对路径等）
                    val resolvedUrl = resolvePixivUrl(rawHref)
                    
                    // 查找</a>结束标签
                    val closeTagIndex = text.indexOf("</a>", tagEnd, ignoreCase = true)
                    if (closeTagIndex != -1) {
                        val linkText = decodeHtmlEntities(text.substring(tagEnd + 1, closeTagIndex))
                        
                        // 使用新的 LinkAnnotation API
                        val link = if (onLinkClick != null) {
                            LinkAnnotation.Clickable(
                                tag = "URL",
                                linkInteractionListener = { onLinkClick(resolvedUrl) },
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        } else {
                            LinkAnnotation.Url(
                                url = resolvedUrl,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                            )
                        }
                        
                        withLink(link) {
                            append(linkText)
                        }
                        
                        currentIndex = closeTagIndex + 4 // 跳过 </a>
                    } else {
                        // 没有找到结束标签
                        currentIndex = tagEnd + 1
                    }
                }
                
                // 处理其他结束标签（忽略）
                tag.startsWith("</") -> {
                    currentIndex = tagEnd + 1
                }
                
                // 忽略其他标签
                else -> {
                    currentIndex = tagEnd + 1
                }
            }
        }
    }
}

/**
 * 解码HTML实体
 */
private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&#x27;", "'")
        .replace("&#x2F;", "/")
        .replace("&#34;", "\"")
        .replace("&#60;", "<")
        .replace("&#62;", ">")
}

/**
 * 处理 Pixiv 链接格式
 * 
 * Pixiv 的外部链接格式为: /jump.php?https%3A%2F%2Fexample.com
 * 需要提取并解码实际的 URL
 * 
 * @param href 原始 href 值
 * @return 处理后的完整 URL
 */
private fun resolvePixivUrl(href: String): String {
    return when {
        // 处理 /jump.php? 跳转链接
        href.startsWith("/jump.php?") -> {
            val encodedUrl = href.removePrefix("/jump.php?")
            try {
                encodedUrl.decodeURLPart()
            } catch (e: Exception) {
                // 解码失败，返回原始值
                "https://www.pixiv.net$href"
            }
        }
        // 处理相对路径
        href.startsWith("/") -> {
            "https://www.pixiv.net$href"
        }
        // 已经是完整 URL
        href.startsWith("http://") || href.startsWith("https://") -> {
            href
        }
        // 其他情况，尝试作为相对路径处理
        else -> {
            "https://www.pixiv.net/$href"
        }
    }
}

/**
 * 可点击的HTML富文本组件
 * 
 * @param html HTML文本内容
 * @param modifier Modifier
 * @param style 文本样式
 * @param maxLines 最大行数，null表示不限制
 * @param overflow 文本溢出处理方式
 * @param onClick 点击文本（非链接区域）的回调
 * @param onLinkClick 链接点击回调，如果为null则使用系统默认浏览器打开
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onClick: (() -> Unit)? = null,
    onLinkClick: ((url: String) -> Unit)? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    
    val annotatedString = remember(html, linkColor, onLinkClick) {
        parseSimpleHtml(html, linkColor, onLinkClick)
    }
    
    // 使用 Text 组件，它支持新的 LinkAnnotation
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = overflow
    )
}

/**
 * 将HTML文本转换为纯文本（移除所有标签）
 * 
 * @param html HTML文本
 * @return 纯文本
 */
fun htmlToPlainText(html: String): String {
    return html
        // 将 <br> 替换为换行
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        // 移除所有其他HTML标签
        .replace(Regex("<[^>]+>"), "")
        // 解码HTML实体
        .let { decodeHtmlEntities(it) }
        // 清理多余的空白
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
