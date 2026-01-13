package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.repository.WidgetRepository

/**
 * 获取Widget作品列表的UseCase
 */
class GetWidgetArtworksUseCase(
    private val widgetRepository: WidgetRepository
) {
    
    /**
     * 获取Widget作品列表
     * 
     * @param config Widget配置
     * @param forceRefresh 是否强制刷新
     * @return 过滤后的作品列表
     */
    suspend operator fun invoke(
        config: WidgetConfig,
        forceRefresh: Boolean = false
    ): Result<List<Artwork>> {
        return widgetRepository.getWidgetArtworks(config, forceRefresh)
    }
}
