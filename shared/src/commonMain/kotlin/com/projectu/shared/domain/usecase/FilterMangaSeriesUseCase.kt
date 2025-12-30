package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.cache.BlockRuleCache
import com.projectu.shared.domain.filter.ContentFilter
import com.projectu.shared.domain.model.MangaSeries

/**
 * 漫画系列过滤用例
 * 
 * 根据启用的屏蔽规则过滤漫画系列列表
 * 使用 BlockRuleCache 获取规则（内存访问，零延迟）
 */
class FilterMangaSeriesUseCase(
    private val blockRuleCache: BlockRuleCache
) {
    /**
     * 过滤漫画系列列表
     * 
     * @param series 原始漫画系列列表
     * @return 过滤后的漫画系列列表
     */
    operator fun invoke(series: List<MangaSeries>): List<MangaSeries> {
        // 从内存缓存读取启用的规则
        val enabledRules = blockRuleCache.getEnabledRules()
        
        // 快速路径：没有启用的规则时直接返回
        if (enabledRules.isEmpty()) return series
        
        // 创建过滤器并应用
        val filter = ContentFilter(enabledRules)
        return filter.filterMangaSeries(series)
    }
}
