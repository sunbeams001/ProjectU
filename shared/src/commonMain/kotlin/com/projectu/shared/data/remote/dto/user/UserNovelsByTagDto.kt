package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.novel.NovelSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户指定标签的小说作品列表响应体
 * 
 * 接口地址: GET /ajax/user/{userId}/novels/tag?tag={tag}&offset={offset}&limit={limit}&lang=zh
 * 示例: https://www.pixiv.net/ajax/user/16208053/novels/tag?tag=凌辱&offset=0&limit=30&lang=zh
 * 
 * 用途：获取该用户指定标签下的小说作品列表
 */
@Serializable
data class UserNovelsByTagBody(
    /**
     * 作品列表
     */
    val works: List<NovelSimple>,
    
    /**
     * 该标签下的作品总数
     */
    val total: Int,
    
    /**
     * 广告配置
     */
    @SerialName("zoneConfig")
    val zoneConfig: Map<String, ZoneConfig>? = null,
    
    /**
     * 额外数据
     */
    val extraData: ExtraData? = null
)
