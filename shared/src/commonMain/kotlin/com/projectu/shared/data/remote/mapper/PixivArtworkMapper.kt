package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.illust.IllustDetailBody
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.dto.illust.UgoiraMetaBody
import com.projectu.shared.data.remote.dto.ranking.RankingContent
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.domain.model.UgoiraFrame
import com.projectu.shared.domain.model.UgoiraMetadata
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil

/**
 * 判断作品是否为 AI 生成
 * 
 * @param aiType AI 类型标识
 * @param tags 原始标签列表
 * @return true 表示是 AI 作品
 */
internal fun isAiGeneratedArtwork(aiType: Int, tags: List<String>): Boolean {
    return aiType == 2 || tags.contains("AIイラスト")
}

/**
 * 将 IllustDetailBody 转换为 Artwork
 * 
 * 注意：PixivTag 的 translation 字段中，key 固定为 "en"，
 * 但实际内容是根据请求的 lang 参数返回的翻译（Pixiv API 设计问题）
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun IllustDetailBody.toArtwork(ageLimitDeterminer: AgeLimitDeterminer): Artwork {
    // 直接从 PixivTag 的 translation["en"] 读取翻译（实际是当前语言的翻译）
    val translatedTags = this.tags.tags.map { pixivTag ->
        Tag(
            name = pixivTag.tag,
            translatedName = pixivTag.translation?.get("en")
        )
    }
    
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
        tags = translatedTags,
        viewCount = this.viewCount,
        likeCount = this.likeCount,
        bookmarkCount = this.bookmarkCount,
        commentCount = this.commentCount,
        createdTime = this.createDate,
        isBookmarked = this.bookmarkData != null,
        isMuted = false,
        isAiGenerated = isAiGeneratedArtwork(this.aiType, this.tags.tags.map { it.tag }),
        totalView = this.viewCount,
        totalBookmarks = this.bookmarkCount,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = this.xRestrict,
            sl = this.sl,
            tags = this.tags.tags.map { it.tag }
        ),
        ugoiraMetadata = null // 需要单独查询
    )
}

/**
 * 将 IllustSimple 转换为 Artwork
 * 
 * @param tagTranslationUtil 标签翻译工具
 * @param tagTranslation 全局标签翻译映射表（从 API 响应中获取）
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun IllustSimple.toArtwork(
    tagTranslationUtil: TagTranslationUtil,
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): Artwork {
    // 使用 TagTranslationUtil 翻译标签
    val translatedTags = tagTranslationUtil.translateTags(this.tags, tagTranslation)
    
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
        tags = translatedTags,
        viewCount = 0, // 简化版本不包含
        likeCount = 0,
        bookmarkCount = 0,
        commentCount = 0,
        createdTime = this.createDate,
        isBookmarked = this.bookmarkData != null,
        isMuted = this.isMasked,
        isAiGenerated = isAiGeneratedArtwork(this.aiType, this.tags),
        totalView = 0,
        totalBookmarks = 0,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = this.xRestrict,
            sl = this.sl,
            tags = this.tags
        ),
        ugoiraMetadata = null
    )
}

/**
 * 将 IllustSimple 列表转换为 Artwork 列表
 * 
 * @param tagTranslationUtil 标签翻译工具
 * @param tagTranslation 全局标签翻译映射表（从 API 响应中获取）
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun List<IllustSimple>.toArtworkList(
    tagTranslationUtil: TagTranslationUtil,
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): List<Artwork> {
    return this.map { it.toArtwork(tagTranslationUtil, tagTranslation, ageLimitDeterminer) }
}

/**
 * 将 RankingContent 转换为 Artwork
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun RankingContent.toArtwork(ageLimitDeterminer: AgeLimitDeterminer): Artwork {
    return Artwork(
        id = this.illust_id.toString(),
        title = this.title,
        description = "",
        type = when (this.illust_type) {
            "illustration" -> ArtworkType.ILLUSTRATION
            "manga" -> ArtworkType.MANGA
            "ugoira" -> ArtworkType.UGOIRA
            else -> ArtworkType.ILLUSTRATION
        },
        imageUrls = listOf(this.url),
        width = this.width,
        height = this.height,
        pageCount = this.illust_page_count.toIntOrNull() ?: 1,
        userId = this.user_id.toString(),
        userName = this.user_name,
        userProfileImageUrl = this.profile_img,
        tags = this.tags.map { Tag(name = it, translatedName = null) },
        viewCount = this.view_count,
        likeCount = this.rating_count,
        bookmarkCount = 0,
        commentCount = 0,
        createdTime = this.date,
        isBookmarked = this.is_bookmarked,
        isMuted = this.is_masked,
        isAiGenerated = isAiGeneratedArtwork(0, this.tags),
        totalView = this.view_count,
        totalBookmarks = 0,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = this.illust_content_type.sexual,
            tags = this.tags
        ),
        ugoiraMetadata = null
    )
}

/**
 * 将 RankingContent 列表转换为 Artwork 列表
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun List<RankingContent>.toArtworkList(ageLimitDeterminer: AgeLimitDeterminer): List<Artwork> {
    return this.map { it.toArtwork(ageLimitDeterminer) }
}

/**
 * 将 UgoiraMetaBody 转换为 UgoiraMetadata
 */
fun UgoiraMetaBody.toUgoiraMetadata(): UgoiraMetadata {
    return UgoiraMetadata(
        zipUrl = this.originalSrc,
        frames = this.frames.map { frame ->
            UgoiraFrame(
                file = frame.file,
                delay = frame.delay
            )
        }
    )
}
