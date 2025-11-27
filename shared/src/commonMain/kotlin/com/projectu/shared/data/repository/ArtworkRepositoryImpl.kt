package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtwork
import com.projectu.shared.data.remote.mapper.toArtworkList
import com.projectu.shared.data.remote.mapper.toUgoiraMetadata
import com.projectu.shared.data.remote.mapper.updatePages
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.UgoiraMetadata
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 作品仓储实现
 * 基于 Pixiv Web API 实现
 */
class ArtworkRepositoryImpl(
    private val pixivApi: PixivApi,
    private val tagTranslationUtil: TagTranslationUtil,
    private val ageLimitDeterminer: AgeLimitDeterminer
) : ArtworkRepository {

    override suspend fun getArtworkDetail(artworkId: Long): Result<Artwork> = runCatching {
        val response = pixivApi.illustApi.getDetail(artworkId)
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.toArtwork(ageLimitDeterminer) ?: throw IllegalStateException("作品详情为空")
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
        response.body?.thumbnails?.illust?.toArtworkList(
            tagTranslationUtil = tagTranslationUtil,
            tagTranslation = response.body.tagTranslation,
            ageLimitDeterminer = ageLimitDeterminer
        ) ?: emptyList()
    }

    override suspend fun getDiscoveryIllusts(
        mode: DiscoveryMode,
        limit: Int
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.illustApi.getDiscovery(
            mode = mode.value,
            limit = limit
        )
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        response.body?.thumbnails?.illust?.toArtworkList(
            tagTranslationUtil = tagTranslationUtil,
            tagTranslation = response.body.tagTranslation,
            ageLimitDeterminer = ageLimitDeterminer
        ) ?: emptyList()
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
        response.body?.thumbnails?.illust?.toArtworkList(
            tagTranslationUtil = tagTranslationUtil,
            tagTranslation = response.body.tagTranslation,
            ageLimitDeterminer = ageLimitDeterminer
        ) ?: emptyList()
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
        response.body?.illustManga?.data?.toArtworkList(
            tagTranslationUtil = tagTranslationUtil,
            tagTranslation = response.body.tagTranslation,
            ageLimitDeterminer = ageLimitDeterminer
        ) ?: emptyList()
    }

    override suspend fun getRankingArtworks(
        mode: RankingMode,
        content: RankingContent,
        page: Int,
        date: String?
    ): Result<List<Artwork>> = runCatching {
        val response = pixivApi.rankingApi.getIllustRanking(
            mode = mode,
            content = content,
            page = page,
            date = date
        )
        response.contents.toArtworkList(ageLimitDeterminer)
    }
    
    override suspend fun getRankingWithDateInfo(
        mode: RankingMode,
        content: RankingContent,
        page: Int,
        date: String?
    ): Result<Pair<List<Artwork>, Triple<String?, String?, String?>>> = runCatching {
        val response = pixivApi.rankingApi.getIllustRanking(
            mode = mode,
            content = content,
            page = page,
            date = date
        )
        val artworks = response.contents.toArtworkList(ageLimitDeterminer)
        val dateInfo = Triple(response.date, response.prev_date, response.next_date)
        Pair(artworks, dateInfo)
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
        body.toUgoiraMetadata()
    }

    override suspend fun getArtworkPages(artwork: Artwork): Result<Artwork> = runCatching {
        // 检查是否为多页作品
        if (artwork.pageCount <= 1) {
            return@runCatching artwork
        }
        
        // 获取多页作品的所有页详情
        val response = pixivApi.illustApi.getPages(artwork.id.toLong())
        if (response.error) {
            throw IllegalStateException(response.message)
        }
        
        val pages = response.body ?: throw IllegalStateException("多页作品详情为空")
        artwork.updatePages(pages)
    }

    override fun observeArtworkDetail(artworkId: Long): Flow<Artwork> = flow {
        val result = getArtworkDetail(artworkId)
        result.getOrNull()?.let { emit(it) }
    }
}

