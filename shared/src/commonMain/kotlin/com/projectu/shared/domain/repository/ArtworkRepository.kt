package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.flow.Flow

/**
 * 作品仓储接口
 */
interface ArtworkRepository {
    
    /**
     * 获取作品详情
     */
    suspend fun getArtworkDetail(artworkId: String): Result<Artwork>
    
    /**
     * 获取推荐作品
     */
    suspend fun getRecommendedArtworks(page: Int = 1, limit: Int = 30): Result<List<Artwork>>
    
    /**
     * 获取关注用户作品
     */
    suspend fun getFollowingArtworks(page: Int = 1): Result<List<Artwork>>
    
    /**
     * 搜索作品
     */
    suspend fun searchArtworks(
        keyword: String,
        page: Int = 1,
        searchMode: String = "s_tag",
        order: String = "date_desc"
    ): Result<List<Artwork>>
    
    /**
     * 获取排行榜作品
     */
    suspend fun getRankingArtworks(
        mode: String = "daily",
        page: Int = 1,
        date: String? = null
    ): Result<List<Artwork>>
    
    /**
     * 添加收藏
     */
    suspend fun addBookmark(
        artworkId: String,
        isPrivate: Boolean = false,
        tags: List<String> = emptyList()
    ): Result<Unit>
    
    /**
     * 移除收藏
     */
    suspend fun removeBookmark(artworkId: String): Result<Unit>
    
    /**
     * 获取Ugoira动图元数据
     */
    suspend fun getUgoiraMetadata(artworkId: String): Result<UgoiraMetadata>
    
    /**
     * 观察作品详情（Flow版本）
     */
    fun observeArtworkDetail(artworkId: String): Flow<Artwork>
}

