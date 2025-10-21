package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse

/**
 * 收藏 API
 * 提供作品收藏、取消收藏等功能
 */
class BookmarkApi(private val client: PixivApiClient) {

    /**
     * 收藏插画
     * @param illustId 作品ID
     * @param restrict 是否私密（0=公开，1=私密）
     * @param comment 备注
     * @param tags 标签列表
     */
    suspend fun addIllust(
        illustId: Long,
        restrict: Int = 0,
        comment: String = "",
        tags: List<String> = emptyList()
    ): PixivResponse<Map<String, Any>> {
        return client.postJson("/ajax/illusts/bookmarks/add", mapOf(
            "illust_id" to illustId,
            "restrict" to restrict,
            "comment" to comment,
            "tags" to tags
        ))
    }

    /**
     * 删除插画收藏
     * @param bookmarkId 收藏ID（从作品的bookmarkData字段获取）
     */
    suspend fun deleteIllust(bookmarkId: String): PixivResponse<Unit> {
        return client.postForm("/ajax/illusts/bookmarks/delete", mapOf(
            "bookmark_id" to bookmarkId
        ))
    }

    /**
     * 批量删除插画收藏
     * @param bookmarkIds 收藏ID列表
     */
    suspend fun deleteIllusts(bookmarkIds: List<String>): PixivResponse<Unit> {
        return client.postJson("/ajax/illusts/bookmarks/remove", mapOf(
            "bookmarkIds" to bookmarkIds
        ))
    }

    /**
     * 收藏小说
     * @param novelId 小说ID
     * @param restrict 是否私密（0=公开，1=私密）
     * @param comment 备注
     * @param tags 标签列表
     */
    suspend fun addNovel(
        novelId: Long,
        restrict: Int = 0,
        comment: String = "",
        tags: List<String> = emptyList()
    ): PixivResponse<Map<String, Any>> {
        return client.postJson("/ajax/novels/bookmarks/add", mapOf(
            "novel_id" to novelId,
            "restrict" to restrict,
            "comment" to comment,
            "tags" to tags
        ))
    }

    /**
     * 删除小说收藏
     * @param bookId 收藏ID
     */
    suspend fun deleteNovel(bookId: String): PixivResponse<Unit> {
        return client.postForm("/ajax/novels/bookmarks/delete", mapOf(
            "book_id" to bookId,
            "del" to "1"
        ))
    }

    /**
     * 批量删除小说收藏
     * @param bookmarkIds 收藏ID列表
     */
    suspend fun deleteNovels(bookmarkIds: List<String>): PixivResponse<Unit> {
        return client.postJson("/ajax/novels/bookmarks/remove", mapOf(
            "bookmarkIds" to bookmarkIds
        ))
    }
}

