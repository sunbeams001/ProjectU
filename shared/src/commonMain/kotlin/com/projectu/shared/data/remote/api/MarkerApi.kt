package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.marker.NovelMarkerBody
import com.projectu.shared.data.remote.dto.marker.NovelMarkerItem
import com.projectu.shared.data.remote.dto.marker.NovelMarkerListBody

/**
 * 小说书签 API
 * 提供小说书签（稍后再读）的添加、删除、列表查询功能
 */
class MarkerApi(private val client: PixivApiClient) {

    /**
     * 添加小说书签（稍后再读）
     * @param novelId 小说ID
     * @param userId 用户ID
     * @param page 页码（通常为1，表示添加）
     */
    suspend fun addNovelMarker(
        novelId: Long,
        userId: Long,
        page: Int = 1
    ): NovelMarkerBody {
        return client.postFormRaw(
            url = "/novel/rpc_marker.php",
            formParams = mapOf(
                "mode" to "save",
                "i_id" to novelId.toString(),
                "u_id" to userId.toString(),
                "page" to page.toString()
            )
        )
    }

    /**
     * 删除小说书签（取消稍后再读）
     * @param novelId 小说ID
     * @param userId 用户ID
     */
    suspend fun deleteNovelMarker(
        novelId: Long,
        userId: Long
    ): NovelMarkerBody {
        return client.postFormRaw(
            url = "/novel/rpc_marker.php",
            formParams = mapOf(
                "mode" to "save",
                "i_id" to novelId.toString(),
                "u_id" to userId.toString(),
                "page" to "0"
            )
        )
    }

    /**
     * 获取小说书签列表
     * @return 小说书签列表响应体
     */
    suspend fun getNovelMarkerList(): NovelMarkerListBody {
        val html = client.getHtml("/novel/marker_all.php")
        return parseNovelMarkerListHtml(html)
    }

    /**
     * 删除小说书签（通过marker_id）
     * 注意：此接口通过URL参数删除，返回HTML页面
     * @param markerId 书签ID（即小说ID）
     * @return HTML字符串
     */
    suspend fun deleteNovelMarkerById(markerId: Long): String {
        return client.getHtml(
            url = "/novel/marker_all.php",
            params = mapOf(
                "mode" to "delete",
                "marker_id" to markerId
            )
        )
    }

    /**
     * 解析小说书签列表HTML
     */
    private fun parseNovelMarkerListHtml(html: String): NovelMarkerListBody {
        // 提取总数
        val countMatch = Regex("""<span class="count-badge">(\d+)件</span>""").find(html)
        val total = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // 提取小说列表 <ul class="novel-items">...</ul>
        // 使用手动标签匹配以处理嵌套的 <ul> 标签
        val novelItemsStart = html.indexOf("""<ul class="novel-items">""")
        val novelItemsHtml = if (novelItemsStart >= 0) {
            // 从 <ul class="novel-items"> 开始查找
            val startPos = novelItemsStart + """<ul class="novel-items">""".length
            
            // 手动计数 <ul> 和 </ul> 来找到匹配的结束标签
            var ulCount = 1  // 已经遇到了第一个 <ul class="novel-items">
            var endPos = startPos
            
            while (ulCount > 0 && endPos < html.length) {
                val nextUlOpen = html.indexOf("<ul", endPos)
                val nextUlClose = html.indexOf("</ul>", endPos)
                
                when {
                    nextUlClose == -1 -> break
                    nextUlOpen == -1 || nextUlClose < nextUlOpen -> {
                        ulCount--
                        endPos = nextUlClose + 5
                    }
                    else -> {
                        ulCount++
                        endPos = nextUlOpen + 3
                    }
                }
            }
            
            if (ulCount == 0) {
                html.substring(startPos, endPos - 5)
            } else {
                ""
            }
        } else {
            ""
        }

        // 解析每个小说项
        val novels = parseNovelItems(novelItemsHtml)

        return NovelMarkerListBody(
            total = total,
            novels = novels
        )
    }

    /**
     * 解析小说项列表
     */
    private fun parseNovelItems(html: String): List<NovelMarkerItem> {
        val novels = mutableListOf<NovelMarkerItem>()
        
        // 匹配所有 <section class="_novel-item ...">...</section>
        val sectionPattern = Regex("""<section\s+class="[^"]*_novel-item[^"]*">(.*?)</section>""", RegexOption.DOT_MATCHES_ALL)
        val sections = sectionPattern.findAll(html)
        
        for (section in sections) {
            val sectionHtml = section.groupValues[1]
            
            val item = parseNovelItem(sectionHtml)
            if (item != null) {
                novels.add(item)
            }
        }

        return novels
    }

    /**
     * 解析单个小说项
     */
    private fun parseNovelItem(html: String): NovelMarkerItem? {
        try {
            // 小说ID - 从 data-id 或 href
            val idMatch = Regex("""data-id="(\d+)"""").find(html) 
                ?: Regex("""/novel/show\.php\?id=(\d+)""").find(html)
            val id = idMatch?.groupValues?.get(1) ?: return null

            // 标题 - 从 <h1 class="title"> 中的链接文本
            val titleMatch = Regex("""<h1\s+class="title[^"]*">[\s\S]*?<a[^>]*>([\s\S]*?)</a>""").find(html)
            val title = titleMatch?.groupValues?.get(1)
                ?.trim()
                ?.replace(Regex("""<.*?>"""), "")
                ?.replace(Regex("""\s+"""), " ")
                ?: "未知标题"

            // 用户ID和用户名
            val userIdMatch = Regex("""data-user_id="(\d+)"""").find(html)
            val userNameMatch = Regex("""data-user_name="([^"]*)"""").find(html)
            val userId = userIdMatch?.groupValues?.get(1) ?: "0"
            val userName = userNameMatch?.groupValues?.get(1) ?: "未知作者"

            // 封面URL
            val coverMatch = Regex("""data-src="(https://i\.pximg\.net/[^"]+)"""").find(html)
            val coverUrl = coverMatch?.groupValues?.get(1)

            // 字符数
            val charsMatch = Regex("""<div\s+class="chars">\s*([0-9,]+)\s*个字符\s*</div>""").find(html)
            val textCount = charsMatch?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

            // 收藏数
            val bookmarkMatch = Regex("""<i\s+class="_icon\s+_bookmark-icon-inline"></i>\s*(\d+)""").find(html)
            val bookmarkCount = bookmarkMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            // 标签列表
            val tags = mutableListOf<String>()
            val tagsPattern = Regex("""<a\s+href="/tags/[^"]*"\s+class="tag-value">\s*([^<]+)\s*</a>""")
            tagsPattern.findAll(html).forEach { match ->
                val tag = match.groupValues[1].trim()
                if (tag.isNotBlank()) {
                    tags.add(tag)
                }
            }

            // 描述
            val descMatch = Regex("""<p\s+class="novel-caption">([\s\S]*?)</p>""").find(html)
            val description = descMatch?.groupValues?.get(1)
                ?.trim()
                ?.replace(Regex("""\s+"""), " ")
                ?: ""

            // 系列信息
            val seriesLinkMatch = Regex("""<a[^>]*class="series-title"[^>]*href="/novel/series/(\d+)"[^>]*>([\s\S]*?)</a>""").find(html)
            val seriesId = seriesLinkMatch?.groupValues?.get(1)
            val seriesTitle = seriesLinkMatch?.groupValues?.get(2)
                ?.replace(Regex("""<.*?>"""), "")
                ?.replace(Regex("""&lt;"""), "<")
                ?.replace(Regex("""&gt;"""), ">")
                ?.replace(Regex("""&amp;"""), "&")
                ?.replace(Regex("""&quot;"""), "\"")
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()

            // R-18标识
            val xRestrict = if (tags.any { it.equals("R-18", ignoreCase = true) || it.equals("R-18G", ignoreCase = true) }) 1 else 0

            return NovelMarkerItem(
                id = id,
                title = title,
                userId = userId,
                userName = userName,
                coverUrl = coverUrl,
                textCount = textCount,
                bookmarkCount = bookmarkCount,
                tags = tags,
                description = description,
                xRestrict = xRestrict,
                seriesId = seriesId,
                seriesTitle = seriesTitle
            )
        } catch (e: Exception) {
            return null
        }
    }
}
