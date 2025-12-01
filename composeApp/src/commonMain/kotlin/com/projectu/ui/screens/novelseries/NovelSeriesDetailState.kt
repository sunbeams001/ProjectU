package com.projectu.ui.screens.novelseries

import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries

/**
 * 小说系列详情页状态
 */
data class NovelSeriesDetailState(
    // 系列信息
    val series: NovelSeries? = null,
    val isLoadingSeries: Boolean = false,
    val seriesError: String? = null,
    
    // 系列内容（小说列表）- 使用 Novel 模型以便复用 NovelCard
    val novels: List<Novel> = emptyList(),
    val isLoadingContents: Boolean = false,
    val contentsError: String? = null,
    val hasMore: Boolean = true,
    val lastOrder: Int? = null,
    
    // 追更操作状态
    val isWatchLoading: Boolean = false,
    val watchError: String? = null
)
