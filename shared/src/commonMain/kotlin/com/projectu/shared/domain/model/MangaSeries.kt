package com.projectu.shared.domain.model

/**
 * 漫画系列领域模型
 */
data class MangaSeries(
    val id: String,
    val title: String,
    val description: String = "",
    val caption: String = "",
    // 用户信息
    val userId: String,
    val userName: String,
    val profileImageUrl: String? = null,
    val isFollowed: Boolean = false,
    // 封面图片
    val coverUrl: String? = null,
    // 系列状态
    val isWatched: Boolean = false,
    val isNotifying: Boolean = false,
    // 统计信息
    val total: Int = 0, // 作品数量（系列中的篇数）
    val watchCount: Int? = null, // 追更人数
    // 时间信息
    val createDate: String = "",
    val updateDate: String = "",
    // 第一篇和最新一篇的ID
    val firstIllustId: String? = null,
    val latestIllustId: String? = null,
    // 作品列表
    val works: List<MangaSeriesWork> = emptyList()
)

/**
 * 漫画系列中的作品
 */
data class MangaSeriesWork(
    val workId: String,
    val order: Int
)
