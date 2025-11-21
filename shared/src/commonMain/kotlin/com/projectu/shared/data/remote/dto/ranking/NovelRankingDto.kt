package com.projectu.shared.data.remote.dto.ranking

import kotlinx.serialization.Serializable

/**
 * 小说排行榜条目数据类
 * 从HTML页面解析得到的小说信息
 */
@Serializable
data class NovelRankingItem(
    // 排行榜排名
    val rank: Int,
    
    // 小说ID
    val novelId: String,
    
    // 小说标题
    val title: String,
    
    // 作者信息
    val author: AuthorInfo,
    
    // 封面图片URL
    val coverImageUrl: String,
    
    // 字符数
    val characterCount: Int,
    
    // 书签数（收藏数）
    val bookmarkCount: Int,
    
    // 标签列表
    val tags: List<String>,
    
    // 简介/描述
    val caption: String,
    
    // 系列信息（可选）
    val series: SeriesInfo? = null,
    
    // 小说链接
    val novelUrl: String,
    
    // 是否已收藏
    val isBookmarked: Boolean = false,
    
    // 收藏ID（如果已收藏）
    val bookmarkId: String? = null,
    
    // 收藏限制（0=公开，1=私密）
    val bookmarkRestrict: String? = null,
    
    // 阅读进度标记（书签位置）
    val marker: Int? = null
)

/**
 * 作者信息
 */
@Serializable
data class AuthorInfo(
    // 作者ID
    val userId: String,
    
    // 作者名称
    val userName: String,
    
    // 作者头像URL
    val profileImageUrl: String,
    
    // 作者小说列表链接
    val novelListUrl: String
)

/**
 * 系列信息
 */
@Serializable
data class SeriesInfo(
    // 系列ID
    val seriesId: String,
    
    // 系列标题
    val seriesTitle: String,
    
    // 系列链接
    val seriesUrl: String
)

/**
 * 小说排行榜响应
 */
@Serializable
data class NovelRankingResponse(
    // 排行榜类型（daily, weekly, monthly等）
    val mode: String,
    
    // 排行榜日期
    val date: String,
    
    // 当前页码
    val currentPage: Int,
    
    // 总页数
    val totalPages: Int,
    
    // 当前页的排名范围（如：#1 - #50）
    val rankRange: String,
    
    // 小说列表
    val novels: List<NovelRankingItem>,
    
    // 下一页链接（可选）
    val nextPageUrl: String? = null,
    
    // 上一页链接（可选）
    val previousPageUrl: String? = null
)
