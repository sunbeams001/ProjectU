package com.projectu.shared.data.repository

import com.projectu.shared.data.local.dao.WidgetConfigDao
import com.projectu.shared.data.local.entity.toDomain
import com.projectu.shared.data.local.entity.toEntity
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.model.WidgetDataSource
import com.projectu.shared.domain.model.WidgetRankingMode
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.WidgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Widget 作品缓存数据
 */
private data class WidgetArtworkCache(
    val artworks: List<Artwork>,
    val timestamp: Long,
    val cacheKey: String
) {
    companion object {
        // 缓存有效期：30分钟
        const val CACHE_DURATION_MS = 30 * 60 * 1000L
    }
    
    /**
     * 检查缓存是否有效
     */
    fun isValid(): Boolean {
        return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS
    }
}

/**
 * Widget 仓储实现
 */
class WidgetRepositoryImpl(
    private val widgetConfigDao: WidgetConfigDao,
    private val artworkRepository: ArtworkRepository
) : WidgetRepository {
    
    // Widget作品缓存 - 键：widgetId，值：缓存数据
    private val artworkCache = mutableMapOf<Int, WidgetArtworkCache>()
    
    override suspend fun saveWidgetConfig(config: WidgetConfig) {
        widgetConfigDao.upsertConfig(config.toEntity())
    }
    
    override suspend fun getWidgetConfig(widgetId: Int): WidgetConfig? {
        return widgetConfigDao.getConfig(widgetId)?.toDomain()
    }
    
    override fun getAllWidgetConfigs(): Flow<List<WidgetConfig>> {
        return widgetConfigDao.getAllConfigs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun deleteWidgetConfig(widgetId: Int) {
        widgetConfigDao.deleteConfig(widgetId)
    }
    
    override suspend fun updateCurrentArtwork(
        widgetId: Int,
        artworkId: String,
        index: Int
    ) {
        widgetConfigDao.updateCurrentArtwork(
            widgetId = widgetId,
            artworkId = artworkId,
            index = index,
            timestamp = System.currentTimeMillis()
        )
    }
    
    override suspend fun getWidgetArtworks(
        config: WidgetConfig,
        forceRefresh: Boolean
    ): Result<List<Artwork>> {
        // 生成缓存键（基于数据源、排行榜模式、过滤条件）
        val cacheKey = "${config.dataSource}_${config.rankingMode}_${config.r18Filter}_${config.aiFilter}"
        
        // 检查缓存
        val cached = artworkCache[config.widgetId]
        if (!forceRefresh && cached != null && cached.cacheKey == cacheKey && cached.isValid()) {
            return Result.success(cached.artworks)
        }
        
        if (forceRefresh) {
            artworkCache.remove(config.widgetId)
        }
        
        // 根据数据源获取作品
        val artworksResult = when (config.dataSource) {
            WidgetDataSource.RECOMMENDED -> {
                artworkRepository.getRecommendedArtworks(page = 1, limit = 30)
            }
            WidgetDataSource.FOLLOWING_LATEST -> {
                artworkRepository.getFollowLatestIllusts(mode = "all", page = 1)
                    .map { it.first } // 只取作品列表部分
            }
            WidgetDataSource.RANKING -> {
                val mode = when (config.rankingMode) {
                    WidgetRankingMode.DAY -> RankingMode.DAILY
                    WidgetRankingMode.WEEK -> RankingMode.WEEKLY
                    WidgetRankingMode.MONTH -> RankingMode.MONTHLY
                    null -> RankingMode.DAILY
                }
                artworkRepository.getRankingArtworks(
                    mode = mode,
                    content = RankingContent.ILLUST,
                    page = 1
                )
            }
        }
        
        // 应用过滤器并缓存结果
        return artworksResult.map { artworks ->
            val filtered = artworks.filter { artwork ->
                // R-18 过滤
                val isR18 = artwork.ageLimit == AgeLimit.R18 || artwork.ageLimit == AgeLimit.R18G
                val r18Match = when (config.r18Filter) {
                    FilterType.MUST_BE -> isR18
                    FilterType.MUST_NOT_BE -> !isR18
                    FilterType.ANY -> true
                }
                
                // AI 过滤
                val aiMatch = when (config.aiFilter) {
                    FilterType.MUST_BE -> artwork.isAiGenerated
                    FilterType.MUST_NOT_BE -> !artwork.isAiGenerated
                    FilterType.ANY -> true
                }
                
                r18Match && aiMatch
            }
            
            // 缓存过滤后的结果
            if (filtered.isNotEmpty()) {
                artworkCache[config.widgetId] = WidgetArtworkCache(
                    artworks = filtered,
                    timestamp = System.currentTimeMillis(),
                    cacheKey = cacheKey
                )
            }
            
            filtered
        }
    }
}

