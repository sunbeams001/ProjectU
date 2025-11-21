package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 标签建议响应体
 */
@Serializable
data class TagSuggestBody(
    val candidates: List<TagCandidate> = emptyList()
)

/**
 * 标签候选项
 */
@Serializable
data class TagCandidate(
    @SerialName("tag_name") val tagName: String,
    @SerialName("illust_count") val illustCount: Long = 0,
    @SerialName("total_count") val totalCount: Long = 0,
    @SerialName("suggest_type") val suggestType: String? = null
)

/**
 * 标签搜索建议响应体（来自 /rpc/cps.php）
 */
@Serializable
data class TagSearchSuggestBody(
    val candidates: List<TagSearchCandidate> = emptyList()
)

/**
 * 标签搜索候选项
 */
@Serializable
data class TagSearchCandidate(
    @SerialName("tag_name") val tagName: String,
    @SerialName("access_count") val accessCount: String = "0",
    val type: String,
    @SerialName("tag_translation") val tagTranslation: String? = null
)

/**
 * 标签翻译信息
 */
@Serializable
data class TagTranslation(
    val tag: String? = null,
    @SerialName("abstract") val abstract: String? = null,
    val url: String? = null
)

/**
 * 标签信息响应体
 */
@Serializable
data class TagInfoBody(
    val tag: String,
    @SerialName("abstract") val abstract: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    val en: TagTranslation? = null,
    @SerialName("en_new") val enNew: TagTranslation? = null,
    val ja: TagTranslation? = null,
    @SerialName("ja_new") val jaNew: TagTranslation? = null,
    @SerialName("is_view_lead_wire") val isViewLeadWire: Boolean = false
)

/**
 * 添加标签响应体
 */
@Serializable
data class AddTagBody(
    val success: Boolean = false,
    val message: String? = null
)

/**
 * 搜索建议响应体（点击搜索框时触发）
 */
@Serializable
data class SearchSuggestionBody(
    @SerialName("popularTags") val popularTags: PopularTags? = null,
    @SerialName("recommendTags") val recommendTags: RecommendTags? = null,
    @SerialName("recommendByTags") val recommendByTags: RecommendByTags? = null,
    @SerialName("myFavoriteTags") val myFavoriteTags: List<String> = emptyList(),
    @SerialName("tagTranslation") val tagTranslation: Map<String, TagTranslationInfo> = emptyMap(),
    val thumbnails: List<ThumbnailInfo> = emptyList()
)

/**
 * 热门标签
 */
@Serializable
data class PopularTags(
    val illust: List<PopularTag> = emptyList(),
    val novel: List<PopularTag> = emptyList()
)

/**
 * 推荐标签
 */
@Serializable
data class RecommendTags(
    val illust: List<PopularTag> = emptyList()
)

/**
 * 基于标签推荐
 */
@Serializable
data class RecommendByTags(
    val illust: List<PopularTag> = emptyList()
)

/**
 * 热门标签项
 */
@Serializable
data class PopularTag(
    val ids: List<String> = emptyList(),
    val tag: String
)

/**
 * 标签翻译信息
 */
@Serializable
data class TagTranslationInfo(
    val en: String = "",
    val ko: String = "",
    val zh: String = "",
    @SerialName("zh_tw") val zhTw: String = "",
    val romaji: String = ""
)

/**
 * 缩略图信息
 */
@Serializable
data class ThumbnailInfo(
    val id: String,
    val title: String,
    @SerialName("illustType") val illustType: Int,
    @SerialName("xRestrict") val xRestrict: Int,
    val restrict: Int,
    val sl: Int,
    val url: String,
    val description: String,
    val tags: List<String> = emptyList(),
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    val width: Int,
    val height: Int,
    @SerialName("pageCount") val pageCount: Int,
    @SerialName("isBookmarkable") val isBookmarkable: Boolean,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    val alt: String,
    @SerialName("titleCaptionTranslation") val titleCaptionTranslation: TitleCaptionTranslation? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("isUnlisted") val isUnlisted: Boolean,
    @SerialName("isMasked") val isMasked: Boolean,
    @SerialName("aiType") val aiType: Int,
    @SerialName("visibilityScope") val visibilityScope: Int,
    @SerialName("profileImageUrl") val profileImageUrl: String
)
