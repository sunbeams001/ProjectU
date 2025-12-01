package com.projectu.ui.navigation

import kotlinx.coroutines.flow.StateFlow

/**
 * 作品列表源接口
 * 
 * 实现此接口的 ViewModel 可以作为作品详情页的列表数据源，
 * 支持响应式的列表更新和加载更多功能。
 * 
 * 使用场景：
 * - 排行榜页面 (RankingViewModel)
 * - 推荐插画页面 (DiscoveryIllustsViewModel)
 * - 推荐用户页面 (DiscoveryUsersViewModel)
 * - 用户作品列表 (UserViewModel)
 * 
 * 当用户从这些页面点击作品进入详情页时，详情页可以：
 * 1. 订阅 artworkIdsFlow 获取实时的作品列表
 * 2. 在滑动到列表末尾时调用 loadMoreArtworks() 加载更多
 */
interface ArtworkListSource {
    /**
     * 作品ID列表的 StateFlow
     * 
     * 详情页会 collect 这个 Flow 来获取最新的列表。
     * 当列表变化（如加载更多后）时，详情页会自动更新。
     */
    val artworkIdsFlow: StateFlow<List<String>>
    
    /**
     * 加载更多作品
     * 
     * 当详情页滑动接近列表末尾时调用。
     * 加载完成后，artworkIdsFlow 会自动发出新值。
     */
    fun loadMoreArtworks()
}
