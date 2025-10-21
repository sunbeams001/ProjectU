package com.projectu.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pixiv API作品响应DTO
 */
@Serializable
data class ArtworkDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("description")
    val description: String = "",
    
    @SerialName("imageUrls")
    val imageUrls: ImageUrlsDto,
    
    @SerialName("user")
    val user: UserDto,
    
    @SerialName("tags")
    val tags: List<TagDto> = emptyList(),
    
    @SerialName("createDate")
    val createDate: String,
    
    @SerialName("pageCount")
    val pageCount: Int = 1,
    
    @SerialName("width")
    val width: Int,
    
    @SerialName("height")
    val height: Int,
    
    @SerialName("viewCount")
    val viewCount: Int = 0,
    
    @SerialName("bookmarkCount")
    val bookmarkCount: Int = 0,
    
    @SerialName("likeCount")
    val likeCount: Int = 0,
    
    @SerialName("commentCount")
    val commentCount: Int = 0,
    
    @SerialName("isBookmarked")
    val isBookmarked: Boolean = false,
    
    @SerialName("type")
    val type: String = "illust",
    
    @SerialName("xRestrict")
    val xRestrict: Int = 0
)

@Serializable
data class ImageUrlsDto(
    @SerialName("squareMedium")
    val squareMedium: String,
    
    @SerialName("medium")
    val medium: String,
    
    @SerialName("large")
    val large: String,
    
    @SerialName("original")
    val original: String
)

@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("account")
    val account: String,
    
    @SerialName("profileImageUrls")
    val profileImageUrls: ProfileImageUrlsDto,
    
    @SerialName("isFollowed")
    val isFollowed: Boolean = false
)

@Serializable
data class ProfileImageUrlsDto(
    @SerialName("medium")
    val medium: String
)

@Serializable
data class TagDto(
    @SerialName("name")
    val name: String,
    
    @SerialName("translatedName")
    val translatedName: String? = null
)

@Serializable
data class UgoiraMetadataDto(
    @SerialName("zipUrls")
    val zipUrls: ZipUrlsDto,
    
    @SerialName("frames")
    val frames: List<UgoiraFrameDto>
)

@Serializable
data class ZipUrlsDto(
    @SerialName("medium")
    val medium: String
)

@Serializable
data class UgoiraFrameDto(
    @SerialName("file")
    val file: String,
    
    @SerialName("delay")
    val delay: Int
)

