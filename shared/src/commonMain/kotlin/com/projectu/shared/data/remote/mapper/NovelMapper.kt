package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.novel.NovelDetailBody
import com.projectu.shared.data.remote.dto.novel.NovelRecommendBody
import com.projectu.shared.data.remote.dto.novel.NovelRecommendInitBody
import com.projectu.shared.data.remote.dto.novel.NovelSimple
import com.projectu.shared.data.remote.dto.ranking.NovelRankingItem
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelEmbeddedImageInfo
import com.projectu.shared.domain.model.NovelGenre
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil

/**
 * 小说 DTO 到 Domain 模型的映射器
 */

/**
 * 判断小说是否为 AI 生成
 * 
 * @param aiType AI 类型标识
 * @param tags 原始标签列表
 * @return true 表示是 AI 作品
 */
internal fun isAiGeneratedNovel(aiType: Int, tags: List<String>): Boolean {
    return aiType == 2 || tags.contains("AI小説")
}

/**
 * 将 NovelSimple 转换为 Novel
 * 
 * @param tagTranslation 标签翻译映射表
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun NovelSimple.toNovel(
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): Novel {
    // 翻译标签
    val translatedTags = tags.map { tagName ->
        Tag(
            name = tagName,
            translatedName = tagTranslation?.get(tagName)?.get("zh")
        )
    }
    
    return Novel(
        id = id,
        title = title,
        description = description,
        content = null, // 简化接口不返回正文
        imageUrl = url, // 从DTO的url字段获取封面
        userId = userId,
        userName = userName,
        userProfileImageUrl = profileImageUrl,
        tags = translatedTags,
        viewCount = 0, // NovelSimple 不包含浏览数
        likeCount = 0, // NovelSimple 不包含点赞数
        bookmarkCount = bookmarkCount ?: 0, // 被删除作品可能返回 null
        commentCount = 0, // NovelSimple 不包含评论数
        markerCount = marker ?: 0,
        createdTime = createDate,
        updatedTime = updateDate,
        bookmarkStatus = when {
            bookmarkData == null -> BookmarkStatus.NOT_BOOKMARKED
            bookmarkData.private -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = bookmarkData?.id,
        isMasked = isMasked,
        isAiGenerated = isAiGeneratedNovel(aiType, tags),
        isOriginal = isOriginal,
        isBungei = false, // NovelSimple 不包含此字段
        textCount = textCount,
        wordCount = wordCount,
        readingTime = readingTime,
        useWordCount = useWordCount,
        genre = NovelGenre.fromString(genre),
        language = language,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = xRestrict,
            tags = tags
        ),
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        isUnlisted = isUnlisted,
        pageCount = 1, // NovelSimple 不包含页数
        marker = marker
    )
}

/**
 * 将 NovelSimple 列表转换为 Novel 列表
 * 
 * @param tagTranslation 标签翻译映射表
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun List<NovelSimple>.toNovelList(
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): List<Novel> {
    return this.map { it.toNovel(tagTranslation, ageLimitDeterminer) }
}

/**
 * 将 NovelRecommendBody 转换为 Novel 列表
 * 用于推荐小说接口 (getRecommendNovels)
 * 
 * @param tagTranslationUtil 标签翻译工具
 * @param tagTranslation 全局标签翻译映射表
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun com.projectu.shared.data.remote.dto.novel.NovelRecommendBody.toNovelList(
    tagTranslationUtil: TagTranslationUtil,
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): List<Novel> {
    return this.novels.toNovelList(tagTranslation, ageLimitDeterminer)
}

/**
 * 将 NovelRecommendInitBody 转换为 Novel 列表
 * 用于推荐小说初始化接口 (getRecommendInit)
 * 
 * @param tagTranslationUtil 标签翻译工具
 * @param tagTranslation 全局标签翻译映射表
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun com.projectu.shared.data.remote.dto.novel.NovelRecommendInitBody.toNovelList(
    tagTranslationUtil: TagTranslationUtil,
    tagTranslation: Map<String, Map<String, String>>? = null,
    ageLimitDeterminer: AgeLimitDeterminer
): List<Novel> {
    return this.novels.toNovelList(tagTranslation, ageLimitDeterminer)
}

/**
 * 将 NovelDetailBody 转换为 Novel
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun NovelDetailBody.toNovel(ageLimitDeterminer: AgeLimitDeterminer): Novel {
    // 处理标签翻译（详情接口的标签结构不同）
    val translatedTags = tags.tags.map { pixivTag ->
        Tag(
            name = pixivTag.tag,
            translatedName = pixivTag.translation?.get("zh")
        )
    }
    
    // 转换内嵌图片
    val embeddedImagesMap = textEmbeddedImages?.mapValues { (_, imageDto) ->
        NovelEmbeddedImageInfo(
            imageId = imageDto.novelImageId,
            smallUrl = imageDto.urls.small,
            mediumUrl = imageDto.urls.medium,
            largeUrl = imageDto.urls.large,
            originalUrl = imageDto.urls.original
        )
    } ?: emptyMap()
    
    // 解析小说类型
    val novelGenre = genre?.let { NovelGenre.fromString(it) } ?: NovelGenre.OTHER
    
    return Novel(
        id = id,
        title = title,
        description = description,
        content = content, // 详情接口包含正文
        imageUrl = coverUrl ?: "", // 使用 coverUrl
        userId = userId,
        userName = userName,
        userProfileImageUrl = "", // 详情接口不返回用户头像，需从用户接口获取
        tags = translatedTags,
        viewCount = viewCount,
        likeCount = likeCount,
        bookmarkCount = bookmarkCount,
        commentCount = commentCount,
        markerCount = markerCount,
        createdTime = createDate,
        updatedTime = uploadDate,
        bookmarkStatus = when {
            bookmarkData == null -> BookmarkStatus.NOT_BOOKMARKED
            bookmarkData.private -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = bookmarkData?.id,
        isMasked = false,
        isAiGenerated = isAiGeneratedNovel(aiType, tags.tags.map { it.tag }),
        isOriginal = isOriginal,
        isBungei = isBungei,
        isLiked = likeData,
        textCount = characterCount ?: pageCount, // 优先使用 characterCount
        wordCount = wordCount ?: (characterCount ?: pageCount),
        readingTime = readingTime ?: ((characterCount ?: pageCount) / 500), // 优先使用API返回的阅读时间
        useWordCount = useWordCount,
        genre = novelGenre,
        language = language ?: "ja",
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = xRestrict,
            tags = tags.tags.map { it.tag }
        ),
        seriesId = seriesNavData?.seriesId?.toString(),
        seriesTitle = seriesNavData?.title,
        seriesOrder = seriesNavData?.order,
        isUnlisted = isUnlisted,
        pageCount = pageCount,
        marker = marker,  // 使用API返回的书签位置
        embeddedImages = embeddedImagesMap
    )
}

/**
 * 将 NovelRankingItem 转换为 Novel
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun NovelRankingItem.toNovel(ageLimitDeterminer: AgeLimitDeterminer): Novel {
    // 处理标签（排行榜返回的是简单的字符串列表）
    val translatedTags = tagA.map { tagName ->
        Tag(
            name = tagName,
            translatedName = null // 排行榜接口不返回翻译
        )
    }
    
    return Novel(
        id = id,
        title = title,
        description = comment,
        content = null, // 排行榜接口不返回正文
        imageUrl = url, // 从DTO的url字段获取封面
        userId = userId,
        userName = userName,
        userProfileImageUrl = profileImg,
        tags = translatedTags,
        viewCount = 0, // 排行榜接口不返回浏览数
        likeCount = 0, // 排行榜接口不返回点赞数
        bookmarkCount = bookmarkCount,
        commentCount = 0, // 排行榜接口不返回评论数
        markerCount = marker?.toIntOrNull() ?: 0,
        createdTime = createDate ?: "",
        updatedTime = createDate ?: "", // 排行榜只有创建日期
        bookmarkStatus = when {
            !isBookmarked -> BookmarkStatus.NOT_BOOKMARKED
            bookmarkRestrict == "1" -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = bookmarkId,
        isMasked = false, // 排行榜接口不返回此字段
        isAiGenerated = aiType == "2",
        isOriginal = isOriginal == "1",
        isBungei = false, // 排行榜接口不返回此字段
        textCount = characterCount,
        wordCount = wordCount,
        readingTime = readingTime,
        useWordCount = true,
        genre = NovelGenre.fromString(genre),
        language = language,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = xRestrict.toIntOrNull() ?: 0,
            tags = tagA
        ),
        seriesId = seriesId?.toString(),
        seriesTitle = seriesTitle,
        isUnlisted = false,
        pageCount = 1,
        marker = marker?.toIntOrNull()
    )
}

/**
 * 将 NovelRankingItem 列表转换为 Novel 列表
 * 
 * @param ageLimitDeterminer 年龄限制判定工具
 */
fun List<NovelRankingItem>.toNovelRankingList(ageLimitDeterminer: AgeLimitDeterminer): List<Novel> {
    return this.map { it.toNovel(ageLimitDeterminer) }
}


