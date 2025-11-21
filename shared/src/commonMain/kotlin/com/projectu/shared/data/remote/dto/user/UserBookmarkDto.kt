package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.ZoneConfig
import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import kotlinx.serialization.Serializable

/**
 * 用户收藏响应体
 */
@Serializable
data class UserBookmarkBody(
    val works: List<IllustSimple>,
    val total: Int,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)
