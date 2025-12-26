package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.illust.IllustDetailBody
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.dto.illust.PageInfo
import com.projectu.shared.data.remote.dto.illust.UgoiraMetaBody
import com.projectu.shared.data.remote.dto.ranking.RankingContent
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.ArtworkImageUrls
import com.projectu.shared.domain.model.ArtworkType
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.ImageUrls
import com.projectu.shared.domain.model.PageImageUrls
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
    return aiType == 2 || tags.contains("AIイラスト") || tags.contains("AI生成作品")
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
        type = ArtworkType.fromIllustType(this.illustType),
        imageUrls = ArtworkImageUrls(
            pages = listOf(
                PageImageUrls(
                    page = 0,  // 单页作品页码为 0
                    urls = ImageUrls(
                        mini = this.urls.mini,           // 48x48 缩略图
                        squareMedium = this.urls.thumb,  // 250x250 custom-thumb
                        medium = null,                    // 详情接口不提供 360x360
                        large = this.urls.small,         // 540x540
                        master1200 = this.urls.regular,  // img-master/master1200
                        original = this.urls.original    // img-original（详情接口独有）
                    ),
                    width = this.width,
                    height = this.height
                )
            )
        ),
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
        bookmarkStatus = when {
            this.bookmarkData == null -> BookmarkStatus.NOT_BOOKMARKED
            this.bookmarkData.private -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = this.bookmarkData?.id,
        isMuted = false,
        isAiGenerated = isAiGeneratedArtwork(this.aiType, this.tags.tags.map { it.tag }),
        totalView = this.viewCount,
        totalBookmarks = this.bookmarkCount,
        ageLimit = ageLimitDeterminer.determine(
            xRestrict = this.xRestrict,
            sl = this.sl,
            tags = this.tags.tags.map { it.tag }
        ),
        seriesId = this.seriesNavData?.seriesId,
        seriesTitle = this.seriesNavData?.title,
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
        type = ArtworkType.fromIllustType(this.illustType),
        imageUrls = ArtworkImageUrls(
            pages = listOf(
                PageImageUrls(
                    page = 0,  // 单页作品页码为 0
                    urls = ImageUrls(
                        mini = null,                                            // 发现接口不提供 mini
                        squareMedium = this.urls?.get("250x250") ?: this.url,  // 250x250 custom-thumb
                        medium = this.urls?.get("360x360"),                     // 360x360 custom-thumb
                        large = this.urls?.get("540x540"),                      // 540x540
                        master1200 = this.urls?.get("1200x1200"),               // img-master/master1200
                        original = null                                         // 发现接口不返回原图
                    ),
                    width = this.width,
                    height = this.height
                )
            )
        ),
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
        bookmarkStatus = when {
            this.bookmarkData == null -> BookmarkStatus.NOT_BOOKMARKED
            this.bookmarkData.private -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = this.bookmarkData?.id,
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
        type = ArtworkType.fromString(this.illust_type),
        imageUrls = ArtworkImageUrls(
            pages = listOf(
                PageImageUrls(
                    page = 0,  // 单页作品页码为 0
                    urls = ImageUrls(
                        mini = null,
                        squareMedium = this.url,  // 排行榜只返回一个 480x960 的缩略图
                        medium = null,
                        large = null,
                        master1200 = null,
                        original = null           // 排行榜接口不返回原图
                    ),
                    width = this.width,
                    height = this.height
                )
            )
        ),
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
        bookmarkStatus = when {
            !this.is_bookmarked -> BookmarkStatus.NOT_BOOKMARKED
            this.bookmark_illust_restrict == "1" -> BookmarkStatus.PRIVATE
            else -> BookmarkStatus.PUBLIC
        },
        bookmarkId = this.bookmark_id,
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

/**
 * 将 PageInfo 列表转换为 PageImageUrls 列表
 * 
 * 用于将多页插画的 API 响应转换为 Domain 模型
 * 
 * API 响应示例（/ajax/illust/{pid}/pages）：
 * ```json
 * {
 *   "urls": {
 *     "thumb_mini": "https://i.pximg.net/c/128x128/img-master/...",
 *     "small": "https://i.pximg.net/c/540x540_70/img-master/...",
 *     "regular": "https://i.pximg.net/img-master/.../p0_master1200.jpg",
 *     "original": "https://i.pximg.net/img-original/.../p0.png"
 *   },
 *   "width": 1536,
 *   "height": 1024
 * }
 * ```
 */
fun List<PageInfo>.toImagePages(): List<PageImageUrls> {
    return this.mapIndexed { index, pageInfo ->
        PageImageUrls(
            page = index,
            urls = ImageUrls(
                mini = pageInfo.urls.thumb_mini,   // 128x128
                squareMedium = pageInfo.urls.thumb_mini, // 使用 mini 作为 fallback
                medium = null,                      // pages 接口不提供此尺寸
                large = pageInfo.urls.small,       // 540x540
                master1200 = pageInfo.urls.regular, // img-master/master1200
                original = pageInfo.urls.original  // img-original（原图）
            ),
            width = pageInfo.width,
            height = pageInfo.height
        )
    }
}

/**
 * 使用 PageInfo 列表更新 Artwork 的 pages 字段
 * 
 * 用于在获取作品详情后，进一步获取多页作品的所有页详情
 * 
 * @receiver 现有的 Artwork 实例
 * @param pageInfos 从 getPages API 获取的 PageInfo 列表
 * @return 更新后的 Artwork（imageUrls.pages 包含所有页的完整信息）
 */
fun Artwork.updatePages(pageInfos: List<PageInfo>): Artwork {
    return this.copy(
        imageUrls = ArtworkImageUrls(
            pages = pageInfos.toImagePages()
        )
    )
}
