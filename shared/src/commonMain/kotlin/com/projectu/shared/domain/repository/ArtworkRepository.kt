package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.UgoiraMetadata
import kotlinx.coroutines.flow.Flow

/**
 * 作品仓储接口
 */
interface ArtworkRepository {
    
    /**
     * 获取推荐作品流
     */
    fun getRecommendedArtworks(): Flow<Result<List<Artwork>>>
    
    /**
     * 获取关注用户作品流
     */
    fun getFollowingArtworks(): Flow<Result<List<Artwork>>>
    
    /**
     * 根据ID获取作品详情
     */
    suspend fun getArtworkById(id: String): Result<Artwork>
    
    /**
     * 搜索作品
     */
    fun searchArtworks(keyword: String): Flow<Result<List<Artwork>>>
    
    /**
     * 获取用户作品列表
     */
    fun getUserArtworks(userId: String): Flow<Result<List<Artwork>>>
    
    /**
     * 获取排行榜作品
     */
    fun getRankingArtworks(mode: String, date: String? = null): Flow<Result<List<Artwork>>>
    
    /**
     * 收藏作品
     */
    suspend fun bookmarkArtwork(artworkId: String): Result<Unit>
    
    /**
     * 取消收藏
     */
    suspend fun unbookmarkArtwork(artworkId: String): Result<Unit>
    
    /**
     * 获取Ugoira动图元数据
     */
    suspend fun getUgoiraMetadata(artworkId: String): Result<UgoiraMetadata>
}

