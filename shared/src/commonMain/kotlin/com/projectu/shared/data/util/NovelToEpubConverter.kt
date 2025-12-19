@file:Suppress("HardcodedChinese") // Chinese punctuation marks (《》「」) are used for functional text processing in parsePageContent

package com.projectu.shared.data.util

import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelEmbeddedImageInfo
import com.projectu.shared.domain.model.getUrlByQuality
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 小说内容转换器
 * 将 Pixiv 小说格式转换为 EPUB XHTML 格式
 * 
 * 处理的标签：
 * - [newpage] - 分页标记（转换为新章节）
 * - [chapter:《标题》] - 章节标题（转换为 <h2>）
 * - [pixivimage:id] - Pixiv插画引用（下载并嵌入）
 * - [uploadedimage:id] - 上传的内嵌图片（从embeddedImages获取URL并下载）
 * - [[jumpuri:显示文本>URL]] - 跳转链接（转换为 <a>）
 * - [[rb:文本>假名]] - Ruby注音（转换为 <ruby>）
 * 
 * @param httpClient HTTP客户端
 * @param settingsCache 设置缓存
 * @param formatPageTitle 格式化页面标题的函数，接收页码返回本地化的标题（例如：formatPageTitle(1) -> "第1页" 或 "Page 1"）
 */
class NovelToEpubConverter(
    private val httpClient: HttpClient,
    private val settingsCache: SettingsCache,
    private val formatPageTitle: (pageNumber: Int) -> String = { pageNumber -> "Page $pageNumber" }
) {
    /**
     * 解析后的章节
     * @param parentId 父章节ID，用于创建多级目录结构。如果为null，表示顶层章节
     */
    data class ParsedChapter(
        val id: String,
        val title: String,
        val htmlContent: String,
        val order: Int,
        val parentId: String? = null
    )
    
    /**
     * 解析后的图片
     */
    data class ParsedImage(
        val id: String,
        val fileName: String,
        val data: ByteArray,
        val mimeType: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as ParsedImage
            return id == other.id && fileName == other.fileName && mimeType == other.mimeType
        }
        
        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }
    
    /**
     * 转换结果
     */
    data class ConversionResult(
        val chapters: List<ParsedChapter>,
        val images: List<ParsedImage>
    )
    
    // 正则表达式模式
    private val NEW_PAGE_PATTERN = Regex("""\[newpage\]""", RegexOption.IGNORE_CASE)
    private val CHAPTER_PATTERN = Regex("""\[chapter:([^\]]+)\]""")
    private val PIXIV_IMAGE_PATTERN = Regex("""\[pixivimage:(\d+)(?:-(\d+))?\]""")
    private val UPLOADED_IMAGE_PATTERN = Regex("""\[uploadedimage:(\d+)\]""")
    private val JUMP_URI_PATTERN = Regex("""\[\[jumpuri:([^>\]]+?)\s*>\s*([^\]]+)\]\]""")
    private val RUBY_PATTERN = Regex("""\[\[rb:([^>]+)>([^\]]+)\]\]""")
    
    /**
     * 转换小说内容为 EPUB 格式
     * 
     * @param novel 小说对象
     * @param downloadImages 是否下载图片（如果为false，图片标签将被移除）
     * @return 转换结果
     */
    suspend fun convert(
        novel: Novel,
        downloadImages: Boolean = true
    ): ConversionResult = withContext(Dispatchers.IO) {
        val content = novel.content ?: ""
        val embeddedImages = novel.embeddedImages
        
        // 按 [newpage] 分割页面
        val pageTexts = NEW_PAGE_PATTERN.split(content)
        
        val chapters = mutableListOf<ParsedChapter>()
        val images = mutableListOf<ParsedImage>()
        val downloadedImageIds = mutableSetOf<String>()
        
        pageTexts.forEachIndexed { index, pageText ->
            val trimmedText = pageText.trim()
            if (trimmedText.isEmpty()) return@forEachIndexed
            
            // 解析章节内容
            val (htmlContent, pageImages) = parsePageContent(
                pageText = trimmedText,
                embeddedImages = embeddedImages,
                downloadImages = downloadImages,
                downloadedImageIds = downloadedImageIds
            )
            
            // 添加章节
            val chapterTitle = if (pageTexts.size > 1) {
                "${novel.title} - ${formatPageTitle(index + 1)}"
            } else {
                novel.title
            }
            
            chapters.add(
                ParsedChapter(
                    id = "chapter${index + 1}",
                    title = chapterTitle,
                    htmlContent = htmlContent,
                    order = index
                )
            )
            
            // 添加图片
            images.addAll(pageImages)
        }
        
        ConversionResult(chapters, images)
    }
    
    /**
     * 转换小说系列为 EPUB 格式
     * 
     * @param novels 系列中的小说列表（按顺序）
     * @param seriesTitle 系列标题
     * @param downloadImages 是否下载图片
     * @return 转换结果
     */
    suspend fun convertSeries(
        novels: List<Novel>,
        seriesTitle: String,
        downloadImages: Boolean = true
    ): ConversionResult = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<ParsedChapter>()
        val images = mutableListOf<ParsedImage>()
        val downloadedImageIds = mutableSetOf<String>()
        
        var chapterIndex = 0
        
        novels.forEachIndexed { novelIndex, novel ->
            val content = novel.content ?: ""
            val embeddedImages = novel.embeddedImages
            
            // 按 [newpage] 分割页面
            val pageTexts = NEW_PAGE_PATTERN.split(content)
            
            if (pageTexts.size > 1) {
                // 多页小说：创建父章节（空内容）和子章节（所有分页）
                val parentId = "chapter${chapterIndex + 1}"
                
                // 创建父章节（只作为目录节点，内容为空）
                chapters.add(
                    ParsedChapter(
                        id = parentId,
                        title = novel.title,
                        htmlContent = "<p>${escapeHtml(novel.title)}</p>",
                        order = chapterIndex,
                        parentId = null
                    )
                )
                chapterIndex++
                
                // 创建子章节（所有分页，从第1页开始）
                pageTexts.forEachIndexed { pageIndex, pageText ->
                    val trimmedText = pageText.trim()
                    if (trimmedText.isEmpty()) return@forEachIndexed
                    
                    val (htmlContent, pageImages) = parsePageContent(
                        pageText = trimmedText,
                        embeddedImages = embeddedImages,
                        downloadImages = downloadImages,
                        downloadedImageIds = downloadedImageIds
                    )
                    
                    chapters.add(
                        ParsedChapter(
                            id = "chapter${chapterIndex + 1}",
                            title = formatPageTitle(pageIndex + 1),
                            htmlContent = htmlContent,
                            order = chapterIndex,
                            parentId = parentId
                        )
                    )
                    
                    images.addAll(pageImages)
                    chapterIndex++
                }
            } else {
                // 单页小说：只创建顶层章节
                pageTexts.forEachIndexed { pageIndex, pageText ->
                    val trimmedText = pageText.trim()
                    if (trimmedText.isEmpty()) return@forEachIndexed
                    
                    val (htmlContent, pageImages) = parsePageContent(
                        pageText = trimmedText,
                        embeddedImages = embeddedImages,
                        downloadImages = downloadImages,
                        downloadedImageIds = downloadedImageIds
                    )
                    
                    chapters.add(
                        ParsedChapter(
                            id = "chapter${chapterIndex + 1}",
                            title = novel.title,
                            htmlContent = htmlContent,
                            order = chapterIndex,
                            parentId = null
                        )
                    )
                    
                    images.addAll(pageImages)
                    chapterIndex++
                }
            }
        }
        
        ConversionResult(chapters, images)
    }
    
    /**
     * 解析页面内容为 HTML
     */
    private suspend fun parsePageContent(
        pageText: String,
        embeddedImages: Map<String, NovelEmbeddedImageInfo>,
        downloadImages: Boolean,
        downloadedImageIds: MutableSet<String>
    ): Pair<String, List<ParsedImage>> {
        val images = mutableListOf<ParsedImage>()
        var remainingText = pageText
        val htmlBuilder = StringBuilder()
        
        while (remainingText.isNotEmpty()) {
            // 查找最近的标签
            val chapterMatch = CHAPTER_PATTERN.find(remainingText)
            val pixivImageMatch = PIXIV_IMAGE_PATTERN.find(remainingText)
            val uploadedImageMatch = UPLOADED_IMAGE_PATTERN.find(remainingText)
            val jumpMatch = JUMP_URI_PATTERN.find(remainingText)
            val rubyMatch = RUBY_PATTERN.find(remainingText)
            
            // 找到最近的匹配
            val matches = listOfNotNull(chapterMatch, pixivImageMatch, uploadedImageMatch, jumpMatch, rubyMatch)
            val nearestMatch = matches.minByOrNull { it.range.first }
            
            if (nearestMatch == null) {
                // 没有更多标签，添加剩余文本
                appendTextAsHtml(remainingText, htmlBuilder)
                break
            }
            
            // 添加标签前的文本
            if (nearestMatch.range.first > 0) {
                val textBefore = remainingText.substring(0, nearestMatch.range.first)
                appendTextAsHtml(textBefore, htmlBuilder)
            }
            
            // 处理匹配的标签
            when (nearestMatch) {
                chapterMatch -> {
                    val title = nearestMatch.groupValues[1]
                        .removeSurrounding("《", "》")
                        .removeSurrounding("「", "」")
                        .trim()
                    htmlBuilder.append("""<h2 class="chapter-title">${escapeHtml(title)}</h2>""").append("\n")
                }
                
                pixivImageMatch -> {
                    val illustId = nearestMatch.groupValues[1]
                    val pageIndex = nearestMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                    
                    if (downloadImages) {
                        // TODO: 下载 Pixiv 插画需要实现插画 API 调用
                        // 暂时跳过 pixivimage 标签
                        htmlBuilder.append("""<p class="image-placeholder">[Illustration ID: $illustId${if (pageIndex > 0) "-$pageIndex" else ""}]</p>""").append("\n")
                    }
                }
                
                uploadedImageMatch -> {
                    val imageId = nearestMatch.groupValues[1]
                    val imageInfo = embeddedImages[imageId]
                    
                    if (downloadImages && imageInfo != null && imageId !in downloadedImageIds) {
                        // 根据用户配置获取图片质量
                        val preferredQuality = settingsCache.getNovelDownloadImageQuality()
                        val imageUrl = imageInfo.getUrlByQuality(preferredQuality)
                        if (imageUrl != null) {
                            try {
                                val imageData = downloadImage(imageUrl)
                                val fileName = "image_$imageId.jpg"
                                images.add(
                                    ParsedImage(
                                        id = "img_$imageId",
                                        fileName = fileName,
                                        data = imageData,
                                        mimeType = "image/jpeg"
                                    )
                                )
                                downloadedImageIds.add(imageId)
                                
                                htmlBuilder.append("""<div class="image"><img src="../Images/$fileName" alt="Image $imageId"/></div>""").append("\n")
                            } catch (e: Exception) {
                                // 下载失败，跳过
                                htmlBuilder.append("""<p class="image-placeholder">[Image download failed: $imageId]</p>""").append("\n")
                            }
                        }
                    } else if (!downloadImages) {
                        // 不下载图片，添加占位符
                        htmlBuilder.append("""<p class="image-placeholder">[Image ID: $imageId]</p>""").append("\n")
                    }
                }
                
                jumpMatch -> {
                    val displayText = nearestMatch.groupValues[1].trim()
                    val url = nearestMatch.groupValues[2].trim()
                    htmlBuilder.append("""<a href="${escapeHtml(url)}" class="link">${escapeHtml(displayText)}</a>""")
                }
                
                rubyMatch -> {
                    val text = nearestMatch.groupValues[1]
                    val reading = nearestMatch.groupValues[2]
                    htmlBuilder.append("""<ruby>${escapeHtml(text)}<rt>${escapeHtml(reading)}</rt></ruby>""")
                }
            }
            
            // 继续处理剩余文本
            remainingText = remainingText.substring(nearestMatch.range.last + 1)
        }
        
        return Pair(htmlBuilder.toString(), images)
    }
    
    /**
     * 将文本转换为 HTML 段落
     */
    private fun appendTextAsHtml(text: String, builder: StringBuilder) {
        if (text.isEmpty()) return
        
        // 检查是否只包含空白字符（包括换行）
        if (text.isBlank()) {
            // 计算换行符数量
            val lineBreaks = text.count { it == '\n' }
            // 添加相应数量的 <br/>
            repeat(lineBreaks) {
                builder.append("<br/>")
            }
            return
        }
        
        // 按空行分割段落
        val paragraphs = text.split(Regex("\n\n+"))
        
        paragraphs.forEach { paragraph ->
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isNotEmpty()) {
                // 保留段落内的换行符（用 <br/> 替换）
                val lines = trimmedParagraph.split("\n")
                builder.append("<p>")
                lines.forEachIndexed { index, line ->
                    builder.append(escapeHtml(line))
                    if (index < lines.size - 1) {
                        builder.append("<br/>")
                    }
                }
                builder.append("</p>").append("\n")
            }
        }
    }
    
    /**
     * 下载图片
     */
    private suspend fun downloadImage(url: String): ByteArray = withContext(Dispatchers.IO) {
        val response = httpClient.get(url) {
            headers.append("Referer", "https://www.pixiv.net/")
        }
        
        val channel = response.bodyAsChannel()
        val buffer = mutableListOf<Byte>()
        val tempBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
        
        while (!channel.isClosedForRead) {
            val bytesRead = channel.readAvailable(tempBuffer, 0, tempBuffer.size)
            if (bytesRead > 0) {
                buffer.addAll(tempBuffer.take(bytesRead))
            }
        }
        
        buffer.toByteArray()
    }
    
    /**
     * 转义 HTML 特殊字符
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
