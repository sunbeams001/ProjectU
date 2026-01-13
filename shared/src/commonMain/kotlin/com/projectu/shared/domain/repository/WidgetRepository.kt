package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

/**
 * Widget 仓储接口
 */
interface WidgetRepository {
    
    /**
     * 保存 Widget 配置
     */
    suspend fun saveWidgetConfig(config: WidgetConfig)
    
    /**
     * 获取 Widget 配置
     */
    suspend fun getWidgetConfig(widgetId: Int): WidgetConfig?
    
    /**
     * 获取所有 Widget 配置
     */
    fun getAllWidgetConfigs(): Flow<List<WidgetConfig>>
    
    /**
     * 删除 Widget 配置
     */
    suspend fun deleteWidgetConfig(widgetId: Int)
    
    /**
     * 更新当前显示的作品
     */
    suspend fun updateCurrentArtwork(
        widgetId: Int,
        artworkId: String,
        index: Int
    )
    
    /**
     * 获取 Widget 作品列表（带缓存）
     */
    suspend fun getWidgetArtworks(
        config: WidgetConfig,
        forceRefresh: Boolean = false
    ): Result<List<Artwork>>
}
