package com.projectu.shared.util

/**
 * 文本分割工具类
 * 
 * 用于将长文本按句子分割成指定大小的块，支持中日文标点符号。
 * 主要用于翻译服务和小说阅读等场景。
 */
object TextSplitter {
    
    /**
     * 按句子分割文本，并组合成不超过指定大小的文本块
     * 
     * @param text 要分割的原始文本
     * @param maxChunkSize 每个文本块的最大字符数
     * @return 分割后的文本块列表
     * 
     * 注意：正则表达式中包含中文标点符号（。！？），用于正确识别中日文句子边界
     */
    @Suppress("HardcodedChinese")
    fun splitIntoChunks(text: String, maxChunkSize: Int): List<String> {
        if (text.isBlank()) return emptyList()
        if (text.length <= maxChunkSize) return listOf(text)
        
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        
        // 按句子分割（按句号、问号、感叹号、换行符等分割）
        // 支持中文标点（。！？）和英文标点（. ! ?）
        val sentences = text.split(Regex("(?<=[。！？\\.\\!\\?\\n])"))
        
        for (sentence in sentences) {
            if (sentence.isBlank()) continue
            
            // 如果单个句子就超过限制，需要强制分割
            if (sentence.length > maxChunkSize) {
                // 先保存当前块
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk.clear()
                }
                
                // 将超长句子按固定长度分割
                var start = 0
                while (start < sentence.length) {
                    val end = minOf(start + maxChunkSize, sentence.length)
                    chunks.add(sentence.substring(start, end))
                    start = end
                }
                continue
            }
            
            // 如果加上当前句子会超过限制，先保存当前块
            if (currentChunk.length + sentence.length > maxChunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk.clear()
                }
            }
            
            // 添加当前句子到块中
            currentChunk.append(sentence)
        }
        
        // 保存最后一个块
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        return chunks
    }
}
