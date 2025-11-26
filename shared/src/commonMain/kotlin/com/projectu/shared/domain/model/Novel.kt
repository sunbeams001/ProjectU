package com.projectu.shared.domain.model

/**
 * 小说领域模型
 */
data class Novel(
    val id: String,
    val title: String,
    val description: String,
    val content: String? = null, // 小说正文内容（仅在详情接口返回）
    val imageUrl: String = "", // 小说封面URL
    // 用户信息
    val userId: String,
    val userName: String,
    val userProfileImageUrl: String,
    // 标签
    val tags: List<Tag>,
    // 统计信息
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val bookmarkCount: Int,
    val commentCount: Int = 0,
    val markerCount: Int = 0, // 阅读标记数量
    // 时间
    val createdTime: String,
    val updatedTime: String? = null,
    // 状态
    val bookmarkStatus: BookmarkStatus = BookmarkStatus.NOT_BOOKMARKED,
    val bookmarkId: String? = null,
    val isMasked: Boolean = false,
    val isAiGenerated: Boolean = false,
    val isOriginal: Boolean = false,
    val isBungei: Boolean = false, // 是否为文艺小说
    // 阅读信息
    val textCount: Int, // 文本字数
    val wordCount: Int, // 单词数
    val readingTime: Int, // 预计阅读时间（分钟）
    val useWordCount: Boolean = false,
    // 分类和类型
    val genre: NovelGenre,
    val language: String = "ja", // 语言代码
    // 年龄限制
    val ageLimit: AgeLimit = AgeLimit.ALL_AGE,
    // 系列信息
    val seriesId: String? = null,
    val seriesTitle: String? = null,
    // 其他
    val isUnlisted: Boolean = false, // 是否为非公开作品
    val pageCount: Int = 1, // 页数（用于分章节小说）
    val marker: Int? = null // 当前阅读标记位置
)

/**
 * 小说类型枚举
 */
enum class NovelGenre {
    /** 其他 */
    OTHER,
    /** 爱情 */
    LOVE,
    /** 幻想 */
    FANTASY,
    /** 文学 */
    LITERATURE,
    /** 散文 */
    PROSE,
    /** BL */
    BL,
    /** GL */
    GL;
    
    companion object {
        /**
         * 从字符串类型转换为 NovelGenre
         * @param genre 小说类型字符串
         */
        fun fromString(genre: String): NovelGenre {
            return when (genre.lowercase()) {
                "love" -> LOVE
                "fantasy" -> FANTASY
                "literature" -> LITERATURE
                "prose" -> PROSE
                "bl" -> BL
                "gl" -> GL
                "other" -> OTHER
                else -> OTHER // 默认为其他
            }
        }
    }
}

