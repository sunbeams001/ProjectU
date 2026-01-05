package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.user.DiscoveryUsersBody
import com.projectu.shared.data.remote.dto.user.MyPixivBody
import com.projectu.shared.data.remote.dto.user.ProfileAllBody
import com.projectu.shared.data.remote.dto.user.ProfileIllustsBody
import com.projectu.shared.data.remote.dto.user.ProfileNovelsBody
import com.projectu.shared.data.remote.dto.user.UnfollowUserResponse
import com.projectu.shared.data.remote.dto.user.UserFollowDetailBody
import com.projectu.shared.data.remote.dto.user.UserFollowingBody
import com.projectu.shared.data.remote.dto.user.UserIllustsByTagBody
import com.projectu.shared.data.remote.dto.user.UserIllustTag
import com.projectu.shared.data.remote.dto.user.UserInfoBody
import com.projectu.shared.data.remote.dto.user.UserNovelsByTagBody
import com.projectu.shared.data.remote.dto.user.UserRecommendBody

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
     * 查询用户的小说作品
     * 
     * @param uid 用户ID
     * @param ids 小说ID列表（从 getProfileAll 接口获取）
     * 
     * 请求示例：
     * GET /ajax/user/18662946/profile/novels?ids[]=26469344&ids[]=26469328&...&lang=zh
     * 
     * 使用流程：
     * 1. 先调用 getProfileAll 获取用户的所有小说ID列表
     * 2. 再调用本接口获取小说详细信息
     * 
     * @return 包含小说详细信息的响应体，works 为 Map<小说ID, NovelSimple>
     */
    suspend fun getProfileNovels(
        uid: Long,
        ids: List<String>
    ): PixivResponse<ProfileNovelsBody> {
        return client.get("/ajax/user/$uid/profile/novels", mapOf(
            "ids[]" to ids
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
     * 推荐用户（针对特定用户）
     * 根据指定用户推荐相似用户
     * @param uid 用户ID
     * @param userNum 推荐用户数量
     * @param workNum 每个用户附带的作品数量
     * @param isR18 是否包含R18
     */
    suspend fun getRecommendUsers(
        uid: Long,
        userNum: Int = 20,
        workNum: Int = 3,
        isR18: Boolean = true
    ): PixivResponse<UserRecommendBody> {
        return client.get("/ajax/user/$uid/recommends", mapOf(
            "userNum" to userNum,
            "workNum" to workNum,
            "isR18" to isR18
        ))
    }

    /**
     * 发现用户（总体推荐）
     * 获取推荐给当前登录账户的用户，不针对特定用户
     * @param limit 返回数量（默认20）
     */
    suspend fun getDiscoveryUsers(
        limit: Int = 20
    ): PixivResponse<DiscoveryUsersBody> {
        return client.get("/ajax/discovery/users", mapOf(
            "limit" to limit
        ))
    }

    /**
     * 获取用户关注详情
     * 
     * 查询指定用户的关注状态（公开/悄悄关注）
     * 
     * ⚠️ 重要：此接口只能查询自己关注的用户，未关注的用户会返回错误
     * 
     * @param userId 用户ID
     * @return 关注详情，包含restrict字段：
     *         - "0" = 公开关注
     *         - "1" = 悄悄关注（私密）
     * 
     * 应用场景：
     * - 精确同步全局状态缓存中的用户关注状态
     * - 在Discovery列表中区分公开/悄悄关注
     * - 纠正Pixiv官方接口返回的不完整状态信息
     */
    suspend fun getUserFollowDetail(
        userId: Long
    ): PixivResponse<UserFollowDetailBody> {
        return client.get("/ajax/following/user/details", mapOf(
            "user_id" to userId
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

    /**
     * 获取好P友列表（MyPixiv）
     * 
     * 获取用户的好P友（互相关注的好友）列表
     * 
     * @param uid 用户ID
     * @param offset 偏移量
     * @param limit 返回数量（最大24）
     * @return 好P友列表响应体
     * 
     * 接口地址: GET /ajax/user/{userId}/mypixiv
     * 示例: https://www.pixiv.net/ajax/user/4966721/mypixiv?offset=0&limit=24&lang=zh
     */
    suspend fun getMyPixiv(
        uid: Long,
        offset: Int = 0,
        limit: Int = 24
    ): PixivResponse<MyPixivBody> {
        return client.get("/ajax/user/$uid/mypixiv", mapOf(
            "offset" to offset,
            "limit" to limit
        ))
    }

    /**
     * 获取用户插画的全部标签
     * 
     * 获取指定用户所有插画作品的标签列表，包括标签名称、翻译和作品数量
     * 
     * @param uid 用户ID
     * @param all 获取全部标签（固定为1）
     * @return 标签列表响应体
     * 
     * 接口地址: GET /ajax/user/{userId}/illusts/tags?all=1&lang=zh
     * 示例: https://www.pixiv.net/ajax/user/757415/illusts/tags?all=1&lang=zh
     */
    suspend fun getUserIllustTags(
        uid: Long,
        all: Int = 1
    ): PixivResponse<List<UserIllustTag>> {
        return client.get("/ajax/user/$uid/illusts/tags", mapOf(
            "all" to all
        ))
    }

    /**
     * 获取用户指定标签的插画作品
     * 
     * 根据标签筛选获取用户的插画作品列表
     * 
     * @param uid 用户ID
     * @param tag 标签名称（需URL编码）
     * @param offset 偏移量
     * @param limit 返回数量（最大48）
     * @param sensitiveFilterMode 敏感内容过滤模式，默认 "userSetting"
     * @return 作品列表响应体
     * 
     * 接口地址: GET /ajax/user/{userId}/illusts/tag?tag={tag}&offset={offset}&limit={limit}&sensitiveFilterMode=userSetting&lang=zh
     * 示例: https://www.pixiv.net/ajax/user/16208053/illusts/tag?tag=女の子&offset=0&limit=48&sensitiveFilterMode=userSetting&lang=zh
     */
    suspend fun getUserIllustsByTag(
        uid: Long,
        tag: String,
        offset: Int = 0,
        limit: Int = 48,
        sensitiveFilterMode: String = "userSetting"
    ): PixivResponse<UserIllustsByTagBody> {
        return client.get("/ajax/user/$uid/illusts/tag", mapOf(
            "tag" to tag,
            "offset" to offset,
            "limit" to limit,
            "sensitiveFilterMode" to sensitiveFilterMode
        ))
    }

    /**
     * 获取用户小说的全部标签
     * 
     * 获取指定用户所有小说作品的标签列表，包括标签名称、翻译和作品数量
     * 
     * @param uid 用户ID
     * @param all 获取全部标签（固定为1）
     * @return 标签列表响应体
     * 
     * 接口地址: GET /ajax/user/{userId}/novels/tags?all=1&lang=zh
     * 示例: https://www.pixiv.net/ajax/user/16208053/novels/tags?all=1&lang=zh
     */
    suspend fun getUserNovelTags(
        uid: Long,
        all: Int = 1
    ): PixivResponse<List<UserIllustTag>> {
        return client.get("/ajax/user/$uid/novels/tags", mapOf(
            "all" to all
        ))
    }

    /**
     * 获取用户指定标签的小说作品
     * 
     * 根据标签筛选获取用户的小说作品列表
     * 
     * @param uid 用户ID
     * @param tag 标签名称（需URL编码）
     * @param offset 偏移量
     * @param limit 返回数量（最大30）
     * @return 作品列表响应体
     * 
     * 接口地址: GET /ajax/user/{userId}/novels/tag?tag={tag}&offset={offset}&limit={limit}&lang=zh
     * 示例: https://www.pixiv.net/ajax/user/16208053/novels/tag?tag=凌辱&offset=0&limit=30&lang=zh
     */
    suspend fun getUserNovelsByTag(
        uid: Long,
        tag: String,
        offset: Int = 0,
        limit: Int = 30
    ): PixivResponse<UserNovelsByTagBody> {
        return client.get("/ajax/user/$uid/novels/tag", mapOf(
            "tag" to tag,
            "offset" to offset,
            "limit" to limit
        ))
    }
}


