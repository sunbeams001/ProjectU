package com.projectu.shared.data.remote.dto.ranking

import kotlinx.serialization.Serializable

@Serializable
data class IllustSeries(
    val illust_series_id: String,
    val illust_series_user_id: String,
    val illust_series_title: String,
    val illust_series_caption: String,
    val illust_series_content_count: String,
    val illust_series_create_datetime: String,
    val illust_series_content_illust_id: String,
    val illust_series_content_order: String,
    val page_url: String
)
