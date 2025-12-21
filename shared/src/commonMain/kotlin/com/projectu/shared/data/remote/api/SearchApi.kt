package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.illust.IllustSearchBody
import com.projectu.shared.data.remote.dto.novel.NovelSearchBody
import com.projectu.shared.data.remote.dto.user.UserSearchBody
import com.projectu.shared.data.remote.model.IllustSearchMode
import com.projectu.shared.data.remote.model.NovelSearchMode
import com.projectu.shared.data.remote.model.UserSearchMode
import io.ktor.http.encodeURLPathPart

/**
 * 搜索 API
 * 提供插画、小说和用户的搜索功能
 */
class SearchApi(private val client: PixivApiClient) {

    /**
     * 搜索插画+漫画+动图
     * @param keyword 关键词（需要UTF-8编码，空格替换为%20）
     * @param searchMode 搜索模式：
     *   - s_tag: 标签（部分一致）
     *   - s_tag_full: 标签（完全一致）
     *   - s_tc: 标题、说明文字
     * @param order 排序：date_d(从新到旧), date(从旧到新)
     * @param mode 模式：all, safe, r18
     * @param page 页码
     * @param aiType AI作品过滤：1(隐藏AI作品), null(显示AI作品)
     * @param scd 发布时间起始（格式：yyyy-MM-dd）
     * @param ecd 发布时间结束（格式：yyyy-MM-dd）
     */
    suspend fun searchIllust(
        keyword: String,
        searchMode: String = IllustSearchMode.DEFAULT.value,
        order: String = "date_d",
        mode: String = "all",
        page: Int = 1,
        aiType: Int? = null,
        scd: String? = null,
        ecd: String? = null
    ): PixivResponse<IllustSearchBody> {
        // URL 编码关键词（使用encodeURLPathPart以确保/等特殊字符被正确编码）
        val encodedKeyword = keyword.encodeURLPathPart()
        
        val params = mutableMapOf<String, Any?>(
            "word" to keyword,
            "s_mode" to searchMode,
            "order" to order,
            "mode" to mode,
            "p" to page
        )
        aiType?.let { params["ai_type"] = it }
        scd?.let { params["scd"] = it }
        ecd?.let { params["ecd"] = it }

        return client.get("/ajax/search/artworks/$encodedKeyword", params)
    }

    /**
     * 搜索小说
     * @param keyword 关键词（需要UTF-8编码，空格替换为%20）
     * @param searchMode 搜索模式：
     *   - s_tag_only: 标签（部分一致）
     *   - s_tag_full: 标签（完全一致）
     *   - s_tc: 正文
     *   - s_tag: 标签、标题、说明文字
     * @param order 排序：date_d(从新到旧), date(从旧到新)
     * @param mode 模式：all, safe, r18
     * @param page 页码
     * @param aiType AI作品过滤：1(隐藏AI作品), null(显示AI作品)
     * @param scd 发布时间起始（格式：yyyy-MM-dd）
     * @param ecd 发布时间结束（格式：yyyy-MM-dd）
     */
    suspend fun searchNovel(
        keyword: String,
        searchMode: String = NovelSearchMode.DEFAULT.value,
        order: String = "date_d",
        mode: String = "all",
        page: Int = 1,
        aiType: Int? = null,
        scd: String? = null,
        ecd: String? = null
    ): PixivResponse<NovelSearchBody> {
        // URL 编码关键词（使用encodeURLPathPart以确保/等特殊字符被正确编码）
        val encodedKeyword = keyword.encodeURLPathPart()
        
        val params = mutableMapOf<String, Any?>(
            "word" to keyword,
            "s_mode" to searchMode,
            "order" to order,
            "mode" to mode,
            "p" to page
        )
        aiType?.let { params["ai_type"] = it }
        scd?.let { params["scd"] = it }
        ecd?.let { params["ecd"] = it }

        return client.get("/ajax/search/novels/$encodedKeyword", params)
    }

    /**
     * 搜索用户
     * @param keyword 关键词（用户昵称）
     * @param searchMode 搜索模式：
     *   - s_usr: 部分一致
     *   - s_usr_full: 完全一致
     * @param hasWork 是否只搜索有投稿作品的用户：1(只搜索有作品的用户), 0(搜索全部用户)
     * @param page 页码
     */
    suspend fun searchUser(
        keyword: String,
        searchMode: String = UserSearchMode.DEFAULT.value,
        hasWork: Int = 1,
        page: Int = 1
    ): PixivResponse<UserSearchBody> {
        val params = mapOf(
            "nick" to keyword,
            "s_mode" to searchMode,
            "i" to hasWork,
            "p" to page
        )

        return client.get("/ajax/search/users", params)
    }
}
