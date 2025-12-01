package com.projectu.shared.domain.model

/**
 * 小说系列领域模型
 */
data class NovelSeries(
    val id: String,
    val title: String,
    val caption: String,
    // 用户信息
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    // 封面图片
    val coverUrl: String?,
    // 系列状态
    val isOriginal: Boolean = false,
    val isConcluded: Boolean = false,
    val isWatched: Boolean = false,
    val isNotifying: Boolean = false,
    // 统计信息
    val contentCount: Int, // 作品数量（系列中的篇数）
    val totalCharacterCount: Int?, // 总字符数
    val totalWordCount: Int?, // 总字数
    val readingTimeSeconds: Int?, // 预计阅读时间（秒）
    val watchCount: Int?, // 追更人数
    // 标签
    val tags: List<String> = emptyList(),
    // 分类
    val genreId: String? = null,
    val language: String? = null,
    // 年龄限制
    val xRestrict: Int = 0, // 0=全年龄, 1=R-18, 2=R-18G
    val maxXRestrict: Int? = null,
    val aiType: Int = 0,
    // 时间信息
    val createDate: String,
    val updateDate: String,
    val createdTimestamp: Long? = null,
    val updatedTimestamp: Long? = null,
    val lastPublishedContentTimestamp: Long? = null,
    // 第一篇和最新一篇的ID
    val firstNovelId: String? = null,
    val latestNovelId: String? = null,
    // 其他
    val hasGlossary: Boolean = false
) {
    /**
     * 判断系列是否为 R-18
     * 检查 xRestrict/maxXRestrict 标识或 R-18 相关标签
     */
    val isR18: Boolean
        get() = xRestrict == 1 || maxXRestrict == 1 || tags.contains("R-18")
    
    /**
     * 判断系列是否为 R-18G
     * 检查 xRestrict/maxXRestrict 标识或 R-18G 相关标签
     */
    val isR18G: Boolean
        get() = xRestrict == 2 || maxXRestrict == 2 || tags.contains("R-18G")
    
    /**
     * 获取年龄限制等级
     * 统一的年龄限制判断，考虑 xRestrict、maxXRestrict 和标签
     */
    val ageLimit: AgeLimit
        get() = when {
            isR18G -> AgeLimit.R18G
            isR18 -> AgeLimit.R18
            else -> AgeLimit.ALL_AGE
        }
    
    /**
     * 判断是否为 AI 生成
     * 检查 aiType 标识或 AI 相关标签
     */
    val isAiGenerated: Boolean
        get() = aiType == 2 || tags.contains("AI小説")
    
    /**
     * 预计阅读时间（分钟）- 用于显示
     */
    val readingTimeMinutes: Int?
        get() = readingTimeSeconds?.let { (it + 59) / 60 } // 向上取整
}
