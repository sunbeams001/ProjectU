package com.projectu.shared.data.remote.dto.illust

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import kotlinx.serialization.Serializable

@Serializable
data class IllustSimple(
    val id: String,  // 字符串类型
    val title: String,
    val illustType: Int,
    val xRestrict: Int,
    val restrict: Int,
    val sl: Int,
    val url: String,
    val description: String,
    val tags: List<String>,
    val userId: String,  // 字符串类型
    val userName: String,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val isBookmarkable: Boolean,
    val bookmarkData: BookmarkData? = null,
    val alt: String,
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val createDate: String,
    val updateDate: String,
    val isUnlisted: Boolean = false,
    val isMasked: Boolean = false,
    val aiType: Int = 0,
    val visibilityScope: Int = 0,  // 可见性范围
    val profileImageUrl: String? = null,
    val type: String? = null,  // 作品类型，如 "illust", "manga" 等
    val urls: Map<String, String>? = null,  // 不同尺寸的缩略图 URL，如 "250x250", "360x360", "540x540", "1200x1200"
    val seriesId: String? = null,  // 系列 ID
    val seriesTitle: String? = null  // 系列标题
)
