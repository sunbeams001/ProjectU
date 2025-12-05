package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.common.ZoneConfig
import kotlinx.serialization.Serializable

/**
 * 好P友列表响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/mypixiv
 * 参数:
 *   - offset: 偏移量
 *   - limit: 返回数量（最大24）
 *   - lang: 语言
 * 
 * 注意：与关注/粉丝列表不同，此接口的 total 是字符串类型
 */
@Serializable
data class MyPixivBody(
    val users: List<FollowingUser>,
    val total: String,  // 注意：好P友接口的 total 是字符串类型
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)
