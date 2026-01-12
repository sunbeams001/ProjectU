package com.projectu.shared.domain.model


/**
 * 发现页面导航配置
 * 用于自定义发现页面中显示哪些内容类型和子项
 * 
 * 支持的内容类型：
 * - USERS：用户推荐（无二级导航）
 * - ILLUSTS：插画&amp;漫画（二级导航：ALL/SAFE/R18）
 * - NOVELS：小说（二级导航：ALL/SAFE/R18）
 * - PIXIVISION：Pixivision文章（二级导航：插画/漫画分类）
 */
data class DiscoveryNavigationPreferences(
    /**
     * 启用的内容类型
     * 存储 DiscoveryContentType 的 name 字符串
     * 默认全部启用：["USERS", "ILLUSTS", "NOVELS", "PIXIVISION"]
     */
    val enabledContentTypes: Set<String> = DEFAULT_ENABLED_CONTENT_TYPES,
    
    /**
     * ILLUSTS 内容类型的启用模式
     * 存储 DiscoveryMode 的 name 字符串
     * 默认全部启用：["ALL", "SAFE", "R18"]
     */
    val illustsEnabledModes: Set<String> = DEFAULT_ENABLED_MODES,
    
    /**
     * NOVELS 内容类型的启用模式
     * 存储 DiscoveryMode 的 name 字符串
     * 默认全部启用：["ALL", "SAFE", "R18"]
     */
    val novelsEnabledModes: Set<String> = DEFAULT_ENABLED_MODES,
    
    /**
     * PIXIVISION 内容类型的启用类别
     * 存储 PixivisionCategory 的 name 字符串
     * 默认全部启用：["ILLUSTRATION", "MANGA"]
     */
    val pixivisionEnabledCategories: Set<String> = DEFAULT_ENABLED_PIXIVISION_CATEGORIES
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
            setOf(DEFAULT_MODE)
        } else {
            illustsEnabledModes
        }
    }
    
    /**
     * 获取NOVELS的启用模式（保证至少有一个）
     */
    fun getFilteredNovelsModes(): Set<String> {
        return if (novelsEnabledModes.isEmpty()) {
            setOf(DEFAULT_MODE)
        } else {
            novelsEnabledModes
        }
    }
    
    /**
     * 获取PIXIVISION的启用类别（保证至少有一个）
     */
    fun getFilteredPixivisionCategories(): Set<String> {
        return if (pixivisionEnabledCategories.isEmpty()) {
            setOf(DEFAULT_PIXIVISION_CATEGORY)
        } else {
            pixivisionEnabledCategories
        }
    }
    
    /**
     * 切换内容类型的启用状态
     */
    fun toggleContentType(contentType: String): DiscoveryNavigationPreferences {
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
     * 切换ILLUSTS模式的启用状态
     */
    fun toggleIllustsMode(mode: String): DiscoveryNavigationPreferences {
        val newModes = if (illustsEnabledModes.contains(mode)) {
            if (illustsEnabledModes.size > 1) {
                illustsEnabledModes - mode
            } else {
                illustsEnabledModes
            }
        } else {
            illustsEnabledModes + mode
        }
        return copy(illustsEnabledModes = newModes)
    }
    
    /**
     * 切换NOVELS模式的启用状态
     */
    fun toggleNovelsMode(mode: String): DiscoveryNavigationPreferences {
        val newModes = if (novelsEnabledModes.contains(mode)) {
            if (novelsEnabledModes.size > 1) {
                novelsEnabledModes - mode
            } else {
                novelsEnabledModes
            }
        } else {
            novelsEnabledModes + mode
        }
        return copy(novelsEnabledModes = newModes)
    }
    
    /**
     * 切换PIXIVISION类别的启用状态
     */
    fun togglePixivisionCategory(category: String): DiscoveryNavigationPreferences {
        val newCategories = if (pixivisionEnabledCategories.contains(category)) {
            if (pixivisionEnabledCategories.size > 1) {
                pixivisionEnabledCategories - category
            } else {
                pixivisionEnabledCategories
            }
        } else {
            pixivisionEnabledCategories + category
        }
        return copy(pixivisionEnabledCategories = newCategories)
    }
    
    companion object {
        private const val DEFAULT_CONTENT_TYPE = "USERS"
        private const val DEFAULT_MODE = "ALL"
        private const val DEFAULT_PIXIVISION_CATEGORY = "ILLUSTRATION"
        
        private val DEFAULT_ENABLED_CONTENT_TYPES = setOf("USERS", "ILLUSTS", "NOVELS", "PIXIVISION")
        private val DEFAULT_ENABLED_MODES = setOf("ALL", "SAFE", "R18")
        private val DEFAULT_ENABLED_PIXIVISION_CATEGORIES = setOf("ILLUSTRATION", "MANGA")
        
        /**
         * 默认配置：全部启用
         */
        val DEFAULT = DiscoveryNavigationPreferences()
    }
}
