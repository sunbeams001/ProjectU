package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.AddTagBody
import com.projectu.shared.data.remote.dto.pixiv.BookmarkData
import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import com.projectu.shared.data.remote.dto.pixiv.PopularTag
import com.projectu.shared.data.remote.dto.pixiv.PopularTags
import com.projectu.shared.data.remote.dto.pixiv.RecommendByTags
import com.projectu.shared.data.remote.dto.pixiv.RecommendTags
import com.projectu.shared.data.remote.dto.pixiv.SearchSuggestionBody
import com.projectu.shared.data.remote.dto.pixiv.TagCandidate
import com.projectu.shared.data.remote.dto.pixiv.TagInfoBody
import com.projectu.shared.data.remote.dto.pixiv.TagSearchCandidate
import com.projectu.shared.data.remote.dto.pixiv.TagSearchSuggestBody
import com.projectu.shared.data.remote.dto.pixiv.TagSuggestBody
import com.projectu.shared.data.remote.dto.pixiv.TagTranslation
import com.projectu.shared.data.remote.dto.pixiv.TagTranslationInfo
import com.projectu.shared.data.remote.dto.pixiv.ThumbnailInfo
import com.projectu.shared.data.remote.dto.pixiv.TitleCaptionTranslation

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
     * 获取搜索建议（点击搜索框时触发，无需输入关键字）
     * @param mode 模式（all: 全部作品, r18: R18作品）
     */
    suspend fun getSearchSuggestion(
        mode: String = "all"
    ): PixivResponse<SearchSuggestionBody> {
        return client.get("/ajax/search/suggestion", mapOf(
            "mode" to mode
        ))
    }

    /**
     * 标签搜索建议（用于搜索时的标签提示）
     * @param keyword 关键字
     */
    suspend fun getSearchSuggest(
        keyword: String
    ): TagSearchSuggestBody {
        return client.getRaw("/rpc/cps.php", mapOf(
            "keyword" to keyword
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
