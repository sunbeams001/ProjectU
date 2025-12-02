package com.projectu.shared.data.remote.model

/**
 * Pixiv 排行榜模式
 * 具有两个分类维度：
 * 1. 内容分类：一般 / R-18
 * 2. 生成方式：普通 / AI生成
 */
enum class RankingMode(
    val value: String,
    val displayName: String,
    val category: RankingCategory,
    val aiType: RankingAiType
) {
    // ==================== 一般排行榜 - 普通 ====================
    
    /** 今日 */
    DAILY("daily", "Daily", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 本周 */
    WEEKLY("weekly", "Weekly", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 本月 */
    MONTHLY("monthly", "Monthly", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 新人 */
    ROOKIE("rookie", "Rookie", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 原创 */
    ORIGINAL("original", "Original", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 男性向 */
    MALE("male", "Male", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 女性向 */
    FEMALE("female", "Female", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    // ==================== 一般排行榜 - AI生成 ====================
    
    /** AI生成 */
    DAILY_AI("daily_ai", "AI", RankingCategory.GENERAL, RankingAiType.AI),
    
    // ==================== 小说专属排行榜 ====================
    
    /** 本周原创（小说） */
    WEEKLY_ORIGINAL("weekly_original", "Weekly Original", RankingCategory.GENERAL, RankingAiType.NON_AI),
    
    /** 本周AI（小说） */
    WEEKLY_AI("weekly_ai", "Weekly AI", RankingCategory.GENERAL, RankingAiType.AI),
    
    /** 本周R-18 AI（小说） */
    WEEKLY_R18_AI("weekly_r18_ai", "Weekly R-18 AI", RankingCategory.R18, RankingAiType.AI),
    
    // ==================== R-18 排行榜 - 普通 ====================
    
    /** 今日R-18 */
    DAILY_R18("daily_r18", "Daily R-18", RankingCategory.R18, RankingAiType.NON_AI),
    
    /** 本周R-18 */
    WEEKLY_R18("weekly_r18", "Weekly R-18", RankingCategory.R18, RankingAiType.NON_AI),
    
    /** 男性向R-18 */
    MALE_R18("male_r18", "Male R-18", RankingCategory.R18, RankingAiType.NON_AI),
    
    /** 女性向R-18 */
    FEMALE_R18("female_r18", "Female R-18", RankingCategory.R18, RankingAiType.NON_AI),
    
    // ==================== R-18 排行榜 - AI生成 ====================
    
    /** AI生成R-18 */
    DAILY_R18_AI("daily_r18_ai", "AI R-18", RankingCategory.R18, RankingAiType.AI),
    
    // ==================== R-18G 排行榜 ====================
    
    /** R-18G（猎奇向） */
    R18G("r18g", "R-18G", RankingCategory.R18G, RankingAiType.NON_AI);
    
    companion object {
        /**
         * 根据字符串值获取枚举
         */
        fun fromValue(value: String): RankingMode? {
            return entries.find { it.value == value }
        }
        
        // ==================== 单维度过滤 ====================
        
        /**
         * 获取所有一般排行榜（不限AI）
         */
        fun getGeneralModes(): List<RankingMode> {
            return entries.filter { it.category == RankingCategory.GENERAL }
        }
        
        /**
         * 获取所有 R-18 排行榜（不限AI）
         */
        fun getR18Modes(): List<RankingMode> {
            return entries.filter { it.category == RankingCategory.R18 }
        }
        
        /**
         * 获取所有 R-18G 排行榜
         */
        fun getR18GModes(): List<RankingMode> {
            return entries.filter { it.category == RankingCategory.R18G }
        }
        
        /**
         * 获取所有非AI生成排行榜（不限内容分类）
         */
        fun getNonAiModes(): List<RankingMode> {
            return entries.filter { it.aiType == RankingAiType.NON_AI }
        }
        
        /**
         * 获取所有AI生成排行榜（不限内容分类）
         */
        fun getAiModes(): List<RankingMode> {
            return entries.filter { it.aiType == RankingAiType.AI }
        }
        
        // ==================== 双维度过滤 ====================
        
        /**
         * 根据内容分类和AI类型获取排行榜模式列表
         * 
         * @param category 内容分类（null 表示全部）
         * @param aiType AI类型（null 表示全部）
         * @return 符合条件的排行榜模式列表
         * 
         * 示例：
         * - getModes(GENERAL, NON_AI) -> 一般向非AI排行榜
         * - getModes(R18, AI) -> R-18的AI排行榜
         * - getModes(GENERAL, null) -> 所有一般向排行榜（包括AI和非AI）
         * - getModes(null, AI) -> 所有AI生成排行榜（包括一般向和R-18）
         * - getModes(null, null) -> 所有排行榜
         */
        fun getModes(
            category: RankingCategory? = null,
            aiType: RankingAiType? = null
        ): List<RankingMode> {
            return entries.filter { mode ->
                val categoryMatch = category == null || mode.category == category
                val aiTypeMatch = aiType == null || mode.aiType == aiType
                categoryMatch && aiTypeMatch
            }
        }
        
        /**
         * 获取一般向非AI排行榜
         */
        fun getGeneralNonAiModes(): List<RankingMode> {
            return getModes(RankingCategory.GENERAL, RankingAiType.NON_AI)
        }
        
        /**
         * 获取一般向AI排行榜
         */
        fun getGeneralAiModes(): List<RankingMode> {
            return getModes(RankingCategory.GENERAL, RankingAiType.AI)
        }
        
        /**
         * 获取R-18非AI排行榜
         */
        fun getR18NonAiModes(): List<RankingMode> {
            return getModes(RankingCategory.R18, RankingAiType.NON_AI)
        }
        
        /**
         * 获取R-18 AI排行榜
         */
        fun getR18AiModes(): List<RankingMode> {
            return getModes(RankingCategory.R18, RankingAiType.AI)
        }
        
        /**
         * 获取R-18G排行榜
         */
        fun getR18GNonAiModes(): List<RankingMode> {
            return getModes(RankingCategory.R18G, RankingAiType.NON_AI)
        }
        
        /**
         * 获取所有模式（便捷方法）
         */
        fun getAllModes(): List<RankingMode> {
            return entries
        }
        
        // ==================== 内容类型兼容性查询 ====================
        
        /**
         * 获取支持指定模式的所有内容类型
         * 
         * @param mode 排行榜模式
         * @return 支持该模式的内容类型列表
         */
        fun getSupportedContents(mode: RankingMode): List<RankingContent> {
            return RankingContentModeConfig.getSupportedContents(mode)
        }
        
        /**
         * 检查指定的 content 和 mode 组合是否兼容
         * 
         * @param content 内容类型
         * @param mode 排行榜模式
         * @return true 如果兼容，false 如果不兼容
         */
        fun isCompatible(content: RankingContent, mode: RankingMode): Boolean {
            return RankingContentModeConfig.isCompatible(content, mode)
        }
        
        /**
         * 获取小说专属的排行榜模式
         */
        fun getNovelExclusiveModes(): List<RankingMode> {
            return RankingContentModeConfig.getNovelExclusiveModes()
        }
    }
}

/**
 * 排行榜内容分类
 */
enum class RankingCategory(val displayName: String) {
    /** 一般 */
    GENERAL("General"),
    /** R-18 */
    R18("R-18"),
    /** R-18G（猎奇向） */
    R18G("R-18G")
}

/**
 * 排行榜AI生成类型
 */
enum class RankingAiType(val displayName: String) {
    /** 非AI生成（普通） */
    NON_AI("Non-AI"),
    /** AI生成 */
    AI("AI")
}
