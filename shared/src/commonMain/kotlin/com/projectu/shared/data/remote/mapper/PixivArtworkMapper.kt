package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.pixiv.IllustDetailBody
import com.projectu.shared.data.remote.dto.pixiv.IllustSimple
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkType

/**
 * 将 IllustDetailBody 转换为 Artwork
 */
fun IllustDetailBody.toArtwork(): Artwork {
    return Artwork(
        id = this.id,
        title = this.title,
        description = this.description,
        type = when (this.illustType) {
            0 -> ArtworkType.ILLUSTRATION
            1 -> ArtworkType.MANGA
            2 -> ArtworkType.UGOIRA
            else -> ArtworkType.ILLUSTRATION
        },
        imageUrls = listOf(this.urls.original),
        width = this.width,
        height = this.height,
        pageCount = this.pageCount,
        userId = this.userId,
        userName = this.userName,
        userProfileImageUrl = "", // 详情接口不返回用户头像
        tags = this.tags.tags.map { it.tag },
        viewCount = this.viewCount,
        likeCount = this.likeCount,
        bookmarkCount = this.bookmarkCount,
        commentCount = this.commentCount,
        createdTime = this.createDate,
        isBookmarked = this.bookmarkData != null,
        isMuted = false,
        totalView = this.viewCount,
        totalBookmarks = this.bookmarkCount,
        ageLimit = when (this.xRestrict) {
            0 -> AgeLimit.ALL_AGE
            1 -> AgeLimit.R18
            2 -> AgeLimit.R18G
            else -> AgeLimit.ALL_AGE
        },
        ugoiraMetadata = null // 需要单独查询
    )
}

/**
 * 将 IllustSimple 转换为 Artwork
 */
fun IllustSimple.toArtwork(): Artwork {
    return Artwork(
        id = this.id,
        title = this.title,
        description = this.description,
        type = when (this.illustType) {
            0 -> ArtworkType.ILLUSTRATION
            1 -> ArtworkType.MANGA
            2 -> ArtworkType.UGOIRA
            else -> ArtworkType.ILLUSTRATION
        },
        imageUrls = listOf(this.url),
        width = this.width,
        height = this.height,
        pageCount = this.pageCount,
        userId = this.userId,
        userName = this.userName,
        userProfileImageUrl = this.profileImageUrl ?: "",
        tags = this.tags,
        viewCount = 0, // 简化版本不包含
        likeCount = 0,
        bookmarkCount = 0,
        commentCount = 0,
        createdTime = this.createDate,
        isBookmarked = this.bookmarkData != null,
        isMuted = this.isMasked,
        totalView = 0,
        totalBookmarks = 0,
        ageLimit = when (this.xRestrict) {
            0 -> AgeLimit.ALL_AGE
            1 -> AgeLimit.R18
            2 -> AgeLimit.R18G
            else -> AgeLimit.ALL_AGE
        },
        ugoiraMetadata = null
    )
}

/**
 * 将 IllustSimple 列表转换为 Artwork 列表
 */
fun List<IllustSimple>.toArtworkList(): List<Artwork> {
    return this.map { it.toArtwork() }
}

