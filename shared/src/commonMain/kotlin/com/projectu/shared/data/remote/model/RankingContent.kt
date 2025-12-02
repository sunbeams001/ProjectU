package com.projectu.shared.data.remote.model

/**
 * Pixiv 排行榜内容类型
 */
enum class RankingContent(
    val value: String,
    val displayName: String
) {
    /** 全部 */
    ALL("all", "All"),
    
    /** 插画 */
    ILLUST("illust", "Illust"),
    
    /** 漫画 */
    MANGA("manga", "Manga"),
    
    /** 动图 */
    UGOIRA("ugoira", "Ugoira"),
    
    /** 小说 */
    NOVEL("novel", "Novel");
    
    companion object {
        /**
         * 根据字符串值获取枚举
         */
        fun fromValue(value: String): RankingContent? {
            return entries.find { it.value == value }
        }
        
        /**
         * 获取指定内容类型支持的所有排行榜模式
         * 
         * @param content 内容类型
         * @return 支持的排行榜模式列表
         */
        fun getSupportedModes(content: RankingContent): List<RankingMode> {
            return RankingContentModeConfig.getSupportedModes(content)
        }
    }
}
