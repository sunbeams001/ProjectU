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
    @SerialName("access_count") val accessCount: Long? = null,
    @SerialName("type") val type: String? = null
)

/**
 * 标签信息响应体
 */
@Serializable
data class TagInfoBody(
    val tag: String,
    @SerialName("abstract") val abstract: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("isLocked") val isLocked: Boolean = false,
    @SerialName("deletable") val deletable: Boolean = false,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("translation") val translation: Map<String, String>? = null
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

    /**
     * 获取热门标签
     * @param mode 模式：all, safe, r18
     */
    suspend fun getPopularTags(
        mode: String = "all"
    ): PixivResponse<TagSuggestBody> {
        return client.get("/ajax/tags/popular", mapOf(
            "mode" to mode
        ))
    }
}
