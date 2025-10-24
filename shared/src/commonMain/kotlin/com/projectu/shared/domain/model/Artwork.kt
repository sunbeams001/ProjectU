package com.projectu.shared.domain.model

/**
 * 作品领域模型
 */
data class Artwork(
    val id: String,
    val title: String,
    val description: String,
    val type: ArtworkType = ArtworkType.ILLUSTRATION,
    val imageUrls: List<String>,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    // 用户信息
    val userId: String,
    val userName: String,
    val userProfileImageUrl: String,
    // 标签
    val tags: List<String>,
    // 统计信息
    val viewCount: Int,
    val likeCount: Int,
    val bookmarkCount: Int,
    val commentCount: Int,
    // 时间
    val createdTime: String,
    // 状态
    val isBookmarked: Boolean = false,
    val isMuted: Boolean = false,
    // 扩展信息
    val totalView: Int,
    val totalBookmarks: Int,
    val ageLimit: AgeLimit = AgeLimit.ALL_AGE,
    // Ugoira特有字段
    val ugoiraMetadata: UgoiraMetadata? = null
)

/**
 * 图片URL集合（用于标准API）
 */
data class ImageUrls(
    val squareMedium: String,
    val medium: String,
    val large: String,
    val original: String
)

/**
 * 用户信息（完整版本）
 */
data class User(
    val id: String,
    val name: String,
    val account: String,
    val profileImageUrl: String,
    val isFollowed: Boolean = false,
    val isMuted: Boolean = false,
    val illusts: List<String> = emptyList(),
    val novels: List<String> = emptyList()
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
    UGOIRA         // 动图
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

