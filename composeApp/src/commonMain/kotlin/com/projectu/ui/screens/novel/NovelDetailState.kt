package com.projectu.ui.screens.novel

import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.ui.util.NovelContentParser

/**
 * 小说详情页状态
 * 
 * @param novel 当前展示的小说
 * @param authorFollowStatus 作者关注状态
 * @param isLoading 是否正在加载
 * @param error 错误信息
 * @param parsedPages 解析后的页面列表
 * @param currentPage 当前阅读页码（从1开始）
 * @param isInfoExpanded 信息区域是否展开
 * @param novelIds 小说ID列表（列表导航模式）
 * @param currentIndex 当前小说在列表中的索引
 * @param novelCache 已加载的小说缓存（novelId -> Novel）
 * @param currentNovelId 当前正在加载/展示的小说ID（用于重试）
 * @param isMarkerLoading 书签操作是否正在加载
 */
data class NovelDetailState(
    val novel: Novel? = null,
    val authorFollowStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    val isLoading: Boolean = false,
    val error: String? = null,
    val parsedPages: List<NovelContentParser.NovelPage> = emptyList(),
    val currentPage: Int = 1,
    val isInfoExpanded: Boolean = false,
    val novelIds: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val novelCache: Map<String, Novel> = emptyMap(),
    val currentNovelId: String? = null,
    val isMarkerLoading: Boolean = false
) {
    /**
     * 总页数
     */
    val totalPages: Int
        get() = parsedPages.size.coerceAtLeast(1)
    
    /**
     * 是否有多页
     */
    val hasMultiplePages: Boolean
        get() = totalPages > 1
    
    /**
     * 是否可以翻到上一页
     */
    val canGoPrevious: Boolean
        get() = currentPage > 1
    
    /**
     * 是否可以翻到下一页
     */
    val canGoNext: Boolean
        get() = currentPage < totalPages
    
    /**
     * 当前页的内容
     */
    val currentPageContent: NovelContentParser.NovelPage?
        get() = parsedPages.getOrNull(currentPage - 1)
}
