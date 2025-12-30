package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.cache.BlockRuleCache
import com.projectu.shared.domain.filter.ContentFilter
import com.projectu.shared.domain.model.Artwork

/**
 * 过滤作品 UseCase
 * 
 * 根据启用的屏蔽规则过滤作品列表
 * 使用 BlockRuleCache 内存缓存，避免频繁查询数据库
 */
class FilterArtworksUseCase(
    private val blockRuleCache: BlockRuleCache
) {
    
    /**
     * 执行过滤
     * 
     * @param artworks 待过滤的作品列表
     * @return 过滤后的作品列表
     */
    operator fun invoke(artworks: List<Artwork>): List<Artwork> {
        // 从内存缓存读取启用的规则，不查询数据库
        val enabledRules = blockRuleCache.getEnabledRules()
        
        // 快速路径：如果没有启用的规则，直接返回原列表
        if (enabledRules.isEmpty()) {
            return artworks
        }
        
        // 创建过滤器并执行过滤
        val filter = ContentFilter(enabledRules)
        return filter.filter(artworks)
    }
}
