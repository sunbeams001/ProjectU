package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
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
 * 标签 API
 * 提供标签搜索建议、标签信息查询、添加标签等功能
 */
class TagApi(private val client: PixivApiClient) {

    /**
     * 查询标签建议（添加标签或搜索时使用）
     * @param keyword 关键字
     */
    suspend fun getSuggestByWord(keyword: String): PixivResponse<TagSuggestBody> {
        return client.get("/ajax/tags/suggest_by_word", mapOf(
            "word" to keyword
        ))
    }

    /**
     * 查询标签信息
     * @param tag 标签名称
     * @param lang 语言（可选）
     */
    suspend fun getTagInfo(
        tag: String,
        lang: String? = null
    ): PixivResponse<TagInfoBody> {
        val params = mutableMapOf<String, Any?>("tag" to tag)
        lang?.let { params["lang"] = it }
        
        return client.get("/ajax/tag/info", params)
    }

    /**
     * 为插画添加标签
     * @param illustId 作品ID
     * @param tag 标签名称
     */
    suspend fun addIllustTag(
        illustId: Long,
        tag: String
    ): PixivResponse<AddTagBody> {
        return client.postJson("/ajax/tags/illust/$illustId/add", mapOf(
            "tag" to tag
        ))
    }

    /**
     * 删除插画标签
     * @param illustId 作品ID
     * @param tag 标签名称
     */
    suspend fun deleteIllustTag(
        illustId: Long,
        tag: String
    ): PixivResponse<AddTagBody> {
        return client.postJson("/ajax/tags/illust/$illustId/delete", mapOf(
            "tag" to tag
        ))
    }

    /**
     * 为小说添加标签
     * @param novelId 小说ID
     * @param tag 标签名称
     */
    suspend fun addNovelTag(
        novelId: Long,
        tag: String
    ): PixivResponse<AddTagBody> {
        return client.postJson("/ajax/tags/novel/$novelId/add", mapOf(
            "tag" to tag
        ))
    }

    /**
     * 删除小说标签
     * @param novelId 小说ID
     * @param tag 标签名称
     */
    suspend fun deleteNovelTag(
        novelId: Long,
        tag: String
    ): PixivResponse<AddTagBody> {
        return client.postJson("/ajax/tags/novel/$novelId/delete", mapOf(
            "tag" to tag
        ))
    }
}
