package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtwork
import com.projectu.shared.data.remote.mapper.toArtworkList
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.UgoiraMetadata
import com.projectu.shared.domain.repository.ArtworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 作品仓储实现
 * 基于 Pixiv Web API 实现
 */
class ArtworkRepositoryImpl(
    private val pixivApi: PixivApi
) : ArtworkRepository {

    override suspend fun getArtworkDetail(artworkId: Long): Result<Artwork> = runCatching {
        val response = pixivApi.illustApi.getDetail(artworkId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.toArtwork() ?: throw IllegalStateException("作品详情为空")
    }

    override suspend fun getRecommendedArtworks(
        page: Int,
        limit: Int
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.illustApi.getDiscovery(
            mode = "all",
            limit = limit
        )
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.thumbnails?.illust?.toArtworkList() ?: emptyList()
    }

    override suspend fun getFollowingArtworks(
        page: Int
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.illustApi.getFollowLatest(
            mode = "all",
            page = page
        )
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.thumbnails?.illust?.toArtworkList() ?: emptyList()
    }

    override suspend fun searchArtworks(
        keyword: String,
        page: Int,
        searchMode: String,
        order: String
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.illustApi.search(
            keyword = keyword,
            searchMode = searchMode,
            order = order,
            page = page
        )
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.illustManga?.data?.toArtworkList() ?: emptyList()
    }

    override suspend fun getRankingArtworks(
        mode: RankingMode,
        page: Int,
        date: String?
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.rankingApi.getIllustRanking(
            mode = mode,
            page = page,
            date = date
        )
        response.contents.map { content ->
            Artwork(
                id = content.illust_id.toString(),
                title = content.title,
                description = "",
                type = when (content.illust_type) {
                    "illustration" -> com.projectu.shared.domain.model.ArtworkType.ILLUSTRATION
                    "manga" -> com.projectu.shared.domain.model.ArtworkType.MANGA
                    "ugoira" -> com.projectu.shared.domain.model.ArtworkType.UGOIRA
                    else -> com.projectu.shared.domain.model.ArtworkType.ILLUSTRATION
                },
                imageUrls = listOf(content.url),
                width = content.width,
                height = content.height,
                pageCount = content.illust_page_count.toIntOrNull() ?: 1,
                userId = content.user_id.toString(),
                userName = content.user_name,
                userProfileImageUrl = content.profile_img,
                tags = content.tags,
                viewCount = content.view_count,
                likeCount = content.rating_count,
                bookmarkCount = 0,
                commentCount = 0,
                createdTime = content.date,
                isBookmarked = false,
                isMuted = false,
                totalView = content.view_count,
                totalBookmarks = 0,
                ageLimit = com.projectu.shared.domain.model.AgeLimit.ALL_AGE,
                ugoiraMetadata = null
            )
        }
    }

    override suspend fun addBookmark(
        artworkId: Long,
        isPrivate: Boolean,
        tags: List<String>
    ): Result<Unit> = runCatching {
        val response = pixivApi.bookmarkApi.addIllust(
            illustId = artworkId,
            restrict = if (isPrivate) 1 else 0,
            tags = tags
        )
        if (response.error) {
            throw IllegalStateException(response.message)
        }
    }

    override suspend fun removeBookmark(artworkId: Long): Result<Unit> = runCatching {
        // 需要先获取作品详情来获取bookmarkId
        val detailResponse = pixivApi.illustApi.getDetail(artworkId)
        if (detailResponse.error) {
            throw IllegalStateException(detailResponse.message)
        }
        val bookmarkId = detailResponse.body?.bookmarkData?.id
            ?: throw IllegalStateException("作品未收藏")
        
        val response = pixivApi.bookmarkApi.deleteIllust(bookmarkId.toString())
        if (response.error) {
            throw IllegalStateException(response.message)
        }
    }

    override suspend fun getUgoiraMetadata(artworkId: Long): Result<UgoiraMetadata> = runCatching {
        val response = pixivApi.illustApi.getUgoiraMeta(artworkId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        val body = response.body ?: throw IllegalStateException("Ugoira元数据为空")
        
        UgoiraMetadata(
            zipUrl = body.originalSrc,
            frames = body.frames.map { frame ->
                com.projectu.shared.domain.model.UgoiraFrame(
                    file = frame.file,
                    delay = frame.delay
                )
            }
        )
    }

    override fun observeArtworkDetail(artworkId: Long): Flow<Artwork> = flow {
        val result = getArtworkDetail(artworkId)
        result.getOrNull()?.let { emit(it) }
    }
}

