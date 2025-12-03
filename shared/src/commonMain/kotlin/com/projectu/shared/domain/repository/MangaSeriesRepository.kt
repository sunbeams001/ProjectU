package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries

/**
 * 系列作品列表结果
 * 包含作品列表和总数信息
 */
data class SeriesWorksResult(
    val artworks: List<Artwork>,
    val total: Int
)

/**
 * 漫画系列仓库接口
 * 
 * 提供漫画系列相关的数据访问功能
 */
interface MangaSeriesRepository {
    
    /**
     * 获取漫画系列详情
     * @param seriesId 系列ID
     * @param page 页码（从1开始）
     * @return 系列详情
     */
    suspend fun getSeriesDetail(seriesId: Long, page: Int = 1): Result<MangaSeries>
    
    /**
     * 获取系列中的作品列表
     * 从系列详情中提取的缩略图作品数据
     * @param seriesId 系列ID
     * @param page 页码（从1开始）
     * @return 作品列表结果（包含作品列表和总数）
     */
    suspend fun getSeriesWorks(seriesId: Long, page: Int = 1): Result<SeriesWorksResult>
    
    /**
     * 加入追更列表（Watch）
     * @param seriesId 系列ID
     */
    suspend fun watchSeries(seriesId: Long): Result<Unit>
    
    /**
     * 取消追更（Unwatch）
     * @param seriesId 系列ID
     */
    suspend fun unwatchSeries(seriesId: Long): Result<Unit>
}
