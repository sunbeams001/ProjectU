package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.user.DiscoveryUsersBody
import com.projectu.shared.data.remote.dto.user.DiscoveryUserInfo
import com.projectu.shared.data.remote.dto.user.IllustThumbnail
import com.projectu.shared.data.remote.dto.user.RecommendUserDetail
import com.projectu.shared.data.remote.dto.user.TagTranslation
import com.projectu.shared.data.remote.dto.user.UserInfoBody
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkImageUrls
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.ImageUrls
import com.projectu.shared.domain.model.PageImageUrls
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.domain.model.User
import com.projectu.shared.util.AgeLimitDeterminer

/**
 * 用户 DTO 到 Domain 模型的映射器
 */

/**
 * 将 IllustThumbnail 转换为 Artwork
 * 
 * @param tagTranslation 标签翻译字典
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun IllustThumbnail.toArtwork(
    tagTranslation: Map<String, TagTranslation>,
    ageLimitDeterminer: AgeLimitDeterminer
): Artwork {
    // 根据 illustType 判断作品类型
    val artworkType = ArtworkType.fromIllustType(illustType)
    
    // 转换标签并填充翻译
    val translatedTags = tags.map { tagName ->
        Tag(
            name = tagName,
            translatedName = tagTranslation[tagName]?.zh // 使用中文翻译
        )
    }
    
    // 构建图片URL结构
    val imageUrls = ArtworkImageUrls(
        pages = listOf(
            PageImageUrls(
                page = 0,
                urls = ImageUrls(
                    mini = null,                          // 缩略图接口不提供 mini
                    squareMedium = urls.size250x250,      // 250x250 custom-thumb
                    medium = urls.size360x360,            // 360x360 custom-thumb
                    large = urls.size540x540,             // 540x540
                    master1200 = urls.size1200x1200,      // img-master/master1200
                    original = null                       // 缩略图不包含原图
                ),
                width = width,
                height = height
            )
        )
    )
    
    return Artwork(
        id = id,
        title = title,
        description = description,
        type = artworkType,
        imageUrls = imageUrls,
        width = width,
        height = height,
        pageCount = pageCount,
        userId = userId,
        userName = userName,
        userProfileImageUrl = profileImageUrl,
        tags = translatedTags,
        viewCount = 0, // IllustThumbnail 不包含浏览数
        likeCount = 0, // IllustThumbnail 不包含点赞数
        bookmarkCount = 0, // IllustThumbnail 不包含收藏数
        commentCount = 0, // IllustThumbnail 不包含评论数
        createdTime = createDate,
        bookmarkStatus = if (bookmarkData != null) BookmarkStatus.PUBLIC else BookmarkStatus.NOT_BOOKMARKED,
        bookmarkId = bookmarkData,
        isMuted = isMasked,
        isAiGenerated = isAiGeneratedArtwork(aiType, tags),
        totalView = 0,
        totalBookmarks = 0,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = xRestrict,
            sl = sl,
            tags = tags
        ),
        ugoiraMetadata = null
    )
}

/**
 * 将 UserInfoBody 转换为 User 实体
 */
fun UserInfoBody.toUser(): User {
    return User(
        id = userId,
        name = name,
        account = userId,
        profileImageUrl = image,
        profileImageUrlBig = imageBig,
        comment = comment,
        followStatus = if (isFollowed) FollowStatus.PUBLIC else FollowStatus.NOT_FOLLOWING,
        isMypixiv = isMypixiv,
        isBlocking = isBlocking,
        followedBack = followedBack,
        isPremium = premium,
        backgroundUrl = background?.url,
        acceptCommissionRequest = acceptRequest,
        followingCount = following,
        webpage = webpage,
        isOfficial = official,
        illusts = emptyList(),
        novels = emptyList()
    )
}

/**
 * 将 DiscoveryUserInfo 转换为 User 实体
 * 需要配合 DiscoveryUsersBody 使用以填充作品列表
 */
fun DiscoveryUserInfo.toUser(): User {
    return User(
        id = userId,
        name = name,
        account = null, // DiscoveryUserInfo 不包含 account 字段
        profileImageUrl = image,
        profileImageUrlBig = imageBig,
        comment = comment,
        followStatus = if (isFollowed) FollowStatus.PUBLIC else FollowStatus.NOT_FOLLOWING,
        isMypixiv = isMypixiv,
        isBlocking = isBlocking,
        followedBack = followedBack,
        isPremium = premium,
        backgroundUrl = background,
        acceptCommissionRequest = commission?.acceptRequest ?: false,
        followingCount = 0, // DiscoveryUserInfo 不包含关注数量
        webpage = null, // DiscoveryUserInfo 不包含网站信息
        isOfficial = false, // DiscoveryUserInfo 不包含官方标记
        illusts = emptyList(), // 需要通过 toUserWithArtworks 填充
        novels = emptyList()
    )
}

/**
 * 将 DiscoveryUsersBody 转换为 User 列表，并填充作品和标签翻译
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 * @return 包含完整作品信息的用户列表
 */
fun DiscoveryUsersBody.toUsersWithArtworks(ageLimitDeterminer: AgeLimitDeterminer): List<User> {
    // 构建用户ID到插画ID列表的映射
    val userIllustMap = recommendedUsers.associate { entry ->
        entry.userId to entry.recentIllustIds
    }
    
    // 构建插画ID到插画对象的映射（带标签翻译）
    val illustMap = thumbnails.illust.associateBy(
        keySelector = { it.id },
        valueTransform = { it.toArtwork(tagTranslation, ageLimitDeterminer) }
    )
    
    // 转换用户并填充作品
    return users.map { userInfo ->
        val userIllustIds = userIllustMap[userInfo.userId] ?: emptyList()
        val userArtworks = userIllustIds.mapNotNull { illustId ->
            illustMap[illustId]
        }
        
        userInfo.toUser().copy(illusts = userArtworks)
    }
}

/**
 * 将 RecommendUserDetail 转换为 User 实体
 */
fun RecommendUserDetail.toUser(): User {
    return User(
        id = userId,
        name = name,
        account = null, // RecommendUserDetail 不包含 account 字段
        profileImageUrl = image,
        profileImageUrlBig = imageBig,
        comment = comment,
        followStatus = if (isFollowed) FollowStatus.PUBLIC else FollowStatus.NOT_FOLLOWING,
        isMypixiv = isMypixiv,
        isBlocking = isBlocking,
        followedBack = followedBack,
        isPremium = premium,
        backgroundUrl = background,
        acceptCommissionRequest = commission?.requestStatus != null,
        followingCount = 0, // RecommendUserDetail 不包含关注数量
        webpage = null, // RecommendUserDetail 不包含网站信息
        isOfficial = false, // RecommendUserDetail 不包含官方标记
        illusts = emptyList(),
        novels = emptyList()
    )
}
