package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.model.WidgetDataSource
import com.projectu.shared.domain.model.WidgetRankingMode

/**
 * Widget 配置数据库实体
 */
@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey
    val widgetId: Int,
    
    val dataSource: String,  // RECOMMENDED / FOLLOWING_LATEST / RANKING
    
    val rankingMode: String?,  // DAY / WEEK / MONTH (nullable)
    
    val r18Filter: String,  // MUST_BE / MUST_NOT_BE / ANY
    
    val aiFilter: String,  // MUST_BE / MUST_NOT_BE / ANY
    
    val updateIntervalMinutes: Int,
    
    val showRefreshButton: Boolean,
    
    val imageScaleType: String,  // FIT_CENTER / CENTER_CROP
    
    val currentArtworkId: String?,
    
    val currentIndex: Int,
    
    val createdAt: Long,
    
    val lastUpdatedAt: Long
)

/**
 * 领域模型转实体
 */
fun WidgetConfig.toEntity(): WidgetConfigEntity {
    return WidgetConfigEntity(
        widgetId = widgetId,
        dataSource = dataSource.name,
        rankingMode = rankingMode?.name,
        r18Filter = r18Filter.name,
        aiFilter = aiFilter.name,
        updateIntervalMinutes = updateIntervalMinutes,
        showRefreshButton = showRefreshButton,
        imageScaleType = imageScaleType.name,
        currentArtworkId = currentArtworkId,
        currentIndex = currentIndex,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt
    )
}

/**
 * 实体转领域模型
 */
fun WidgetConfigEntity.toDomain(): WidgetConfig {
    return WidgetConfig(
        widgetId = widgetId,
        dataSource = WidgetDataSource.valueOf(dataSource),
        rankingMode = rankingMode?.let { WidgetRankingMode.valueOf(it) },
        r18Filter = FilterType.valueOf(r18Filter),
        aiFilter = FilterType.valueOf(aiFilter),
        updateIntervalMinutes = updateIntervalMinutes,
        showRefreshButton = showRefreshButton,
        imageScaleType = com.projectu.shared.domain.model.WidgetImageScaleType.valueOf(imageScaleType),
        currentArtworkId = currentArtworkId,
        currentIndex = currentIndex,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt
    )
}
