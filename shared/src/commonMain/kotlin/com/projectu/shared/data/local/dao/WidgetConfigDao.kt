package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.WidgetConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Widget 配置 DAO
 */
@Dao
interface WidgetConfigDao {
    
    /**
     * 插入或更新 Widget 配置
     */
    @Upsert
    suspend fun upsertConfig(config: WidgetConfigEntity)
    
    /**
     * 获取指定 Widget 的配置
     */
    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun getConfig(widgetId: Int): WidgetConfigEntity?
    
    /**
     * 获取所有 Widget 配置
     */
    @Query("SELECT * FROM widget_configs")
    fun getAllConfigs(): Flow<List<WidgetConfigEntity>>
    
    /**
     * 删除指定 Widget 的配置
     */
    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun deleteConfig(widgetId: Int)
    
    /**
     * 更新当前显示的作品
     */
    @Query("""
        UPDATE widget_configs 
        SET currentArtworkId = :artworkId, 
            currentIndex = :index,
            lastUpdatedAt = :timestamp
        WHERE widgetId = :widgetId
    """)
    suspend fun updateCurrentArtwork(
        widgetId: Int,
        artworkId: String,
        index: Int,
        timestamp: Long
    )
}
