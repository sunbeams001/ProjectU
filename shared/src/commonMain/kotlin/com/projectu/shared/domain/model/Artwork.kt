package com.projectu.shared.domain.model

/**
 * 作品领域模型
 */
data class Artwork(
    val id: String,
    val title: String,
    val description: String,
    val type: ArtworkType = ArtworkType.ILLUSTRATION,
    val imageUrls: ArtworkImageUrls,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    // 用户信息
    val userId: String,
    val userName: String,
    val userProfileImageUrl: String,
    // 标签
    val tags: List<Tag>,
    // 统计信息
    val viewCount: Int,
    val likeCount: Int,
    val bookmarkCount: Int,
    val commentCount: Int,
    // 时间
    val createdTime: String,
    // 状态
    val bookmarkStatus: BookmarkStatus = BookmarkStatus.NOT_BOOKMARKED,
    val bookmarkId: String? = null,
    val isMuted: Boolean = false,
    val isAiGenerated: Boolean = false,
    // 扩展信息
    val totalView: Int,
    val totalBookmarks: Int,
    val ageLimit: AgeLimit = AgeLimit.ALL_AGE,
    // Ugoira特有字段
    val ugoiraMetadata: UgoiraMetadata? = null
)

/**
 * 作品图片 URL 结构
 * 
 * 统一使用 PageImageUrls 表示：
 * - 单页作品：pages 为 null 或只有一页（page=0），使用该页的 URL 和尺寸
 * - 多页作品：pages 包含所有页的完整 URL（需要调用 getPages 接口获取）
 * 
 * @property pages 所有页的图片信息（单页作品只有一页，多页作品有多页）
 */
data class ArtworkImageUrls(
    val pages: List<PageImageUrls>
) {
    /**
     * 获取显示用的最佳 URL
     * 
     * 优先级：
     * 1. 第一页的原图
     * 2. 第一页的最大尺寸缩略图
     */
    fun getBestQualityUrl(): String {
        val firstPage = pages.firstOrNull()?.urls ?: return ""
        return firstPage.original
            ?: firstPage.master1200
            ?: firstPage.large
            ?: firstPage.medium
            ?: firstPage.squareMedium
    }
    
    /**
     * 获取所有页的原图 URL 列表
     * 用于多页作品的浏览
     */
    fun getAllPageUrls(): List<String> {
        return pages.mapNotNull { it.urls.original ?: it.urls.master1200 }
    }
    
    /**
     * 是否有真正的原图（img-original）
     */
    fun hasOriginal(): Boolean {
        return pages.any { it.urls.original != null }
    }
    
    /**
     * 是否为多页作品
     */
    fun isMultiPage(): Boolean = pages.size > 1
}

/**
 * 图片 URL 集合（统一所有尺寸级别）
 * 
 * 尺寸级别映射（按质量从小到大）：
 * 
 * **API 字段 → Domain 字段映射**：
 * 
 * | API 接口 | API 字段 | 尺寸 | Domain 字段 |
 * |---------|---------|------|------------|
 * | 多页 pages | thumb_mini | 128x128 | mini |
 * | 详情/发现 | thumb | 250x250 custom-thumb | squareMedium |
 * | 发现 | 360x360 | 360x360 custom-thumb | medium |
 * | 详情/发现/多页 | small / 540x540 | 540x540 | large |
 * | 详情/发现/多页 | regular / 1200x1200 | master1200 | master1200 |
 * | 详情/多页 | original | 原始尺寸 | original |
 * 
 * @property mini 超小缩略图（128x128，仅多页接口）
 * @property squareMedium 方形中等缩略图（250x250 custom-thumb）
 * @property medium 中等缩略图（360x360 custom-thumb，仅发现接口）
 * @property large 大缩略图（540x540）
 * @property master1200 最大缩略图（img-master/master1200）
 * @property original 原图 URL（img-original，仅详情和多页接口）
 */
data class ImageUrls(
    val mini: String? = null,
    val squareMedium: String,
    val medium: String? = null,
    val large: String? = null,
    val master1200: String? = null,
    val original: String? = null
) {
    /**
     * 获取最佳质量的可用 URL
     */
    fun getBestAvailable(): String {
        return original ?: master1200 ?: large ?: medium ?: squareMedium
    }
}

/**
 * 多页作品的单页图片信息
 * 
 * 真实 API 响应示例（/ajax/illust/{pid}/pages）：
 * ```json
 * {
 *   "urls": {
 *     "thumb_mini": "https://i.pximg.net/c/128x128/img-master/...",
 *     "small": "https://i.pximg.net/c/540x540_70/img-master/...",
 *     "regular": "https://i.pximg.net/img-master/.../p0_master1200.jpg",
 *     "original": "https://i.pximg.net/img-original/.../p0.png"
 *   },
 *   "width": 1536,
 *   "height": 1024
 * }
 * ```
 * 
 * @property page 页码（从 0 开始）
 * @property urls 该页的多尺寸 URL
 * @property width 图片宽度（像素）
 * @property height 图片高度（像素）
 */
data class PageImageUrls(
    val page: Int,
    val urls: ImageUrls,
    val width: Int,
    val height: Int
)

/**
 * 用户信息
 * 基于 UserApi 相关 DTO 重新设计：UserInfoBody, RecommendUserDetail, DiscoveryUserInfo
 */
data class User(
    /** 用户ID */
    val id: String,
    /** 用户名 */
    val name: String,
    /** 用户账号 */
    val account: String? = null,
    /** 头像URL (小) */
    val profileImageUrl: String,
    /** 头像URL (大) */
    val profileImageUrlBig: String? = null,
    /** 个人简介 */
    val comment: String? = null,
    /** 关注状态 */
    val followStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    /** 是否为好P友 */
    val isMypixiv: Boolean = false,
    /** 是否已屏蔽 */
    val isBlocking: Boolean = false,
    /** 是否被对方关注 */
    val followedBack: Boolean = false,
    /** 是否为高级会员 */
    val isPremium: Boolean = false,
    /** 背景图URL */
    val backgroundUrl: String? = null,
    /** 是否接受约稿请求 */
    val acceptCommissionRequest: Boolean = false,
    /** 关注数量 */
    val followingCount: Int = 0,
    /** 个人网站 */
    val webpage: String? = null,
    /** 是否为官方账号 */
    val isOfficial: Boolean = false,
    /** 用户插画作品列表 */
    val illusts: List<Artwork> = emptyList(),
    /** 用户小说作品列表 */
    val novels: List<Novel> = emptyList()
)

/**
 * 标签（完整版本）
 */
data class Tag(
    val name: String,
    val translatedName: String? = null
)

enum class ArtworkType {
    ILLUSTRATION,  // 插画
    MANGA,         // 漫画
    UGOIRA;        // 动图
    
    companion object {
        /**
         * 从 illustType 整数值转换为 ArtworkType
         * @param illustType Pixiv API 返回的作品类型值 (0=插画, 1=漫画, 2=动图)
         */
        fun fromIllustType(illustType: Int): ArtworkType {
            return when (illustType) {
                0 -> ILLUSTRATION
                1 -> MANGA
                2 -> UGOIRA
                else -> ILLUSTRATION // 默认为插画
            }
        }
        
        /**
         * 从字符串类型转换为 ArtworkType
         * @param type 作品类型字符串，支持以下格式：
         *   - 完整名称: "illustration", "manga", "ugoira"
         *   - 数字字符串: "0" (插画), "1" (漫画), "2" (动图)
         */
        fun fromString(type: String): ArtworkType {
            // 先尝试按数字解析（排行榜 API 返回的是数字字符串）
            type.toIntOrNull()?.let { 
                return fromIllustType(it) 
            }
            // 按名称解析
            return when (type.lowercase()) {
                "illustration", "illust" -> ILLUSTRATION
                "manga" -> MANGA
                "ugoira" -> UGOIRA
                else -> ILLUSTRATION // 默认为插画
            }
        }
    }
}

enum class AgeLimit {
    ALL_AGE,
    R18,
    R18G
}

/**
 * Ugoira动图元数据
 */
data class UgoiraMetadata(
    val zipUrl: String,
    val frames: List<UgoiraFrame>
)

data class UgoiraFrame(
    val file: String,
    val delay: Int // 毫秒
)

