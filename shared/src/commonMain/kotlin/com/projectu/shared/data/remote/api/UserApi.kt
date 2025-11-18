package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.*

/**
 * 用户 API
 * 提供用户信息查询、关注等功能
 */
class UserApi(private val client: PixivApiClient) {

    /**
     * 查询用户信息
     * @param uid 用户ID
     * @param full 是否获取完整信息（1=是，0=否）
     */
    suspend fun getUserInfo(
        uid: Long,
        full: Int = 1
    ): PixivResponse<UserInfoBody> {
        return client.get("/ajax/user/$uid", mapOf(
            "full" to full
        ))
    }

    /**
     * 查询用户作品概况
     * @param uid 用户ID
     */
    suspend fun getProfileAll(uid: Long): PixivResponse<ProfileAllBody> {
        return client.get("/ajax/user/$uid/profile/all")
    }

    /**
     * 查询用户的插画作品
     * @param uid 用户ID
     * @param ids 作品ID列表
     * @param workCategory 作品类型：illust(插画), manga(漫画), illustManga(混合)
     * @param isFirstPage 是否为第一页（1=是，0=否）
     */
    suspend fun getProfileIllusts(
        uid: Long,
        ids: List<String>,
        workCategory: String = "illustManga",
        isFirstPage: Int = 0
    ): PixivResponse<ProfileIllustsBody> {
        return client.get("/ajax/user/$uid/profile/illusts", mapOf(
            "ids[]" to ids,
            "work_category" to workCategory,
            "is_first_page" to isFirstPage
        ))
    }

    /**
     * 查询用户收藏的插画
     * @param uid 用户ID
     * @param tag 标签过滤（空字符串表示不过滤）
     * @param offset 偏移量
     * @param limit 返回数量（最大100）
     * @param rest 公开状态：show(公开), hide(私密)
     */
    suspend fun getUserBookmarkIllusts(
        uid: Long,
        tag: String = "",
        offset: Int = 0,
        limit: Int = 48,
        rest: String = "show"
    ): PixivResponse<UserBookmarkBody> {
        return client.get("/ajax/user/$uid/illusts/bookmarks", mapOf(
            "tag" to tag,
            "offset" to offset,
            "limit" to limit,
            "rest" to rest
        ))
    }

    /**
     * 获取用户关注列表
     * @param uid 用户ID
     * @param offset 偏移量
     * @param limit 返回数量（最大100）
     * @param rest 公开状态：show(公开), hide(私密)
     * @param tag 标签过滤（空字符串表示不过滤）
     * @param acceptingRequests 是否只显示正在接稿的用户（0=否，1=是）
     */
    suspend fun getUserFollowing(
        uid: Long,
        offset: Int = 0,
        limit: Int = 24,
        rest: String = "show",
        tag: String = "",
        acceptingRequests: Int = 0
    ): PixivResponse<UserFollowingBody> {
        return client.get("/ajax/user/$uid/following", mapOf(
            "offset" to offset,
            "limit" to limit,
            "rest" to rest,
            "tag" to tag,
            "acceptingRequests" to acceptingRequests
        ))
    }

    /**
     * 获取用户粉丝列表
     * @param uid 用户ID
     * @param offset 偏移量
     * @param limit 返回数量（最大100）
     */
    suspend fun getUserFollowers(
        uid: Long,
        offset: Int = 0,
        limit: Int = 24
    ): PixivResponse<UserFollowingBody> {
        return client.get("/ajax/user/$uid/followers", mapOf(
            "offset" to offset,
            "limit" to limit
        ))
    }

    /**
     * 推荐用户
     * @param uid 用户ID
     * @param userNum 推荐用户数量
     * @param workNum 每个用户附带的作品数量
     * @param isR18 是否包含R18
     */
    suspend fun getRecommendUsers(
        uid: Long,
        userNum: Int = 20,
        workNum: Int = 3,
        isR18: Boolean = false
    ): PixivResponse<UserRecommendBody> {
        return client.get("/ajax/user/$uid/recommends", mapOf(
            "userNum" to userNum,
            "workNum" to workNum,
            "isR18" to isR18
        ))
    }

    /**
     * 关注用户
     * @param userId 用户ID
     * @param tag 标签（可选）
     * @param restrict 是否公开（0=公开，1=私密）
     * @return 返回空字符串 "[]" 表示成功
     */
    suspend fun followUser(
        userId: Long,
        tag: String = "",
        restrict: Int = 0
    ): String {
        val result: kotlinx.serialization.json.JsonArray = client.postFormRaw("/bookmark_add.php", mapOf(
            "mode" to "add",
            "type" to "user",
            "user_id" to userId.toString(),
            "tag" to tag,
            "restrict" to restrict.toString(),
            "format" to "json"
        ))
        return result.toString()
    }

    /**
     * 取消关注用户
     * @param userId 用户ID
     * @return 返回包含用户ID和类型的响应对象
     */
    suspend fun unfollowUser(userId: Long): UnfollowUserResponse {
        return client.postFormRaw("/rpc_group_setting.php", mapOf(
            "mode" to "del",
            "type" to "bookuser",
            "id" to userId.toString()
        ))
    }
}

