package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.follow.WatchListMangaBody
import com.projectu.shared.data.remote.dto.follow.WatchListNovelBody
import com.projectu.shared.data.remote.dto.illust.FollowLatestBody

/**
 * 关注相关 API
 * 提供已关注用户的最新作品、追更等功能
 */
class FollowApi(private val client: PixivApiClient) {

    /**
     * 查询关注作者的最新插画
     * @param mode 模式：all, r18
     * @param page 页码
     */
    suspend fun getFollowLatestIllust(
        mode: String = "all",
        page: Int = 1
    ): PixivResponse<FollowLatestBody> {
        return client.get("/ajax/follow_latest/illust", mapOf(
            "mode" to mode,
            "p" to page
        ))
    }

    /**
     * 查询关注作者的最新小说
     * @param mode 模式：all, r18
     * @param page 页码
     */
    suspend fun getFollowLatestNovel(
        mode: String = "all",
        page: Int = 1
    ): PixivResponse<FollowLatestBody> {
        return client.get("/ajax/follow_latest/novel", mapOf(
            "mode" to mode,
            "p" to page
        ))
    }

    /**
     * 查询漫画追更列表
     * 获取已追更的漫画系列列表
     * @param page 页码
     */
    suspend fun getWatchListManga(
        page: Int = 1
    ): PixivResponse<WatchListMangaBody> {
        return client.get("/ajax/watch_list/manga", mapOf(
            "p" to page,
            "new" to "1"
        ))
    }

    /**
     * 查询小说追更列表
     * 获取已追更的小说系列列表
     * @param page 页码
     */
    suspend fun getWatchListNovel(
        page: Int = 1
    ): PixivResponse<WatchListNovelBody> {
        return client.get("/ajax/watch_list/novel", mapOf(
            "p" to page,
            "new" to "1"
        ))
    }
}
