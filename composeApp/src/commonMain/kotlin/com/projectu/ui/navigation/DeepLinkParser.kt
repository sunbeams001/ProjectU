package com.projectu.ui.navigation

/**
 * 深度链接解析结果
 */
sealed class DeepLinkTarget {
    /**
     * 用户页面
     * @param userId 用户ID
     */
    data class User(val userId: String) : DeepLinkTarget()
    
    /**
     * 作品详情页面（插画、漫画、动图）
     * @param artworkId 作品ID
     */
    data class Artwork(val artworkId: String) : DeepLinkTarget()
    
    /**
     * 小说详情页面
     * @param novelId 小说ID
     */
    data class Novel(val novelId: String) : DeepLinkTarget()
    
    /**
     * 小说系列页面
     * @param seriesId 系列ID
     */
    data class NovelSeries(val seriesId: String) : DeepLinkTarget()
    
    /**
     * 漫画系列页面
     * @param seriesId 系列ID
     */
    data class MangaSeries(val seriesId: String) : DeepLinkTarget()
    
    /**
     * 未知或不支持的链接
     */
    data object Unknown : DeepLinkTarget()
}

/**
 * Pixiv 深度链接解析器
 * 
 * 支持的链接格式：
 * 
 * 用户：
 * - https://www.pixiv.net/users/4966721
 * - https://www.pixiv.net/en/users/4966721
 * 
 * 插画/漫画/动图：
 * - https://www.pixiv.net/artworks/101020338
 * - https://www.pixiv.net/en/artworks/101020338
 * 
 * 小说：
 * - https://www.pixiv.net/novel/show.php?id=26587667
 * 
 * 小说系列：
 * - https://www.pixiv.net/novel/series/11604214
 * 
 * 漫画系列：
 * - https://www.pixiv.net/user/3414789/series/313864
 */
object DeepLinkParser {
    
    // 用户页面正则：/users/{userId} 或 /en/users/{userId}
    private val USER_PATTERN = Regex("""^/(?:en/)?users/(\d+)/?$""")
    
    // 作品详情页正则：/artworks/{artworkId} 或 /en/artworks/{artworkId}
    private val ARTWORK_PATTERN = Regex("""^/(?:en/)?artworks/(\d+)/?$""")
    
    // 小说详情页正则：/novel/show.php?id={novelId}
    private val NOVEL_PATTERN = Regex("""^/novel/show\.php$""")
    
    // 小说系列正则：/novel/series/{seriesId}
    private val NOVEL_SERIES_PATTERN = Regex("""^/novel/series/(\d+)/?$""")
    
    // 漫画系列正则：/user/{userId}/series/{seriesId}
    private val MANGA_SERIES_PATTERN = Regex("""^/user/\d+/series/(\d+)/?$""")
    
    /**
     * 解析深度链接 URL
     * 
     * @param url 完整的 URL 字符串
     * @return 解析结果 [DeepLinkTarget]
     */
    fun parse(url: String?): DeepLinkTarget {
        if (url.isNullOrBlank()) return DeepLinkTarget.Unknown
        
        return try {
            // 简单解析 URL
            val urlWithoutProtocol = url.removePrefix("https://").removePrefix("http://")
            val hostAndPath = urlWithoutProtocol.split("?", limit = 2)
            val pathPart = hostAndPath[0]
            val queryPart = if (hostAndPath.size > 1) hostAndPath[1] else null
            
            // 提取 host 和 path
            val pathStartIndex = pathPart.indexOf('/')
            if (pathStartIndex == -1) return DeepLinkTarget.Unknown
            
            val host = pathPart.substring(0, pathStartIndex)
            val path = pathPart.substring(pathStartIndex)
            
            // 验证 host
            if (!isValidPixivHost(host)) return DeepLinkTarget.Unknown
            
            // 解析 path
            parsePathAndQuery(path, queryPart)
        } catch (e: Exception) {
            DeepLinkTarget.Unknown
        }
    }
    
    /**
     * 解析 URI 组件（适用于 Android Intent URI 解析）
     * 
     * @param host 主机名
     * @param path 路径
     * @param query 查询参数
     * @return 解析结果 [DeepLinkTarget]
     */
    fun parseUri(host: String?, path: String?, query: String?): DeepLinkTarget {
        if (host == null || path == null) return DeepLinkTarget.Unknown
        
        if (!isValidPixivHost(host)) return DeepLinkTarget.Unknown
        
        return parsePathAndQuery(path, query)
    }
    
    /**
     * 验证是否为有效的 Pixiv 主机
     */
    private fun isValidPixivHost(host: String): Boolean {
        return host == "www.pixiv.net" || host == "pixiv.net"
    }
    
    /**
     * 解析路径和查询参数
     */
    private fun parsePathAndQuery(path: String, query: String?): DeepLinkTarget {
        // 尝试匹配用户页面
        USER_PATTERN.find(path)?.let { matchResult ->
            val userId = matchResult.groupValues[1]
            return DeepLinkTarget.User(userId)
        }
        
        // 尝试匹配作品详情页
        ARTWORK_PATTERN.find(path)?.let { matchResult ->
            val artworkId = matchResult.groupValues[1]
            return DeepLinkTarget.Artwork(artworkId)
        }
        
        // 尝试匹配小说详情页
        if (NOVEL_PATTERN.matches(path)) {
            // 从查询参数中提取 id
            val novelId = parseQueryParameter(query, "id")
            if (novelId != null) {
                return DeepLinkTarget.Novel(novelId)
            }
        }
        
        // 尝试匹配小说系列
        NOVEL_SERIES_PATTERN.find(path)?.let { matchResult ->
            val seriesId = matchResult.groupValues[1]
            return DeepLinkTarget.NovelSeries(seriesId)
        }
        
        // 尝试匹配漫画系列
        MANGA_SERIES_PATTERN.find(path)?.let { matchResult ->
            val seriesId = matchResult.groupValues[1]
            return DeepLinkTarget.MangaSeries(seriesId)
        }
        
        return DeepLinkTarget.Unknown
    }
    
    /**
     * 解析查询参数
     */
    private fun parseQueryParameter(query: String?, key: String): String? {
        if (query.isNullOrBlank()) return null
        
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .find { it.size == 2 && it[0] == key }
            ?.get(1)
    }
}
