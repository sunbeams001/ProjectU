package com.projectu.shared.util

import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.domain.model.AgeLimit

/**
 * 年龄限制判定工具
 * 
 * 根据作品的 xRestrict 和 Sanity Level 综合判断年龄限制等级
 * 使用 SettingsCache 获取 R18 Sanity Level 阈值（统一配置缓存框架）
 * 
 * 性能优化：使用 SettingsCache 内存缓存，避免重复数据库查询
 */
class AgeLimitDeterminer(
    private val settingsCache: SettingsCache
) {
    /**
     * 确定作品的年龄限制等级
     * 
     * @param xRestrict 年龄限制标识（0: 全年龄, 1: R18, 2: R18G）
     * @param sl sanity level（安全等级，可选）
     * @param tags 标签列表（可选，用于辅助判断）
     * @return 年龄限制等级
     */
    fun determine(
        xRestrict: Int,
        sl: Int? = null,
        tags: List<String>? = null
    ): AgeLimit {
        // 使用 SettingsCache 内存缓存的阈值，避免每次都查询数据库
        val threshold = settingsCache.getR18SanityLevelThreshold()
        
        // 复合判断：xRestrict 和 Sanity Level 任一达到 R18 标准则为 R18
        val isR18ByXRestrict = xRestrict >= 1
        val isR18BySanityLevel = sl != null && sl >= threshold
        
        return when {
            // R18G 最高优先级
            xRestrict == 2 -> AgeLimit.R18G
            // xRestrict 或 Sanity Level 任一达到 R18
            isR18ByXRestrict || isR18BySanityLevel -> AgeLimit.R18
            // 其他情况为全年龄
            else -> AgeLimit.ALL_AGE
        }
    }
}
