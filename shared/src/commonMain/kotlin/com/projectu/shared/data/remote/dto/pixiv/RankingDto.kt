package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.Serializable

/**
 * 排行榜响应
 */
@Serializable
data class RankingResponse(
    val contents: List<RankingContent>,
    val mode: String,
    val content: String,
    val page: Int,
    val prev: Int? = null,
    val next: Int? = null,
    val date: String,
    val prev_date: String? = null,
    val next_date: String? = null,
    val rank_total: Int
)

@Serializable
data class RankingContent(
    val title: String,
    val date: String,
    val tags: List<String>,
    val url: String,
    val illust_type: String,
    val illust_book_style: String,
    val illust_page_count: String,
    val user_name: String,
    val profile_img: String,
    val illust_content_type: RankingContentType,
    val illust_series: Boolean,
    val illust_id: Long,
    val width: Int,
    val height: Int,
    val user_id: Long,
    val rank: Int,
    val yes_rank: Int = 0,
    val rating_count: Int,
    val view_count: Int,
    val illust_upload_timestamp: Long,
    val attr: String = ""
)

@Serializable
data class RankingContentType(
    val sexual: Int = 0,
    val lo: Boolean = false,
    val grotesque: Boolean = false,
    val violent: Boolean = false,
    val homosexual: Boolean = false,
    val drug: Boolean = false,
    val thoughts: Boolean = false,
    val antisocial: Boolean = false,
    val religion: Boolean = false,
    val original: Boolean = false,
    val furry: Boolean = false,
    val bl: Boolean = false,
    val yuri: Boolean = false
)

