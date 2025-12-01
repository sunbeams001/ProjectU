package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.novel_series.NovelSeriesBody
import com.projectu.shared.data.remote.dto.novel_series.NovelThumbnail
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelGenre
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.domain.model.Tag

/**
 * 小说系列 DTO 到 Domain 模型的映射器
 */

/**
 * 将 NovelSeriesBody 转换为 NovelSeries
 */
fun NovelSeriesBody.toNovelSeries(): NovelSeries {
    return NovelSeries(
        id = id,
        title = title,
        caption = caption,
        userId = userId,
        userName = userName,
        profileImageUrl = profileImageUrl,
        coverUrl = cover?.urls?.let { urls ->
            urls.size480mw ?: urls.size240mw ?: urls.size1200x1200 ?: urls.original
        },
        isOriginal = isOriginal,
        isConcluded = isConcluded,
        isWatched = isWatched,
        isNotifying = isNotifying,
        contentCount = publishedContentCount,
        totalCharacterCount = publishedTotalCharacterCount,
        totalWordCount = publishedTotalWordCount,
        readingTimeSeconds = publishedReadingTime, // API 返回的是秒
        watchCount = watchCount,
        tags = tags,
        genreId = genreId,
        language = language,
        xRestrict = xRestrict,
        maxXRestrict = maxXRestrict,
        aiType = aiType,
        createDate = createDate,
        updateDate = updateDate,
        createdTimestamp = createdTimestamp,
        updatedTimestamp = updatedTimestamp,
        lastPublishedContentTimestamp = lastPublishedContentTimestamp,
        firstNovelId = firstNovelId,
        latestNovelId = latestNovelId,
        hasGlossary = hasGlossary
    )
}

/**
 * 将 NovelThumbnail 转换为 Novel
 * 
 * NovelThumbnail 包含了完整的小说信息，用于显示系列内小说列表
 * 
 * @param order 在系列中的顺序号
 */
fun NovelThumbnail.toNovel(order: Int = 0): Novel {
    // 转换标签
    val translatedTags = tags.map { tagName ->
        Tag(
            name = tagName,
            translatedName = null // 系列内容接口不返回翻译
        )
    }
    
    // 判断年龄限制
    val ageLimit = when (xRestrict) {
        1 -> AgeLimit.R18
        2 -> AgeLimit.R18G
        else -> AgeLimit.ALL_AGE
    }
    
    return Novel(
        id = id,
        title = title,
        description = description ?: "",
        content = null,
        imageUrl = url ?: "",
        userId = userId,
        userName = userName,
        userProfileImageUrl = profileImageUrl ?: "",
        tags = translatedTags,
        viewCount = 0,
        likeCount = 0,
        bookmarkCount = bookmarkCount,
        commentCount = 0,
        markerCount = 0,
        createdTime = createDate,
        updatedTime = updateDate,
        bookmarkStatus = if (bookmarkData != null) BookmarkStatus.PUBLIC else BookmarkStatus.NOT_BOOKMARKED,
        bookmarkId = null,
        isMasked = isMasked,
        isAiGenerated = aiType == 2,
        isOriginal = isOriginal,
        isBungei = false,
        textCount = textCount,
        wordCount = wordCount,
        readingTime = readingTime,
        useWordCount = useWordCount,
        genre = NovelGenre.fromString(genre),
        language = language ?: "ja",
        ageLimit = ageLimit,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        isUnlisted = isUnlisted,
        pageCount = 1,
        marker = null,
        seriesOrder = order // 设置系列中的顺序
    )
}

/**
 * 将 NovelThumbnail 列表转换为 Novel 列表
 * 
 * @param contentOrderMap ID 到序号的映射表
 */
fun List<NovelThumbnail>.toNovelListForSeries(contentOrderMap: Map<String, Int>): List<Novel> {
    return this.mapIndexed { index, thumbnail ->
        val order = contentOrderMap[thumbnail.id] ?: (index + 1)
        thumbnail.toNovel(order)
    }.sortedBy { it.seriesOrder }
}

/**
 * 将 UserProfile 中的 NovelSeriesInfo 转换为 NovelSeries
 * 
 * 用于用户页面的小说系列列表显示
 */
fun com.projectu.shared.data.remote.dto.user.NovelSeriesInfo.toNovelSeries(): NovelSeries {
    return NovelSeries(
        id = id,
        title = title,
        caption = caption ?: "",
        userId = userId,
        userName = userName ?: "",
        profileImageUrl = profileImageUrl,
        coverUrl = cover?.urls?.`240mw` ?: cover?.urls?.`128x128` ?: firstEpisode?.url,
        isOriginal = isOriginal,
        isConcluded = isConcluded,
        isWatched = isWatched,
        isNotifying = isNotifying,
        contentCount = publishedContentCount,
        totalCharacterCount = publishedTotalCharacterCount,
        totalWordCount = publishedTotalWordCount,
        readingTimeSeconds = publishedReadingTime,
        watchCount = watchCount,
        tags = tags ?: emptyList(),
        genreId = genreId,
        language = language,
        xRestrict = xRestrict,
        maxXRestrict = maxXRestrict,
        aiType = aiType,
        createDate = createDate ?: "",
        updateDate = updateDate ?: "",
        createdTimestamp = createdTimestamp,
        updatedTimestamp = updatedTimestamp,
        lastPublishedContentTimestamp = lastPublishedContentTimestamp,
        firstNovelId = firstNovelId,
        latestNovelId = latestNovelId,
        hasGlossary = false
    )
}
