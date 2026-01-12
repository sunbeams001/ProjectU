package com.projectu.shared.domain.model


/**
 * 动态页面导航配置
 * 用于自定义动态页面中显示哪些内容类型和子项
 * 
 * 支持的内容类型：
 * - ILLUSTS：插画&amp;漫画（二级导航：ALL/R18）
 * - NOVELS：小说（二级导航：ALL/R18）
 * - WATCH_LIST：追更列表（二级导航：漫画/小说）
 * - GOOD_P_FRIENDS：好P友（无二级导航）
 */
data class FollowLatestNavigationPreferences(
    /**
     * 启用的内容类型
     * 存储 FollowLatestContentType 的 name 字符串
     * 默认全部启用：["ILLUSTS", "NOVELS", "WATCH_LIST", "GOOD_P_FRIENDS"]
     */
    val enabledContentTypes: Set<String> = DEFAULT_ENABLED_CONTENT_TYPES,
    
    /**
     * ILLUSTS 内容类型的启用模式
     * 存储 FollowLatestMode 的 name 字符串
     * 默认全部启用：["ALL", "R18"]
     */
    val illustsEnabledModes: Set<String> = DEFAULT_ENABLED_MODES,
    
    /**
     * NOVELS 内容类型的启用模式
     * 存储 FollowLatestMode 的 name 字符串
     * 默认全部启用：["ALL", "R18"]
     */
    val novelsEnabledModes: Set<String> = DEFAULT_ENABLED_MODES,
    
    /**
     * WATCH_LIST 内容类型的启用子类型
     * 存储 WatchListContentType 的 name 字符串
     * 默认全部启用：["MANGA", "NOVELS"]
     */
    val watchListEnabledTypes: Set<String> = DEFAULT_ENABLED_WATCH_LIST_TYPES
) {
    
    /**
     * 获取启用的内容类型列表（保证至少有一个）
     */
    fun getFilteredContentTypes(): Set<String> {
        return if (enabledContentTypes.isEmpty()) {
            setOf(DEFAULT_CONTENT_TYPE)
        } else {
            enabledContentTypes
        }
    }
    
    /**
     * 判断内容类型是否启用
     */
    fun isContentTypeEnabled(contentType: String): Boolean {
        return getFilteredContentTypes().contains(contentType)
    }
    
    /**
     * 获取ILLUSTS的启用模式（保证至少有一个）
     */
    fun getFilteredIllustsModes(): Set<String> {
        return if (illustsEnabledModes.isEmpty()) {
            setOf("ALL")
        } else {
            illustsEnabledModes
        }
    }
    
    /**
     * 获取NOVELS的启用模式（保证至少有一个）
     */
    fun getFilteredNovelsModes(): Set<String> {
        return if (novelsEnabledModes.isEmpty()) {
            setOf("ALL")
        } else {
            novelsEnabledModes
        }
    }
    
    /**
     * 获取追更列表的启用子类型（保证至少有一个）
     */
    fun getFilteredWatchListTypes(): Set<String> {
        return if (watchListEnabledTypes.isEmpty()) {
            setOf(DEFAULT_WATCH_LIST_TYPE)
        } else {
            watchListEnabledTypes
        }
    }
    
    /**
     * 切换内容类型的启用状态
     */
    fun toggleContentType(contentType: String): FollowLatestNavigationPreferences {
        val newTypes = if (enabledContentTypes.contains(contentType)) {
            // 禁用：确保至少保留一个
            if (enabledContentTypes.size > 1) {
                enabledContentTypes - contentType
            } else {
                enabledContentTypes // 不允许全部禁用
            }
        } else {
            // 启用
            enabledContentTypes + contentType
        }
        return copy(enabledContentTypes = newTypes)
    }
    
    /**
     * 切换追更列表子类型的启用状态
     */
    fun toggleWatchListType(type: String): FollowLatestNavigationPreferences {
        val newTypes = if (watchListEnabledTypes.contains(type)) {
            if (watchListEnabledTypes.size > 1) {
                watchListEnabledTypes - type
            } else {
                watchListEnabledTypes
            }
        } else {
            watchListEnabledTypes + type
        }
        return copy(watchListEnabledTypes = newTypes)
    }
    
    companion object {
        private const val DEFAULT_CONTENT_TYPE = "ILLUSTS"
        private const val DEFAULT_WATCH_LIST_TYPE = "MANGA"
        
        private val DEFAULT_ENABLED_CONTENT_TYPES = setOf("ILLUSTS", "NOVELS", "WATCH_LIST", "GOOD_P_FRIENDS")
        private val DEFAULT_ENABLED_MODES = setOf("ALL", "R18")
        private val DEFAULT_ENABLED_WATCH_LIST_TYPES = setOf("MANGA", "NOVELS")
        
        /**
         * 默认配置：全部启用
         */
        val DEFAULT = FollowLatestNavigationPreferences()
    }
}
