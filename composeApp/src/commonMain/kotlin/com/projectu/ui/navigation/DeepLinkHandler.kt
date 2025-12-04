package com.projectu.ui.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen

/**
 * 深度链接处理器
 * 
 * 负责将解析后的 [DeepLinkTarget] 转换为对应的 Screen 并执行导航
 */
object DeepLinkHandler {
    
    /**
     * 将 [DeepLinkTarget] 转换为对应的 [Screen]
     * 
     * @param target 解析后的深度链接目标
     * @return 对应的 Screen，如果是 Unknown 则返回 null
     */
    fun targetToScreen(target: DeepLinkTarget): Screen? {
        return when (target) {
            is DeepLinkTarget.User -> UserScreen(target.userId)
            is DeepLinkTarget.Artwork -> ArtworkDetailScreen(artworkId = target.artworkId)
            is DeepLinkTarget.Novel -> NovelDetailScreen(novelId = target.novelId)
            is DeepLinkTarget.NovelSeries -> NovelSeriesScreen(seriesId = target.seriesId)
            is DeepLinkTarget.MangaSeries -> MangaSeriesScreen(seriesId = target.seriesId)
            DeepLinkTarget.Unknown -> null
        }
    }
    
    /**
     * 处理深度链接 URL 并执行导航
     * 
     * @param url 完整的 URL 字符串
     * @param navigator Voyager Navigator
     * @return 是否成功处理了深度链接
     */
    fun handleUrl(url: String?, navigator: Navigator): Boolean {
        val target = DeepLinkParser.parse(url)
        return handleTarget(target, navigator)
    }
    
    /**
     * 处理深度链接 URI 组件并执行导航
     * 
     * @param host 主机名
     * @param path 路径
     * @param query 查询参数
     * @param navigator Voyager Navigator
     * @return 是否成功处理了深度链接
     */
    fun handleUri(host: String?, path: String?, query: String?, navigator: Navigator): Boolean {
        val target = DeepLinkParser.parseUri(host, path, query)
        return handleTarget(target, navigator)
    }
    
    /**
     * 处理解析后的深度链接目标并执行导航
     * 
     * @param target 解析后的深度链接目标
     * @param navigator Voyager Navigator
     * @return 是否成功处理了深度链接
     */
    fun handleTarget(target: DeepLinkTarget, navigator: Navigator): Boolean {
        val screen = targetToScreen(target)
        return if (screen != null) {
            navigator.push(screen)
            true
        } else {
            false
        }
    }
}
