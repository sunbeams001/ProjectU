package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.data.remote.model.IllustSearchMode
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.flow.Flow

/**
 * 作品仓储接口
 */
interface ArtworkRepository {
    
    /**
     * 获取作品详情
     */
    suspend fun getArtworkDetail(artworkId: Long): Result<Artwork>
    
    /**
     * 获取推荐作品
     */
    suspend fun getRecommendedArtworks(page: Int = 1, limit: Int = 30): Result<List<Artwork>>
    
    /**
     * 获取发现插画
     * @param mode 内容模式（全部/全年龄/R-18）
     * @param limit 返回数量
     */
    suspend fun getDiscoveryIllusts(
        mode: DiscoveryMode = DiscoveryMode.ALL,
        limit: Int = 100
    ): Result<List<Artwork>>
    
    /**
     * 获取关注用户作品
     */
    suspend fun getFollowingArtworks(page: Int = 1): Result<List<Artwork>>
    
    /**
     * 获取关注用户的最新插画
     * @param mode 模式：all, r18
     * @param page 页码
     * @return Pair<作品列表, 是否最后一页>
     */
    suspend fun getFollowLatestIllusts(
        mode: String = "all",
        page: Int = 1
    ): Result<Pair<List<Artwork>, Boolean>>
    
    /**
     * 搜索作品
     * @param aiType AI作品过滤：1(隐藏AI作品), null(显示AI作品)
     */
    suspend fun searchArtworks(
        keyword: String,
        page: Int = 1,
        searchMode: String = IllustSearchMode.DEFAULT.value,
        order: String = "date_desc",
        aiType: Int? = null
    ): Result<List<Artwork>>
    
    /**
     * 获取排行榜作品
     * @param mode 排行榜模式
     * @param content 内容类型（ALL, ILLUST, MANGA, UGOIRA）
     * @param page 页码
     * @param date 日期（格式：yyyyMMdd，可选）
     */
    suspend fun getRankingArtworks(
        mode: RankingMode = RankingMode.DAILY,
        content: RankingContent = RankingContent.ALL,
        page: Int = 1,
        date: String? = null
    ): Result<List<Artwork>>
    
    /**
     * 获取排行榜作品（包含日期信息）
     * @return Pair<作品列表, 日期信息(date, prevDate, nextDate)>
     */
    suspend fun getRankingWithDateInfo(
        mode: RankingMode = RankingMode.DAILY,
        content: RankingContent = RankingContent.ALL,
        page: Int = 1,
        date: String? = null
    ): Result<Pair<List<Artwork>, Triple<String?, String?, String?>>>
    
    /**
     * 添加收藏
     */
    suspend fun addBookmark(
        artworkId: Long,
        isPrivate: Boolean = false,
        tags: List<String> = emptyList()
    ): Result<Unit>
    
    /**
     * 移除收藏
     */
    suspend fun removeBookmark(artworkId: Long): Result<Unit>
    
    /**
     * 获取Ugoira动图元数据
     */
    suspend fun getUgoiraMetadata(artworkId: Long): Result<UgoiraMetadata>
    
    /**
     * 获取多页作品的所有页详情
     * 
     * 用于更新 Artwork 的 imageUrls.pages 字段
     * 只应在 pageCount > 1 时调用
     * 
     * @param artwork 现有的 Artwork 实例
     * @return 更新后的 Artwork（pages 字段包含所有页的完整信息）
     */
    suspend fun getArtworkPages(artwork: Artwork): Result<Artwork>
    
    /**
     * 观察作品详情（Flow版本）
     */
    fun observeArtworkDetail(artworkId: Long): Flow<Artwork>
}

