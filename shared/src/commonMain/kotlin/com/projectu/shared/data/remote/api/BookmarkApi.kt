package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.EmptyArrayResponse
import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 添加作品收藏请求体
 */
@Serializable
data class BookmarkRequest(
    @SerialName("illust_id") val illustId: String,
    val restrict: Int,
    val comment: String = "",
    val tags: List<String> = emptyList()
)

/**
 * 添加小说收藏请求体
 */
@Serializable
data class NovelBookmarkRequest(
    @SerialName("novel_id") val novelId: String,
    val restrict: Int,
    val comment: String = "",
    val tags: List<String> = emptyList()
)

/**
 * 添加收藏响应
 */
@Serializable
data class BookmarkAddResponse(
    @SerialName("last_bookmark_id") val lastBookmarkId: String? = null,
    @SerialName("stacc_status_id") val staccStatusId: String? = null
)

/**
 * 收藏标签
 */
@Serializable
data class BookmarkTag(
    val tag: String,
    val cnt: Int
)

/**
 * 收藏标签响应
 */
@Serializable
data class BookmarkTagsResponse(
    val public: List<BookmarkTag> = emptyList(),
    val private: List<BookmarkTag> = emptyList(),
    @SerialName("tooManyBookmark") val tooManyBookmark: Boolean = false,
    @SerialName("tooManyBookmarkTags") val tooManyBookmarkTags: Boolean = false
)

/**
 * 收藏 API
 * 提供作品收藏、取消收藏等功能
 */
class BookmarkApi(private val client: PixivApiClient) {

    // ==================== 插画收藏 ====================

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
    ): PixivResponse<BookmarkAddResponse> {
        val requestBody = BookmarkRequest(illustId.toString(), restrict, comment, tags)
        return client.postJson("/ajax/illusts/bookmarks/add", requestBody)
    }

    /**
     * 删除插画收藏
     * @param bookmarkId 收藏ID（从作品的bookmarkData字段获取）
     */
    suspend fun deleteIllust(bookmarkId: String): EmptyArrayResponse {
        return client.postForm("/ajax/illusts/bookmarks/delete", mapOf(
            "bookmark_id" to bookmarkId
        ))
    }

    /**
     * 批量删除插画收藏
     * @param bookmarkIds 收藏ID列表
     */
    suspend fun deleteIllusts(bookmarkIds: List<String>): EmptyArrayResponse {
        return client.postJson("/ajax/illusts/bookmarks/remove", mapOf(
            "bookmarkIds" to bookmarkIds
        ))
    }

    /**
     * 获取用户的插画收藏标签
     * @param userId 用户ID
     */
    suspend fun getIllustBookmarkTags(userId: Long): PixivResponse<BookmarkTagsResponse> {
        return client.get("/ajax/user/$userId/illusts/bookmark/tags")
    }

    // ==================== 小说收藏 ====================

    /**
     * 收藏小说
     * @param novelId 小说ID
     * @param restrict 是否私密（0=公开，1=私密）
     * @param comment 备注
     * @param tags 标签列表
     * @return 收藏ID（字符串）
     */
    suspend fun addNovel(
        novelId: Long,
        restrict: Int = 0,
        comment: String = "",
        tags: List<String> = emptyList()
    ): PixivResponse<String> {
        val requestBody = NovelBookmarkRequest(novelId.toString(), restrict, comment, tags)
        return client.postJson("/ajax/novels/bookmarks/add", requestBody)
    }

    /**
     * 删除小说收藏
     * @param bookId 收藏ID
     */
    suspend fun deleteNovel(bookId: String): EmptyArrayResponse {
        return client.postForm("/ajax/novels/bookmarks/delete", mapOf(
            "book_id" to bookId,
            "del" to "1"
        ))
    }

    /**
     * 批量删除小说收藏
     * @param bookmarkIds 收藏ID列表
     */
    suspend fun deleteNovels(bookmarkIds: List<String>): EmptyArrayResponse {
        return client.postJson("/ajax/novels/bookmarks/remove", mapOf(
            "bookmarkIds" to bookmarkIds
        ))
    }

    /**
     * 获取用户的小说收藏标签
     * @param userId 用户ID
     */
    suspend fun getNovelBookmarkTags(userId: Long): PixivResponse<BookmarkTagsResponse> {
        return client.get("/ajax/user/$userId/novels/bookmark/tags")
    }
}

