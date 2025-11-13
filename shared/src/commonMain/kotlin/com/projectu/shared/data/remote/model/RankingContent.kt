package com.projectu.shared.data.remote.model

/**
 * Pixiv 排行榜内容类型
 */
enum class RankingContent(
    val value: String,
    val displayName: String
) {
    /** 全部 */
    ALL("all", "全部"),
    
    /** 插画 */
    ILLUST("illust", "插画"),
    
    /** 漫画 */
    MANGA("manga", "漫画"),
    
    /** 动图 */
    UGOIRA("ugoira", "动图");
    
    companion object {
        /**
         * 根据字符串值获取枚举
         */
        fun fromValue(value: String): RankingContent? {
            return entries.find { it.value == value }
        }
    }
}
