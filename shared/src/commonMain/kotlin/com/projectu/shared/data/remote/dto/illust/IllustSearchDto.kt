package com.projectu.shared.data.remote.dto.illust

import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.common.ZoneConfig
import kotlinx.serialization.Serializable

/**
 * 搜索结果响应体
 */
@Serializable
data class IllustSearchBody(
    val illustManga: IllustMangaData,
    val popular: PopularData? = null,
    val relatedTags: List<String>? = null,
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)

@Serializable
data class IllustMangaData(
    val data: List<IllustSimple>,
    val total: Int,
    val lastPage: Int? = null,
    val bookmarkRanges: List<BookmarkRange>? = null
)

@Serializable
data class BookmarkRange(
    val min: Int?,
    val max: Int?
)

@Serializable
data class PopularData(
    val recent: List<IllustSimple>? = null,
    val permanent: List<IllustSimple>? = null
)
