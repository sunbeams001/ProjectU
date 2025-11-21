package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.BookmarkRange
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说搜索响应体
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
 */
@Serializable
data class NovelSearchData(
    val data: List<NovelSearchItem> = emptyList(),
    val total: Int = 0,
    val lastPage: Int = 0,
    val bookmarkRanges: List<BookmarkRange> = emptyList()
)

/**
 * 小说搜索项
 */
@Serializable
data class NovelSearchItem(
    val id: String,
    val title: String,
    val genre: String,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String,
    val tags: List<String> = emptyList(),
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String,
    @SerialName("textCount") val textCount: Int = 0,
    @SerialName("wordCount") val wordCount: Int = 0,
    @SerialName("readingTime") val readingTime: Int = 0,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    val description: String = "",
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    val marker: Int? = null,
    @SerialName("titleCaptionTranslation") val titleCaptionTranslation: TitleCaptionTranslation? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("isMasked") val isMasked: Boolean = false,
    @SerialName("aiType") val aiType: Int = 0,
    @SerialName("seriesId") val seriesId: String? = null,
    @SerialName("seriesTitle") val seriesTitle: String? = null,
    @SerialName("isUnlisted") val isUnlisted: Boolean = false,
    @SerialName("visibilityScope") val visibilityScope: Int = 0,
    val language: String = "ja"
)
