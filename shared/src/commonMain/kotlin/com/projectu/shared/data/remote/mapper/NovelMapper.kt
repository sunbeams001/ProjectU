package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.novel.NovelDetailBody
import com.projectu.shared.data.remote.dto.novel.NovelSimple
import com.projectu.shared.data.remote.dto.ranking.NovelRankingItem
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelGenre
import com.projectu.shared.domain.model.Tag
import com.projectu.shared.util.AgeLimitDeterminer

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
        bookmarkCount = bookmarkCount,
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
    
    return Novel(
        id = id,
        title = title,
        description = description,
        content = content, // 详情接口包含正文
        imageUrl = "", // 详情接口不返回封面URL
        userId = userId,
        userName = userName,
        userProfileImageUrl = "", // 详情接口不返回用户头像
        tags = translatedTags,
        viewCount = viewCount,
        likeCount = likeCount,
        bookmarkCount = bookmarkCount,
        commentCount = commentCount,
        markerCount = markerCount,
        createdTime = createDate,
        updatedTime = uploadDate,
        bookmarkStatus = BookmarkStatus.NOT_BOOKMARKED, // 需要单独查询
        bookmarkId = null,
        isMasked = false,
        isAiGenerated = isAiGeneratedNovel(0, tags.tags.map { it.tag }),
        isOriginal = isOriginal,
        isBungei = isBungei,
        textCount = pageCount, // 详情接口的 pageCount 实际是文字数
        wordCount = pageCount,
        readingTime = pageCount / 500, // 按平均阅读速度估算
        useWordCount = false,
        genre = NovelGenre.OTHER, // 详情接口不返回类型
        language = "ja",
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = xRestrict,
            tags = tags.tags.map { it.tag }
        ),
        seriesId = null, // 需要从其他接口获取
        seriesTitle = null,
        isUnlisted = false,
        pageCount = pageCount,
        marker = null
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
        bookmarkStatus = if (isBookmarked) BookmarkStatus.PUBLIC else BookmarkStatus.NOT_BOOKMARKED,
        bookmarkId = null, // 排行榜接口不返回收藏ID
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


