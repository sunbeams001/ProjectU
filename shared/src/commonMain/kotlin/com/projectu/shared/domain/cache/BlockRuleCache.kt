package com.projectu.shared.domain.cache

import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.repository.BlockRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 屏蔽规则缓存
 * 用于高频访问场景，避免重复查询数据库
 * 参考 SettingsCache 设计
 */
class BlockRuleCache(
    private val blockRuleRepository: BlockRuleRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 内存缓存：启用的规则
    private val _enabledRules = MutableStateFlow<List<BlockRule>>(emptyList())
    val enabledRules: StateFlow<List<BlockRule>> = _enabledRules.asStateFlow()
    
    init {
        // 订阅数据库变化，自动同步到内存
        scope.launch {
            blockRuleRepository.observeEnabledRules().collect { rules ->
                _enabledRules.value = rules
            }
        }
    }
    
    /**
     * 同步获取启用的规则（内存访问，零延迟）
     */
    fun getEnabledRules(): List<BlockRule> = _enabledRules.value
    
    /**
     * 检查是否有启用的规则
     */
    fun hasEnabledRules(): Boolean = _enabledRules.value.isNotEmpty()
}
