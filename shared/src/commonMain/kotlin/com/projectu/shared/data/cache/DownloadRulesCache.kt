package com.projectu.shared.data.cache

import com.projectu.shared.data.local.entity.toDownloadRule
import com.projectu.shared.data.local.store.DownloadRulesStore
import com.projectu.shared.domain.model.DownloadRule
import com.projectu.shared.domain.model.DownloadRuleDefaults
import com.projectu.shared.domain.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 下载规则缓存
 * 
 * 设计目标：
 * - 避免每次下载任务都查询数据库
 * - 提供同步的规则匹配方法
 * - 自动订阅 Store 更新，保持缓存一致性
 * 
 * 性能优化：
 * - 规则列表存储在内存中（StateFlow）
 * - findMatchingRule() 时间复杂度：O(n)，n 为规则数量
 * - 规则数量限制：建议最多 20 条，实际匹配耗时 < 1ms
 */
class DownloadRulesCache(
    private val downloadRulesStore: DownloadRulesStore,
    private val baseDownloadPathProvider: () -> String
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * 缓存的规则列表（按优先级排序）
     */
    private val _rules = MutableStateFlow<List<DownloadRule>>(emptyList())
    val rules: StateFlow<List<DownloadRule>> = _rules.asStateFlow()
    
    init {
        // 自动订阅数据库更新
        coroutineScope.launch {
            downloadRulesStore.rules.collect { entities ->
                _rules.value = entities.map { it.toDownloadRule() }
            }
        }
    }
    
    /**
     * 查找匹配的规则（同步方法，用于下载任务）
     * 
     * 策略：
     * 1. 如果有启用的自定义规则，按优先级匹配
     * 2. 如果没有自定义规则或所有自定义规则都不匹配，使用内置默认规则组
     * 
     * @param task 下载任务
     * @return 匹配的规则（保证总能返回有效规则）
     */
    fun findMatchingRule(task: DownloadTask): DownloadRule {
        val enabledRules = _rules.value.filter { it.enabled }
        
        println("[DownloadRulesCache] Finding rule for task: ${task.resourceType}, resourceId: ${task.resourceId}")
        println("[DownloadRulesCache] Custom rules count: ${enabledRules.size}")
        
        // 1. 如果有自定义规则，尝试匹配
        if (enabledRules.isNotEmpty()) {
            println("[DownloadRulesCache] Checking ${enabledRules.size} custom rules...")
            for (rule in enabledRules) {
                println("[DownloadRulesCache] - Rule #${rule.order}: ${rule.resourceTypes}, path: ${rule.targetPath}")
                if (rule.matches(task)) {
                    println("[DownloadRulesCache] ✓ Matched custom rule #${rule.order}, path: ${rule.targetPath}")
                    return rule
                }
            }
            println("[DownloadRulesCache] No custom rule matched, falling back to built-in rules")
        } else {
            println("[DownloadRulesCache] No custom rules, using built-in rules")
        }
        
        // 2. 没有自定义规则，或者所有自定义规则都不匹配
        // 使用内置默认规则组（动态获取当前的基础路径）
        val currentBasePath = baseDownloadPathProvider().ifEmpty { 
            println("[DownloadRulesCache] WARNING: baseDownloadPath is empty, using default: ${DownloadRuleDefaults.DEFAULT_BASE_PATH_ANDROID}")
            DownloadRuleDefaults.DEFAULT_BASE_PATH_ANDROID 
        }
        println("[DownloadRulesCache] Using base path: $currentBasePath")
        
        val builtInRules = DownloadRuleDefaults.getBuiltInRules(currentBasePath)
        println("[DownloadRulesCache] Built-in rules count: ${builtInRules.size}")
        
        for (rule in builtInRules) {
            println("[DownloadRulesCache] - Built-in rule: ${rule.resourceTypes}, basePath: ${rule.targetPath}, subDir: ${rule.subDirectory}")
            if (rule.matches(task)) {
                val relativePath = rule.buildRelativePath(task)
                println("[DownloadRulesCache] ✓ Matched built-in rule: ${rule.resourceTypes}")
                println("[DownloadRulesCache]   - Base path: ${rule.targetPath}")
                println("[DownloadRulesCache]   - Relative path: $relativePath")
                return rule
            }
        }
        
        // 3. 理论上不应该到这里（内置规则应该覆盖所有类型）
        println("[DownloadRulesCache] ERROR: No matching rule found!")
        throw IllegalStateException("No matching rule found for task: ${task.resourceType}")
    }
    
    /**
     * 获取当前规则数量
     */
    fun getRuleCount(): Int = _rules.value.size
    
    /**
     * 判断是否有自定义规则
     */
    fun hasCustomRules(): Boolean = _rules.value.isNotEmpty()
    
    /**
     * 清理资源
     */
    fun clear() {
        coroutineScope.cancel()
    }
}
