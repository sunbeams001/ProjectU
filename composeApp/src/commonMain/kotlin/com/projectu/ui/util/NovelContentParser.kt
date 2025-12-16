package com.projectu.ui.util

/**
 * 小说内容解析器
 * 
 * 用于解析小说内容中的各种标签：
 * - [newpage] - 分页标记
 * - [chapter:《标题》] - 章节标题
 * - [pixivimage:id] - Pixiv插画引用
 * - [uploadedimage:id] - 上传的内嵌图片（从textEmbeddedImages获取URL）
 * - [[jumpuri:显示文本>URL]] - 跳转链接
 * - [[rb:文本>假名]] - Ruby注音
 */
object NovelContentParser {
    
    /**
     * 解析后的小说页面
     */
    data class NovelPage(
        val pageNumber: Int,  // 页码（从1开始）
        val content: String,  // 页面纯文本内容
        val elements: List<ContentElement>  // 页面内容元素列表
    )
    
    /**
     * 内容元素密封类
     */
    sealed class ContentElement {
        /**
         * 普通文本段落
         */
        data class Text(val content: String) : ContentElement()
        
        /**
         * 章节标题
         * 格式: [chapter:《标题》]
         */
        data class ChapterTitle(val title: String) : ContentElement()
        
        /**
         * Pixiv插画引用
         * 格式: [pixivimage:id] 或 [pixivimage:id-page]
         */
        data class PixivImage(
            val illustId: String,
            val pageIndex: Int? = null
        ) : ContentElement()
        
        /**
         * 上传的内嵌图片
         * 格式: [uploadedimage:id]
         * 图片URL从textEmbeddedImages字段获取
         */
        data class UploadedImage(
            val imageId: String
        ) : ContentElement()
        
        /**
         * 跳转链接
         * 格式: [[jumpuri:显示文本>URL]]
         */
        data class JumpLink(
            val displayText: String,
            val url: String
        ) : ContentElement()
        
        /**
         * Ruby注音
         * 格式: [[rb:文本>假名]]
         */
        data class Ruby(
            val text: String,
            val reading: String
        ) : ContentElement()
        
        /**
         * 空行（段落分隔）
         */
        data object EmptyLine : ContentElement()
    }
    
    // 正则表达式模式
    private val NEW_PAGE_PATTERN = Regex("""\[newpage\]""", RegexOption.IGNORE_CASE)
    private val CHAPTER_PATTERN = Regex("""\[chapter:([^\]]+)\]""")
    private val PIXIV_IMAGE_PATTERN = Regex("""\[pixivimage:(\d+)(?:-(\d+))?\]""")
    private val UPLOADED_IMAGE_PATTERN = Regex("""\[uploadedimage:(\d+)\]""")
    // 支持两种 jumpuri 格式：
    // [[jumpuri:显示文本>URL]] 和 [[jumpuri:显示文本 > URL]]
    private val JUMP_URI_PATTERN = Regex("""\[\[jumpuri:([^>\]]+?)\s*>\s*([^\]]+)\]\]""")
    private val RUBY_PATTERN = Regex("""\[\[rb:([^>]+)>([^\]]+)\]\]""")
    
    /**
     * 解析小说内容，分割成多个页面
     * 
     * @param content 原始小说内容
     * @return 页面列表
     */
    fun parsePages(content: String): List<NovelPage> {
        if (content.isBlank()) {
            return listOf(NovelPage(1, "", emptyList()))
        }
        
        // 按 [newpage] 分割页面
        val pageTexts = NEW_PAGE_PATTERN.split(content)
        
        return pageTexts.mapIndexed { index, pageText ->
            val trimmedText = pageText.trim()
            NovelPage(
                pageNumber = index + 1,
                content = getPlainText(trimmedText),
                elements = parseElements(trimmedText)
            )
        }
    }
    
    /**
     * 解析页面内容为元素列表
     */
    private fun parseElements(pageContent: String): List<ContentElement> {
        if (pageContent.isBlank()) {
            return emptyList()
        }
        
        val elements = mutableListOf<ContentElement>()
        var remainingText = pageContent
        
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
                addTextElements(remainingText, elements)
                break
            }
            
            // 添加标签前的文本
            if (nearestMatch.range.first > 0) {
                val textBefore = remainingText.substring(0, nearestMatch.range.first)
                addTextElements(textBefore, elements)
            }
            
            // 处理匹配的标签
            when (nearestMatch) {
                chapterMatch -> {
                    val title = nearestMatch.groupValues[1]
                        .removeSurrounding("《", "》")
                        .removeSurrounding("「", "」")
                        .trim()
                    elements.add(ContentElement.ChapterTitle(title))
                }
                pixivImageMatch -> {
                    val illustId = nearestMatch.groupValues[1]
                    val pageIndex = nearestMatch.groupValues.getOrNull(2)?.toIntOrNull()
                    elements.add(ContentElement.PixivImage(illustId, pageIndex))
                }
                uploadedImageMatch -> {
                    val imageId = nearestMatch.groupValues[1]
                    elements.add(ContentElement.UploadedImage(imageId))
                }
                jumpMatch -> {
                    val displayText = nearestMatch.groupValues[1].trim()
                    val url = nearestMatch.groupValues[2].trim()
                    elements.add(ContentElement.JumpLink(displayText, url))
                }
                rubyMatch -> {
                    val text = nearestMatch.groupValues[1]
                    val reading = nearestMatch.groupValues[2]
                    elements.add(ContentElement.Ruby(text, reading))
                }
            }
            
            // 继续处理剩余文本
            remainingText = remainingText.substring(nearestMatch.range.last + 1)
        }
        
        return elements
    }
    
    /**
     * 将文本分割为段落元素
     * 
     * 策略：
     * 1. 首先按双换行（\n\n+）分割段落
     * 2. 对于超长段落（>5000字），按单换行进一步拆分以优化渲染性能
     */
    private fun addTextElements(text: String, elements: MutableList<ContentElement>) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return
        }
        
        // 按双换行分割段落
        val paragraphs = trimmedText.split(Regex("\n\n+"))
        
        paragraphs.forEachIndexed { index, paragraph ->
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isNotEmpty()) {
                // 如果段落过长（>5000字），按单换行进一步拆分
                if (trimmedParagraph.length > 5000) {
                    val lines = trimmedParagraph.split("\n")
                    var currentChunk = StringBuilder()
                    
                    lines.forEach { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty()) {
                            // 如果当前块加上新行后超过3000字，先保存当前块
                            if (currentChunk.length > 0 && currentChunk.length + trimmedLine.length > 3000) {
                                elements.add(ContentElement.Text(currentChunk.toString().trim()))
                                currentChunk = StringBuilder()
                            }
                            
                            // 添加当前行到块中
                            if (currentChunk.length > 0) {
                                currentChunk.append("\n")
                            }
                            currentChunk.append(trimmedLine)
                        } else if (currentChunk.length > 0) {
                            // 保留空行（在段落内部）
                            currentChunk.append("\n")
                        }
                    }
                    
                    // 添加最后一块
                    if (currentChunk.isNotEmpty()) {
                        elements.add(ContentElement.Text(currentChunk.toString().trim()))
                    }
                } else {
                    // 段落长度适中，直接添加
                    elements.add(ContentElement.Text(trimmedParagraph))
                }
            }
            
            // 添加段落间的空行（除了最后一个）
            if (index < paragraphs.size - 1) {
                elements.add(ContentElement.EmptyLine)
            }
        }
    }
    
    /**
     * 获取纯文本（移除所有标签）
     */
    private fun getPlainText(content: String): String {
        var text = content
        
        // 移除所有标签，保留章节标题的文本内容
        text = CHAPTER_PATTERN.replace(text) { match ->
            match.groupValues[1]
                .removeSurrounding("《", "》")
                .removeSurrounding("「", "」")
        }
        
        // 移除图片标签
        text = PIXIV_IMAGE_PATTERN.replace(text, "")
        text = UPLOADED_IMAGE_PATTERN.replace(text, "")
        
        // 处理跳转链接，保留显示文本
        text = JUMP_URI_PATTERN.replace(text) { match ->
            match.groupValues[1]
        }
        
        // 处理Ruby注音，只保留主文本
        text = RUBY_PATTERN.replace(text) { match ->
            match.groupValues[1]
        }
        
        return text.trim()
    }
    
    /**
     * 获取页面数量（基于 [newpage] 标签）
     */
    fun getPageCount(content: String): Int {
        if (content.isBlank()) return 1
        return NEW_PAGE_PATTERN.split(content).size
    }
    
    /**
     * 估算阅读时间（分钟）
     * 按每分钟阅读500字计算
     */
    fun estimateReadingTime(content: String): Int {
        val plainText = getPlainText(content)
        val charCount = plainText.length
        return maxOf(1, (charCount + 499) / 500)
    }
}
