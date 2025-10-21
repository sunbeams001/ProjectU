package com.projectu.shared.domain.model

import kotlinx.datetime.Instant

/**
 * 作品领域模型
 */
data class Artwork(
    val id: String,
    val title: String,
    val description: String,
    val imageUrls: ImageUrls,
    val user: User,
    val tags: List<Tag>,
    val createDate: Instant,
    val pageCount: Int,
    val width: Int,
    val height: Int,
    val viewCount: Int,
    val bookmarkCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val isBookmarked: Boolean = false,
    val isLiked: Boolean = false,
    val type: ArtworkType = ArtworkType.ILLUST,
    val ageLimit: AgeLimit = AgeLimit.ALL_AGE,
    // Ugoira特有字段
    val ugoiraMetadata: UgoiraMetadata? = null
)

data class ImageUrls(
    val squareMedium: String,
    val medium: String,
    val large: String,
    val original: String
)

data class User(
    val id: String,
    val name: String,
    val account: String,
    val profileImageUrl: String,
    val isFollowed: Boolean = false
)

data class Tag(
    val name: String,
    val translatedName: String? = null
)

enum class ArtworkType {
    ILLUST,     // 插画
    MANGA,      // 漫画
    UGOIRA      // 动图
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

