package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkRange
import kotlinx.serialization.Serializable

/**
 * 小说搜索响应体
 * 
 * 对应接口：/ajax/search/novels/{keyword}
 */
@Serializable
data class NovelSearchBody(
    val novel: NovelSearchData,
    val relatedTags: List<String> = emptyList(),
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,  // 简单的两层嵌套
    val zoneConfig: kotlinx.serialization.json.JsonElement? = null,  // 复杂嵌套，使用JsonElement
    val extraData: kotlinx.serialization.json.JsonElement? = null  // 复杂嵌套，使用JsonElement
)

/**
 * 小说搜索数据
 * 
 * 包含搜索结果列表、总数、分页等信息
 */
@Serializable
data class NovelSearchData(
    val data: List<NovelSimple> = emptyList(),
    val total: Int = 0,
    val lastPage: Int = 0,
    val bookmarkRanges: List<BookmarkRange> = emptyList()
)
