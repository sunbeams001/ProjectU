package com.projectu.ui.screens.novel

import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.ui.util.NovelContentParser

/**
 * 书签状态枚举
 * 
 * 用于区分书签的三种状态，提供更清晰的用户交互体验
 */
enum class MarkerStatus {
    /** 未添加书签 */
    NO_MARKER,
    /** 已添加书签且是当前页 */
    MARKER_CURRENT_PAGE,
    /** 已添加书签但不是当前页 */
    MARKER_OTHER_PAGE
}

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
    
    /**
     * 书签状态
     * 
     * 根据当前页码和已保存的书签页码计算：
     * - NO_MARKER: 未添加书签
     * - MARKER_CURRENT_PAGE: 已添加书签且是当前页
     * - MARKER_OTHER_PAGE: 已添加书签但不是当前页（需要更新）
     */
    val markerStatus: MarkerStatus
        get() {
            val markerPage = novel?.marker ?: return MarkerStatus.NO_MARKER
            return if (markerPage == currentPage) {
                MarkerStatus.MARKER_CURRENT_PAGE
            } else {
                MarkerStatus.MARKER_OTHER_PAGE
            }
        }
}
