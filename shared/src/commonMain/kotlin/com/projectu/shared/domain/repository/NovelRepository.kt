package com.projectu.shared.domain.repository

import com.projectu.shared.data.remote.model.DiscoveryMode
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
}


