package com.projectu.shared.domain.model

import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingContentModeConfig
import com.projectu.shared.data.remote.model.RankingMode

/**
 * 排行榜导航偏好配置
 * 
 * 用于控制排行榜页面显示哪些导航项：
 * - 一级导航：内容类型（综合、插画、动图、漫画、小说）
 * - 二级导航：排行榜模式（每日、每周、每月等，根据内容类型动态变化）
 * 
 * @property enabledContentTypes 启用的一级导航项（内容类型名称集合）
 * @property enabledModesPerContent 每个内容类型启用的二级导航项（模式名称集合）
 *                                  Key: RankingContent.name, Value: Set of RankingMode.name
 */
data class RankingNavigationPreferences(
    /**
     * 启用的内容类型
     * 默认：全部启用
     */
    val enabledContentTypes: Set<String> = DEFAULT_CONTENT_TYPES,
    
    /**
     * 每个内容类型启用的排行榜模式
     * 默认：每个类型的所有支持模式都启用
     */
    val enabledModesPerContent: Map<String, Set<String>> = DEFAULT_MODES_MAP
) {
    companion object {
        /**
         * 默认的内容类型集合（全部启用）
         */
        private val DEFAULT_CONTENT_TYPES: Set<String> = 
            RankingContent.entries.map { it.name }.toSet()
        
        /**
         * 默认的模式映射（每个内容类型的所有支持模式都启用）
         */
        private val DEFAULT_MODES_MAP: Map<String, Set<String>> = buildDefaultModesMap()
        
        /**
         * 默认配置：全部启用
         */
        val DEFAULT = RankingNavigationPreferences()
        
        /**
         * 构建默认的模式映射
         */
        private fun buildDefaultModesMap(): Map<String, Set<String>> {
            return RankingContent.entries.associate { content ->
                content.name to RankingContentModeConfig
                    .getSupportedModes(content)
                    .map { it.name }
                    .toSet()
            }
        }
    }
    
    /**
     * 检查某个内容类型是否启用
     */
    fun isContentTypeEnabled(content: RankingContent): Boolean {
        return content.name in enabledContentTypes
    }
    
    /**
     * 获取某个内容类型下启用的模式列表
     * 
     * @param content 内容类型
     * @return 启用的排行榜模式列表（按原始顺序）
     */
    fun getEnabledModes(content: RankingContent): List<RankingMode> {
        val enabledModeNames = enabledModesPerContent[content.name] ?: emptySet()
        return RankingContentModeConfig.getSupportedModes(content)
            .filter { it.name in enabledModeNames }
    }
    
    /**
     * 获取所有启用的内容类型
     * 
     * @return 启用的内容类型列表（按原始顺序）
     *         如果全部禁用，返回包含第一个类型的列表（保护性措施）
     */
    fun getEnabledContentTypes(): List<RankingContent> {
        val enabled = RankingContent.entries.filter { it.name in enabledContentTypes }
        // 保护措施：如果全部禁用，至少返回第一个
        return enabled.ifEmpty { listOf(RankingContent.ALL) }
    }
    
    /**
     * 切换某个内容类型的启用状态
     * 
     * @param content 要切换的内容类型
     * @return 新的配置对象
     */
    fun toggleContentType(content: RankingContent): RankingNavigationPreferences {
        val newTypes = if (content.name in enabledContentTypes) {
            enabledContentTypes - content.name
        } else {
            enabledContentTypes + content.name
        }
        return copy(enabledContentTypes = newTypes)
    }
    
    /**
     * 切换某个内容类型下的某个模式的启用状态
     * 
     * @param content 内容类型
     * @param mode 要切换的模式
     * @return 新的配置对象
     */
    fun toggleMode(content: RankingContent, mode: RankingMode): RankingNavigationPreferences {
        val currentModes = enabledModesPerContent[content.name] ?: emptySet()
        val newModes = if (mode.name in currentModes) {
            currentModes - mode.name
        } else {
            currentModes + mode.name
        }
        val newMap = enabledModesPerContent.toMutableMap().apply {
            put(content.name, newModes)
        }
        return copy(enabledModesPerContent = newMap)
    }
    
    /**
     * 重置为默认配置（全部启用）
     */
    fun reset(): RankingNavigationPreferences = DEFAULT
    
    /**
     * 验证配置是否有效
     * 
     * @return true 如果配置有效（至少有一个启用的内容类型和对应的模式）
     */
    fun isValid(): Boolean {
        if (enabledContentTypes.isEmpty()) return false
        
        // 检查是否至少有一个内容类型有启用的模式
        return enabledContentTypes.any { contentName ->
            val modes = enabledModesPerContent[contentName] ?: emptySet()
            modes.isNotEmpty()
        }
    }
}
