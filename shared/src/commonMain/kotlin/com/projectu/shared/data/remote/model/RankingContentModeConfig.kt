package com.projectu.shared.data.remote.model

/**
 * Pixiv 排行榜内容类型和模式的兼容性配置
 * 
 * 管理不同 content 类型支持的 mode 列表
 */
object RankingContentModeConfig {
    
    /**
     * 内容类型和支持的模式映射表
     * 
     * 根据 Pixiv API 限制：
     * - content=all: 支持大部分 mode（不包括小说专属的 3 个）
     * - content=illust: daily, weekly, monthly, rookie, daily_r18, weekly_r18, r18g
     * - content=manga: daily, weekly, monthly, rookie, daily_r18, weekly_r18, r18g
     * - content=ugoira: daily, weekly, daily_r18, weekly_r18
     * - content=novel: daily, weekly, monthly, rookie, male, female, 
     *                  daily_r18, weekly_r18, male_r18, female_r18, r18g,
     *                  weekly_original, weekly_ai, weekly_r18_ai（小说专属）
     */
    private val contentModesMap: Map<RankingContent, Set<RankingMode>> = mapOf(
        // ALL 支持大部分模式（排除小说专属的 3 个）
        RankingContent.ALL to RankingMode.entries.toSet().minus(
            setOf(
                RankingMode.WEEKLY_ORIGINAL,
                RankingMode.WEEKLY_AI,
                RankingMode.WEEKLY_R18_AI
            )
        ),
        
        // ILLUST 支持的模式
        RankingContent.ILLUST to setOf(
            RankingMode.DAILY,
            RankingMode.WEEKLY,
            RankingMode.MONTHLY,
            RankingMode.ROOKIE,
            RankingMode.DAILY_R18,
            RankingMode.WEEKLY_R18,
            RankingMode.R18G
        ),
        
        // MANGA 支持的模式（与 ILLUST 相同）
        RankingContent.MANGA to setOf(
            RankingMode.DAILY,
            RankingMode.WEEKLY,
            RankingMode.MONTHLY,
            RankingMode.ROOKIE,
            RankingMode.DAILY_R18,
            RankingMode.WEEKLY_R18,
            RankingMode.R18G
        ),
        
        // UGOIRA 支持的模式（较少）
        RankingContent.UGOIRA to setOf(
            RankingMode.DAILY,
            RankingMode.WEEKLY,
            RankingMode.DAILY_R18,
            RankingMode.WEEKLY_R18
        ),
        
        // NOVEL 支持的模式（最多，包含专属模式）
        RankingContent.NOVEL to setOf(
            RankingMode.DAILY,
            RankingMode.WEEKLY,
            RankingMode.MONTHLY,
            RankingMode.ROOKIE,
            RankingMode.MALE,
            RankingMode.FEMALE,
            RankingMode.DAILY_R18,
            RankingMode.WEEKLY_R18,
            RankingMode.MALE_R18,
            RankingMode.FEMALE_R18,
            RankingMode.R18G,
            // 小说专属模式
            RankingMode.WEEKLY_ORIGINAL,
            RankingMode.WEEKLY_AI,
            RankingMode.WEEKLY_R18_AI
        )
    )
    
    /**
     * 获取指定内容类型支持的所有排行榜模式
     * 
     * @param content 内容类型
     * @return 支持的排行榜模式列表（按定义顺序）
     */
    fun getSupportedModes(content: RankingContent): List<RankingMode> {
        return contentModesMap[content]?.sortedBy { it.ordinal } ?: emptyList()
    }
    
    /**
     * 检查指定的 content 和 mode 组合是否兼容
     * 
     * @param content 内容类型
     * @param mode 排行榜模式
     * @return true 如果兼容，false 如果不兼容
     */
    fun isCompatible(content: RankingContent, mode: RankingMode): Boolean {
        return contentModesMap[content]?.contains(mode) == true
    }
    
    /**
     * 获取指定模式支持的所有内容类型
     * 
     * @param mode 排行榜模式
     * @return 支持该模式的内容类型列表
     */
    fun getSupportedContents(mode: RankingMode): List<RankingContent> {
        return contentModesMap
            .filter { (_, modes) -> modes.contains(mode) }
            .keys
            .sortedBy { it.ordinal }
    }
    
    /**
     * 获取指定内容类型的默认排行榜模式
     * 
     * 优先级: daily > weekly > monthly > 其他
     * 
     * @param content 内容类型
     * @return 默认的排行榜模式，如果没有支持的模式则返回 null
     */
    fun getDefaultMode(content: RankingContent): RankingMode? {
        val supportedModes = getSupportedModes(content)
        return when {
            supportedModes.contains(RankingMode.DAILY) -> RankingMode.DAILY
            supportedModes.contains(RankingMode.WEEKLY) -> RankingMode.WEEKLY
            supportedModes.contains(RankingMode.MONTHLY) -> RankingMode.MONTHLY
            supportedModes.isNotEmpty() -> supportedModes.first()
            else -> null
        }
    }
    
    /**
     * 根据分类和 AI 类型过滤支持的模式
     * 
     * @param content 内容类型
     * @param category 排行榜分类（null 表示不限制）
     * @param aiType AI 类型（null 表示不限制）
     * @return 符合条件的排行榜模式列表
     * 
     * 示例：
     * ```
     * // 获取插画的一般向排行榜
     * getFilteredModes(ILLUST, GENERAL, null)
     * 
     * // 获取小说的 R-18 AI 排行榜
     * getFilteredModes(NOVEL, R18, AI)
     * ```
     */
    fun getFilteredModes(
        content: RankingContent,
        category: RankingCategory? = null,
        aiType: RankingAiType? = null
    ): List<RankingMode> {
        return getSupportedModes(content).filter { mode ->
            val categoryMatch = category == null || mode.category == category
            val aiTypeMatch = aiType == null || mode.aiType == aiType
            categoryMatch && aiTypeMatch
        }
    }
    
    /**
     * 获取内容类型支持的一般向排行榜
     */
    fun getGeneralModes(content: RankingContent): List<RankingMode> {
        return getFilteredModes(content, RankingCategory.GENERAL, null)
    }
    
    /**
     * 获取内容类型支持的 R-18 排行榜
     */
    fun getR18Modes(content: RankingContent): List<RankingMode> {
        return getFilteredModes(content, RankingCategory.R18, null)
    }
    
    /**
     * 获取内容类型支持的 R-18G 排行榜
     */
    fun getR18GModes(content: RankingContent): List<RankingMode> {
        return getFilteredModes(content, RankingCategory.R18G, null)
    }
    
    /**
     * 获取内容类型支持的非 AI 排行榜
     */
    fun getNonAiModes(content: RankingContent): List<RankingMode> {
        return getFilteredModes(content, null, RankingAiType.NON_AI)
    }
    
    /**
     * 获取内容类型支持的 AI 排行榜
     */
    fun getAiModes(content: RankingContent): List<RankingMode> {
        return getFilteredModes(content, null, RankingAiType.AI)
    }
    
    /**
     * 获取小说专属的排行榜模式
     */
    fun getNovelExclusiveModes(): List<RankingMode> {
        return listOf(
            RankingMode.WEEKLY_ORIGINAL,
            RankingMode.WEEKLY_AI,
            RankingMode.WEEKLY_R18_AI
        )
    }
    
    /**
     * 检查是否为小说专属模式
     */
    fun isNovelExclusiveMode(mode: RankingMode): Boolean {
        return getNovelExclusiveModes().contains(mode)
    }
    
    /**
     * 获取所有内容类型和模式的兼容性矩阵（用于调试）
     */
    fun getCompatibilityMatrix(): Map<RankingContent, List<RankingMode>> {
        return contentModesMap.mapValues { it.value.sortedBy { mode -> mode.ordinal } }
    }
}

