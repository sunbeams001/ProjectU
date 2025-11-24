package com.projectu.shared.util

import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.domain.model.AgeLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 年龄限制判定工具
 * 
 * 根据作品的 xRestrict 和 Sanity Level 综合判断年龄限制等级
 * 从 SettingsStore 订阅 R18 Sanity Level 阈值，缓存在内存中避免重复数据库查询
 * 
 * 性能优化：参照 PixivConfigStore 的语言设置设计，使用内存缓存 + Flow 响应式更新
 */
class AgeLimitDeterminer(
    private val settingsStore: SettingsStore
) {
    // 内存缓存阈值，避免每次都查询数据库
    private val _threshold = MutableStateFlow(6) // 默认值
    val threshold: StateFlow<Int> = _threshold.asStateFlow()
    
    init {
        // 在后台协程中监听设置变化，实时更新内存缓存
        CoroutineScope(Dispatchers.Default).launch {
            settingsStore.settings.collect { settings ->
                _threshold.value = settings.r18SanityLevelThreshold
            }
        }
    }
    
    /**
     * 获取当前阈值（同步方法，使用内存缓存）
     */
    fun getCurrentThreshold(): Int {
        return _threshold.value
    }
    
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
        // 使用内存缓存的阈值，避免每次都查询数据库
        val threshold = getCurrentThreshold()
        
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
