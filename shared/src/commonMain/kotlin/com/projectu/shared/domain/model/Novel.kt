package com.projectu.shared.domain.model

/**
 * 内嵌图片信息
 */
data class NovelEmbeddedImageInfo(
    val imageId: String,
    val smallUrl: String?,  // 240px 宽度
    val mediumUrl: String?, // 480px 宽度
    val largeUrl: String?,  // 1200px
    val originalUrl: String?
)

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
    val isLiked: Boolean = false, // 是否已点赞
    // 阅读信息
    val textCount: Int, // 文本字数
    val wordCount: Int, // 单词数
    val readingTime: Int, // 预计阅读时间（秒）
    val useWordCount: Boolean = false,
    // 分类和类型
    val genre: NovelGenre,
    val language: String = "ja", // 语言代码
    // 年龄限制
    val ageLimit: AgeLimit = AgeLimit.ALL_AGE,
    // 系列信息
    val seriesId: String? = null,
    val seriesTitle: String? = null,
    val seriesOrder: Int? = null, // 在系列中的顺序（仅在系列详情中使用）
    // 可见性
    val viewableType: Int = 0, // 0=公开, 2=好P友限定
    // 其他
    val isUnlisted: Boolean = false, // 是否为非公开作品
    val pageCount: Int = 1, // 页数（用于分章节小说）
    val marker: Int? = null, // 当前阅读标记位置
    // 内嵌图片（仅在详情接口返回）
    val embeddedImages: Map<String, NovelEmbeddedImageInfo> = emptyMap()
) {
    /**
     * 判断该小说是否可查看
     * viewableType: 0=公开, 2=好P友限定
     */
    val isViewable: Boolean
        get() = viewableType == 0
    
    /**
     * 判断是否为好P友限定作品
     */
    val isFriendsOnly: Boolean
        get() = viewableType == 2
}

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

