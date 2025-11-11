package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.*
import com.projectu.shared.domain.model.*

/**
 * DTO到Domain模型的映射器
 */
object ArtworkMapper {
    
    fun toDomain(dto: ArtworkDto): Artwork {
        val userDto = dto.user
        val imageUrlsDto = dto.imageUrls
        return Artwork(
            id = dto.id.toString(),
            title = dto.title,
            description = dto.description,
            type = when (dto.type.lowercase()) {
                "illust", "illustration" -> ArtworkType.ILLUSTRATION
                "manga" -> ArtworkType.MANGA
                "ugoira" -> ArtworkType.UGOIRA
                else -> ArtworkType.ILLUSTRATION
            },
            imageUrls = listOf(imageUrlsDto.original),
            width = dto.width,
            height = dto.height,
            pageCount = dto.pageCount,
            userId = userDto.id.toString(),
            userName = userDto.name,
            userProfileImageUrl = userDto.profileImageUrls.medium,
            tags = dto.tags.map { it.name },
            viewCount = dto.viewCount,
            likeCount = dto.likeCount,
            bookmarkCount = dto.bookmarkCount,
            commentCount = dto.commentCount,
            createdTime = dto.createDate,
            isBookmarked = dto.isBookmarked,
            isMuted = false,
            totalView = dto.viewCount,
            totalBookmarks = dto.bookmarkCount,
            ageLimit = when (dto.xRestrict) {
                0 -> AgeLimit.ALL_AGE
                1 -> AgeLimit.R18
                2 -> AgeLimit.R18G
                else -> AgeLimit.ALL_AGE
            }
        )
    }
    
    fun ImageUrlsDto.toDomain() = ImageUrls(
        squareMedium = squareMedium,
        medium = medium,
        large = large,
        original = original
    )
    
    fun UserDto.toDomain() = User(
        id = id.toString(),
        name = name,
        account = account,
        profileImageUrl = profileImageUrls.medium,
        isFollowed = isFollowed
    )
    
    fun TagDto.toDomain() = Tag(
        name = name,
        translatedName = translatedName
    )
    
    fun UgoiraMetadataDto.toDomain() = UgoiraMetadata(
        zipUrl = zipUrls.medium,
        frames = frames.map { it.toDomain() }
    )
    
    fun UgoiraFrameDto.toDomain() = UgoiraFrame(
        file = file,
        delay = delay
    )
}

