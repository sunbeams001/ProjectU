package com.projectu.shared.domain.model

/**
 * 下载规则（领域模型）
 */
data class DownloadRule(
    val id: Long = 0,
    val order: Int,
    val resourceTypeFilter: ResourceTypeFilter,
    val r18Filter: FilterType,
    val aiFilter: FilterType,
    val authorGrouping: AuthorGrouping,
    val targetPath: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subDirectory: String = "" // 资源类型子目录（仅内置规则使用）
) {
    /**
     * 判断给定的下载任务是否匹配此规则
     * 
     * @param task 下载任务
     * @return true 表示匹配，false 表示不匹配
     */
    fun matches(task: DownloadTask): Boolean {
        // 1. 资源类型匹配
        if (resourceTypeFilter != ResourceTypeFilter.ANY) {
            val expectedType = when (resourceTypeFilter) {
                ResourceTypeFilter.ILLUSTRATION -> ResourceType.ILLUSTRATION
                ResourceTypeFilter.MANGA -> ResourceType.MANGA
                ResourceTypeFilter.UGOIRA -> ResourceType.UGOIRA
                ResourceTypeFilter.NOVEL -> ResourceType.NOVEL
                ResourceTypeFilter.NOVEL_SERIES -> ResourceType.NOVEL_SERIES
                ResourceTypeFilter.ANY -> null // 不会进入这个分支
            }
            if (task.resourceType != expectedType) return false
        }
        
        // 2. R-18 匹配
        when (r18Filter) {
            FilterType.MUST_BE -> if (!task.isR18) return false
            FilterType.MUST_NOT_BE -> if (task.isR18) return false
            FilterType.ANY -> { /* 不过滤 */ }
        }
        
        // 3. AI 匹配
        when (aiFilter) {
            FilterType.MUST_BE -> if (!task.isAi) return false
            FilterType.MUST_NOT_BE -> if (task.isAi) return false
            FilterType.ANY -> { /* 不过滤 */ }
        }
        
        return true
    }
    
    /**
     * 根据规则和任务信息构建相对路径
     * 
     * @param task 下载任务
     * @return 相对路径，例如："Illustrations/123456" 或 "Manga"，如果不分组且无子目录则返回空字符串
     * 
     * 路径构成：subDirectory + authorGrouping
     */
    fun buildRelativePath(task: DownloadTask): String {
        // 根据作者分组模式构建作者路径
        val authorPath = when (authorGrouping) {
            AuthorGrouping.BY_ID -> task.authorId
            AuthorGrouping.BY_NAME -> sanitizeFileName(task.authorName)
            AuthorGrouping.NONE -> ""
        }
        
        // 组合子目录和作者路径
        return when {
            subDirectory.isNotEmpty() && authorPath.isNotEmpty() -> "$subDirectory/$authorPath"
            subDirectory.isNotEmpty() -> subDirectory
            authorPath.isNotEmpty() -> authorPath
            else -> ""
        }
    }
    
    /**
     * 文件名合法化处理
     */
    private fun sanitizeFileName(name: String): String {
        val illegalChars = Regex("""[\\/:*?"<>|\u0000-\u001F\u007F]""")
        return name
            .replace(illegalChars, "_")
            .trim()
            .take(200)
            .ifEmpty { "Unknown" }
    }
}
