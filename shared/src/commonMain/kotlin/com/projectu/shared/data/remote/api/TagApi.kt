package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.tag.AddTagBody
import com.projectu.shared.data.remote.dto.tag.PopularTag
import com.projectu.shared.data.remote.dto.tag.PopularTags
import com.projectu.shared.data.remote.dto.tag.RecommendByTags
import com.projectu.shared.data.remote.dto.tag.RecommendTags
import com.projectu.shared.data.remote.dto.tag.SearchSuggestionBody
import com.projectu.shared.data.remote.dto.tag.TagCandidate
import com.projectu.shared.data.remote.dto.tag.TagInfoBody
import com.projectu.shared.data.remote.dto.tag.TagSearchCandidate
import com.projectu.shared.data.remote.dto.tag.TagSearchSuggestBody
import com.projectu.shared.data.remote.dto.tag.TagSuggestBody
import com.projectu.shared.data.remote.dto.tag.TagTranslation
import com.projectu.shared.data.remote.dto.tag.TagTranslationInfo
import com.projectu.shared.data.remote.dto.tag.ThumbnailInfo

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
     * 获取搜索框推荐内容（点击搜索框时触发）
     * 返回热门标签、推荐标签、收藏标签、缩略图等
     * @param mode 模式（all: 全部作品, r18: R18作品）
     */
    suspend fun getSearchRecommendations(
        mode: String = "all"
    ): PixivResponse<SearchSuggestionBody> {
        return client.get("/ajax/search/suggestion", mapOf(
            "mode" to mode
        ))
    }

    /**
     * 搜索标签自动补全（输入时实时调用）
     * @param keyword 关键字
     */
    suspend fun searchTagAutocomplete(
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
