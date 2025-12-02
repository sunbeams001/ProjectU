package com.projectu.ui.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * 小说列表源接口
 * 
 * 实现此接口的 ViewModel 可以作为小说详情页的列表数据源，
 * 支持响应式的列表更新和加载更多功能。
 * 
 * 使用场景：
 * - 推荐小说页面 (DiscoveryNovelsViewModel)
 * - 小说排行榜页面
 * - 用户小说列表
 * - 小说系列页面
 * 
 * 当用户从这些页面点击小说进入详情页时，详情页可以：
 * 1. 订阅 novelIdsFlow 获取实时的小说列表
 * 2. 在滑动到列表末尾时调用 loadMoreNovels() 加载更多
 */
interface NovelListSource {
    /**
     * 小说ID列表的 StateFlow
     * 
     * 详情页会 collect 这个 Flow 来获取最新的列表。
     * 当列表变化（如加载更多后）时，详情页会自动更新。
     */
    val novelIdsFlow: StateFlow<List<String>>
    
    /**
     * 加载更多小说
     * 
     * 当详情页滑动接近列表末尾时调用。
     * 加载完成后，novelIdsFlow 会自动发出新值。
     */
    fun loadMoreNovels()
}
