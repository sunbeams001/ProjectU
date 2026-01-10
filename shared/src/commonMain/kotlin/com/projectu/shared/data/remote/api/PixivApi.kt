package com.projectu.shared.data.remote.api

import io.ktor.client.HttpClient

/**
 * Pixiv API 统一门面
 * 提供对所有 Pixiv API 的访问入口
 * 
 * @property client Pixiv API 客户端
 * @property illustApi 插画 API
 * @property illustSeriesApi 漫画系列 API
 * @property userApi 用户 API
 * @property bookmarkApi 收藏 API
 * @property rankingApi 排行榜 API
 * @property commentApi 评论 API
 * @property novelApi 小说 API
 * @property novelSeriesApi 小说系列 API
 * @property tagApi 标签 API
 * @property markerApi 书签 API（稍后再读）
 * @property followApi 关注 API（已关注用户的作品、追更）
 * @property searchApi 搜索 API
 * @property pixivisionApi Pixivision API（附属网站特辑）
 */
class PixivApi(
    val client: PixivApiClient
) {
    /**
     * 插画API
     */
    val illustApi: IllustApi = IllustApi(client)

    /**
     * 漫画系列API
     */
    val illustSeriesApi: IllustSeriesApi = IllustSeriesApi(client)

    /**
     * 用户API
     */
    val userApi: UserApi = UserApi(client)

    /**
     * 收藏API
     */
    val bookmarkApi: BookmarkApi = BookmarkApi(client)

    /**
     * 排行榜API
     */
    val rankingApi: RankingApi = RankingApi(client)

    /**
     * 评论API
     */
    val commentApi: CommentApi = CommentApi(client)

    /**
     * 小说API
     */
    val novelApi: NovelApi = NovelApi(client)

    /**
     * 小说系列API
     */
    val novelSeriesApi: NovelSeriesApi = NovelSeriesApi(client)

    /**
     * 标签API
     */
    val tagApi: TagApi = TagApi(client)

    /**
     * 书签API（稍后再读）
     */
    val markerApi: MarkerApi = MarkerApi(client)

    /**
     * 关注API（已关注用户的作品、追更）
     */
    val followApi: FollowApi = FollowApi(client)

    /**
     * 搜索API
     */
    val searchApi: SearchApi = SearchApi(client)

    /**
     * Pixivision API（附属网站特辑）
     */
    val pixivisionApi: PixivisionApi = PixivisionApi(client)

    companion object {
        /**
         * 创建 Pixiv API 实例
         * @param httpClient Ktor HttpClient
         * @param phpSessionId PHPSESSID cookie值
         * @param token CSRF token（可选，会自动获取）
         * @param host API主机地址
         * @param lang 语言设置
         * @param onTokenUpdated CSRF token更新回调
         */
        fun create(
            httpClient: HttpClient,
            phpSessionId: String,
            token: String? = null,
            host: String = PixivApiClient.DEFAULT_HOST,
            lang: String = PixivApiClient.DEFAULT_LANG,
            onTokenUpdated: (suspend (String) -> Unit)? = null
        ): PixivApi {
            val client = PixivApiClient(
                httpClient = httpClient,
                phpSessionId = phpSessionId,
                token = token,
                host = host,
                langProvider = { lang },
                onTokenUpdated = onTokenUpdated
            )
            return PixivApi(client)
        }
    }
}

