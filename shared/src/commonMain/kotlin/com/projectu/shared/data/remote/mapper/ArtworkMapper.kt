package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.*
import com.projectu.shared.domain.model.*
import kotlinx.datetime.Instant

/**
 * DTO到Domain模型的映射器
 */
object ArtworkMapper {
    
    fun toDomain(dto: ArtworkDto): Artwork {
        return Artwork(
            id = dto.id,
            title = dto.title,
            description = dto.description,
            imageUrls = dto.imageUrls.toDomain(),
            user = dto.user.toDomain(),
            tags = dto.tags.map { it.toDomain() },
            createDate = Instant.parse(dto.createDate),
            pageCount = dto.pageCount,
            width = dto.width,
            height = dto.height,
            viewCount = dto.viewCount,
            bookmarkCount = dto.bookmarkCount,
            likeCount = dto.likeCount,
            commentCount = dto.commentCount,
            isBookmarked = dto.isBookmarked,
            type = when (dto.type.lowercase()) {
                "illust" -> ArtworkType.ILLUST
                "manga" -> ArtworkType.MANGA
                "ugoira" -> ArtworkType.UGOIRA
                else -> ArtworkType.ILLUST
            },
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
        id = id,
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

