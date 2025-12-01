package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries

/**
 * 小说系列仓库接口
 * 
 * 提供小说系列相关的数据访问功能
 */
interface NovelSeriesRepository {
    
    /**
     * 获取小说系列详情
     * @param seriesId 系列ID
     * @return 系列详情
     */
    suspend fun getSeriesDetail(seriesId: Long): Result<NovelSeries>
    
    /**
     * 获取系列中的作品列表
     * @param seriesId 系列ID
     * @param limit 返回数量
     * @param lastOrder 最后一个作品的序号（用于分页）
     * @param orderBy 排序方式：asc(升序), desc(降序)
     * @return 作品列表（Novel 包含 seriesOrder 字段）
     */
    suspend fun getSeriesContents(
        seriesId: Long,
        limit: Int = 30,
        lastOrder: Int? = null,
        orderBy: String = "asc"
    ): Result<List<Novel>>
    
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
