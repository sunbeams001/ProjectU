package com.projectu.shared.data.remote.dto.bookmark

import com.projectu.shared.data.remote.dto.common.ZoneConfig
import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.dto.novel.NovelSimple
import com.projectu.shared.data.remote.serializers.MapStringListOrEmptyArraySerializer
import kotlinx.serialization.Serializable

/**
 * 用户收藏的插画·漫画响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/illusts/bookmarks
 */
@Serializable
data class UserBookmarkIllustsBody(
    val works: List<IllustSimple>,
    val total: Int,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null,
    @Serializable(with = MapStringListOrEmptyArraySerializer::class)
    val bookmarkTags: Map<String, List<String>>? = null
)

/**
 * 用户收藏的小说响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/novels/bookmarks
 * 
 * 响应示例:
 * {
 *   "error": false,
 *   "message": "",
 *   "body": {
 *     "works": [...],
 *     "total": 3,
 *     "zoneConfig": {...},
 *     "extraData": {...},
 *     "bookmarkTags": {...}
 *   }
 * }
 */
@Serializable
data class UserBookmarkNovelsBody(
    val works: List<NovelSimple>,
    val total: Int,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null,
    @Serializable(with = MapStringListOrEmptyArraySerializer::class)
    val bookmarkTags: Map<String, List<String>>? = null
)
