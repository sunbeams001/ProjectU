package com.projectu.ui.screens.mangaseries

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries

/**
 * 漫画系列详情页状态
 */
data class MangaSeriesDetailState(
    // 系列信息
    val series: MangaSeries? = null,
    val isLoadingSeries: Boolean = false,
    val seriesError: String? = null,
    
    // 系列内容（作品列表）
    val artworks: List<Artwork> = emptyList(),
    val isLoadingContents: Boolean = false,
    val contentsError: String? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    
    // 追更操作状态
    val isWatchLoading: Boolean = false,
    val watchError: String? = null
)
