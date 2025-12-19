package com.projectu.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.data.repository.DownloadRulesRepository
import com.projectu.shared.domain.model.AuthorGrouping
import com.projectu.shared.domain.model.DownloadRule
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.ResourceType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 下载规则管理 ViewModel
 */
class DownloadRulesViewModel(
    private val downloadRulesRepository: DownloadRulesRepository
) : ViewModel() {
    
    /**
     * 规则列表（按优先级排序）
     */
    val rules: StateFlow<List<DownloadRule>> = downloadRulesRepository.getRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * UI 状态
     */
    private val _uiState = MutableStateFlow(DownloadRulesUiState())
    val uiState: StateFlow<DownloadRulesUiState> = _uiState.asStateFlow()
    
    /**
     * 添加规则
     */
    fun addRule(
        resourceTypes: Set<ResourceType>,
        r18Filter: FilterType,
        aiFilter: FilterType,
        authorGrouping: AuthorGrouping,
        targetPath: String
    ) {
        viewModelScope.launch {
            try {
                val newRule = DownloadRule(
                    id = 0L, // 新规则，ID 由数据库自动生成
                    order = rules.value.size, // 默认排在最后
                    resourceTypes = resourceTypes,
                    r18Filter = r18Filter,
                    aiFilter = aiFilter,
                    authorGrouping = authorGrouping,
                    targetPath = targetPath,
                    enabled = true
                )
                downloadRulesRepository.addRule(newRule)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add rule") }
            }
        }
    }
    
    /**
     * 更新规则
     */
    fun updateRule(rule: DownloadRule) {
        viewModelScope.launch {
            try {
                downloadRulesRepository.updateRule(rule)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update rule") }
            }
        }
    }
    
    /**
     * 删除规则
     */
    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                downloadRulesRepository.deleteRule(ruleId)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete rule") }
            }
        }
    }
    
    /**
     * 批量更新规则顺序（用于拖拽排序）
     */
    fun updateRulesOrder(rules: List<DownloadRule>) {
        viewModelScope.launch {
            try {
                // 重新分配顺序号
                val reorderedRules = rules.mapIndexed { index, rule ->
                    rule.copy(order = index)
                }
                downloadRulesRepository.updateRulesOrder(reorderedRules)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to reorder rules") }
            }
        }
    }
    
    /**
     * 启用/禁用规则
     */
    fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                downloadRulesRepository.setRuleEnabled(ruleId, enabled)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to toggle rule") }
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI 状态
 */
data class DownloadRulesUiState(
    val error: String? = null
)
