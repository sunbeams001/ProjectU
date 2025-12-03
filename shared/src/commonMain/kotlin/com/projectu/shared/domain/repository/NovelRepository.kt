package com.projectu.shared.domain.repository

import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.domain.model.Novel

/**
 * 小说仓库接口
 * 
 * 提供小说相关的数据访问功能
 */
interface NovelRepository {
    
    /**
     * 获取小说详情
     * @param novelId 小说ID
     * @return 小说详情
     */
    suspend fun getNovelDetail(novelId: String): Result<Novel>
    
    /**
     * 搜索小说
     * @param keyword 关键词
     * @param searchMode 搜索模式
     * @param order 排序方式
     * @param mode 内容模式
     * @param page 页码
     * @return 小说列表
     */
    suspend fun searchNovels(
        keyword: String,
        searchMode: String = "s_tag",
        order: String = "date_d",
        mode: String = "all",
        page: Int = 1
    ): Result<List<Novel>>
    
    /**
     * 获取发现/推荐小说
     * @param mode 发现模式（ALL, SAFE, R18）
     * @param limit 返回数量
     * @return 推荐小说列表
     */
    suspend fun getDiscoveryNovels(
        mode: DiscoveryMode = DiscoveryMode.ALL,
        limit: Int = 100
    ): Result<List<Novel>>
    
    /**
     * 获取关注用户的最新小说
     * @param mode 模式：all, r18
     * @param page 页码
     * @return Pair<小说列表, 是否最后一页>
     */
    suspend fun getFollowLatestNovels(
        mode: String = "all",
        page: Int = 1
    ): Result<Pair<List<Novel>, Boolean>>
    
    /**
     * 获取小说排行榜
     * @param mode 排行榜模式
     * @param content 内容类型（应该是 NOVEL 或 ALL）
     * @param page 页码
     * @param date 日期（格式：yyyyMMdd，可选）
     * @return 排行榜小说列表
     */
    suspend fun getRankingNovels(
        mode: RankingMode = RankingMode.DAILY,
        content: RankingContent = RankingContent.NOVEL,
        page: Int = 1,
        date: String? = null
    ): Result<List<Novel>>
    
    /**
     * 获取小说排行榜（包含日期信息）
     * @return Pair<小说列表, 日期信息(currentDate, prevDate, nextDate)>
     */
    suspend fun getRankingWithDateInfo(
        mode: RankingMode = RankingMode.DAILY,
        content: RankingContent = RankingContent.NOVEL,
        page: Int = 1,
        date: String? = null
    ): Result<Pair<List<Novel>, Triple<String?, String?, String?>>>
    
    /**
     * 添加小说收藏
     * @param novelId 小说ID
     * @param isPrivate 是否私人收藏
     * @param tags 收藏标签
     * @return 新创建的收藏ID
     */
    suspend fun addBookmark(
        novelId: Long,
        isPrivate: Boolean = false,
        tags: List<String> = emptyList()
    ): Result<String>
    
    /**
     * 删除小说收藏
     * @param bookmarkId 收藏ID（从Novel.bookmarkId获取）
     */
    suspend fun removeBookmark(bookmarkId: String): Result<Unit>
    
    /**
     * 添加小说阅读书签（稍后再读标记）
     * @param novelId 小说ID
     * @param userId 用户ID
     * @param page 当前阅读页码
     * @return 是否成功
     */
    suspend fun addNovelMarker(
        novelId: Long,
        userId: Long,
        page: Int
    ): Result<Unit>
    
    /**
     * 删除小说阅读书签
     * @param novelId 小说ID
     * @param userId 用户ID
     * @return 是否成功
     */
    suspend fun deleteNovelMarker(
        novelId: Long,
        userId: Long
    ): Result<Unit>
}


