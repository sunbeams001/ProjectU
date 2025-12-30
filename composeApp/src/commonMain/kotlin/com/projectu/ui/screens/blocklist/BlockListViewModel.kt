package com.projectu.ui.screens.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import com.projectu.shared.domain.model.ContentScope
import com.projectu.shared.domain.model.TagMatchMode
import com.projectu.shared.domain.repository.BlockRuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 屏蔽列表 ViewModel
 * 管理屏蔽规则的状态和业务逻辑
 */
class BlockListViewModel(
    private val blockRuleRepository: BlockRuleRepository
) : ViewModel() {
    
    // UI 状态
    private val _uiState = MutableStateFlow(BlockListUiState())
    val uiState: StateFlow<BlockListUiState> = _uiState.asStateFlow()
    
    // 所有规则列表（响应式）
    val allRules: StateFlow<List<BlockRule>> = blockRuleRepository.observeAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        // 初始化固定规则（R-18 和 AI）
        initializeFixedRules()
    }
    
    /**
     * 初始化固定规则
     */
    private fun initializeFixedRules() {
        viewModelScope.launch {
            blockRuleRepository.initializeFixedRules()
        }
    }
    
    /**
     * 切换规则启用状态
     */
    fun toggleRuleEnabled(rule: BlockRule) {
        viewModelScope.launch {
            val updatedRule = rule.copy(enabled = !rule.enabled)
            blockRuleRepository.updateRule(updatedRule)
        }
    }
    
    /**
     * 删除规则
     */
    fun deleteRule(rule: BlockRule) {
        viewModelScope.launch {
            blockRuleRepository.deleteRule(rule.id)
        }
    }
    
    /**
     * 显示添加作者规则对话框
     */
    fun showAddAuthorDialog() {
        _uiState.update { 
            it.copy(
                showAddAuthorDialog = true,
                selectedScopes = ContentScope.ALL_SCOPES
            )
        }
    }
    
    /**
     * 隐藏添加作者规则对话框
     */
    fun hideAddAuthorDialog() {
        _uiState.update { 
            it.copy(
                showAddAuthorDialog = false,
                authorIdInput = "",
                selectedScopes = ContentScope.ALL_SCOPES,
                inputError = null
            )
        }
    }
    
    /**
     * 显示添加标签规则对话框
     */
    fun showAddTagDialog() {
        _uiState.update { 
            it.copy(
                showAddTagDialog = true,
                selectedScopes = ContentScope.ALL_SCOPES,
                selectedMatchMode = TagMatchMode.EXACT  // 默认使用精确匹配
            )
        }
    }
    
    /**
     * 隐藏添加标签规则对话框
     */
    fun hideAddTagDialog() {
        _uiState.update { 
            it.copy(
                showAddTagDialog = false,
                tagInput = "",
                selectedScopes = ContentScope.ALL_SCOPES,
                selectedMatchMode = TagMatchMode.EXACT,
                inputError = null
            )
        }
    }
    
    /**
     * 更新作者 ID 输入
     */
    fun updateAuthorIdInput(authorId: String) {
        _uiState.update { 
            it.copy(
                authorIdInput = authorId,
                inputError = null
            )
        }
    }
    
    /**
     * 更新标签输入
     */
    fun updateTagInput(tag: String) {
        _uiState.update { 
            it.copy(
                tagInput = tag,
                inputError = null
            )
        }
    }
    
    /**
     * 切换适用范围选择
     */
    fun toggleScope(scope: ContentScope) {
        _uiState.update { state ->
            val newScopes = if (state.selectedScopes.contains(scope)) {
                state.selectedScopes - scope
            } else {
                state.selectedScopes + scope
            }
            state.copy(
                selectedScopes = newScopes,
                inputError = null
            )
        }
    }
    
    /**     * 切换 Tag 匹配模式
     */
    fun toggleMatchMode(matchMode: TagMatchMode) {
        _uiState.update { it.copy(selectedMatchMode = matchMode) }
    }
    
    /**     * 添加作者屏蔽规则
     */
    fun addAuthorRule() {
        viewModelScope.launch {
            val authorId = _uiState.value.authorIdInput.trim()
            val scopes = _uiState.value.selectedScopes
            
            // 验证输入
            if (authorId.isEmpty()) {
                _uiState.update { it.copy(inputError = InputError.EMPTY) }
                return@launch
            }
            
            // 验证范围
            if (scopes.isEmpty()) {
                _uiState.update { it.copy(inputError = InputError.EMPTY_SCOPE) }
                return@launch
            }
            
            // 检查是否已存在
            val exists = blockRuleRepository.ruleExists(BlockRuleType.AUTHOR_ID, authorId)
            if (exists) {
                _uiState.update { it.copy(inputError = InputError.ALREADY_EXISTS) }
                return@launch
            }
            
            // 添加规则（使用 ID 作为显示名，因为没有用户名信息）
            val rule = BlockRule.createAuthorRule(authorId, authorId, scopes = scopes)
            blockRuleRepository.addRule(rule)
            
            // 关闭对话框
            hideAddAuthorDialog()
        }
    }
    
    /**
     * 添加标签屏蔽规则
     */
    fun addTagRule() {
        viewModelScope.launch {
            val tag = _uiState.value.tagInput.trim()
            val scopes = _uiState.value.selectedScopes
            val matchMode = _uiState.value.selectedMatchMode
            
            // 验证输入
            if (tag.isEmpty()) {
                _uiState.update { it.copy(inputError = InputError.EMPTY) }
                return@launch
            }
            
            // 验证范围
            if (scopes.isEmpty()) {
                _uiState.update { it.copy(inputError = InputError.EMPTY_SCOPE) }
                return@launch
            }
            
            // 检查是否已存在
            val exists = blockRuleRepository.ruleExists(BlockRuleType.TAG, tag)
            if (exists) {
                _uiState.update { it.copy(inputError = InputError.ALREADY_EXISTS) }
                return@launch
            }
            
            // 添加规则
            val rule = BlockRule.createTagRule(
                tag = tag,
                scopes = scopes,
                matchMode = matchMode
            )
            blockRuleRepository.addRule(rule)
            
            // 关闭对话框
            hideAddTagDialog()
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmDialog(rule: BlockRule) {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = true,
                ruleToDelete = rule
            )
        }
    }
    
    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteConfirmDialog() {
        _uiState.update { 
            it.copy(
                showDeleteConfirmDialog = false,
                ruleToDelete = null
            )
        }
    }
    
    /**
     * 确认删除规则
     */
    fun confirmDelete() {
        val rule = _uiState.value.ruleToDelete ?: return
        deleteRule(rule)
        hideDeleteConfirmDialog()
    }
    
    /**
     * 显示编辑规则对话框
     */
    fun showEditDialog(rule: BlockRule) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editingRule = rule,
                authorIdInput = if (rule.type == BlockRuleType.AUTHOR_ID) rule.value else "",
                tagInput = if (rule.type == BlockRuleType.TAG) rule.value else "",
                selectedScopes = rule.scopes,
                selectedMatchMode = rule.matchMode,  // 加载当前匹配模式
                inputError = null
            )
        }
    }
    
    /**
     * 隐藏编辑规则对话框
     */
    fun hideEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                editingRule = null,
                authorIdInput = "",
                tagInput = "",
                selectedScopes = ContentScope.ALL_SCOPES,
                selectedMatchMode = TagMatchMode.EXACT,
                inputError = null
            )
        }
    }
    
    /**
     * 保存编辑的规则
     */
    fun saveEditedRule() {
        viewModelScope.launch {
            val editingRule = _uiState.value.editingRule ?: return@launch
            val scopes = _uiState.value.selectedScopes
            val matchMode = _uiState.value.selectedMatchMode
            
            // 验证范围
            if (scopes.isEmpty()) {
                _uiState.update { it.copy(inputError = InputError.EMPTY_SCOPE) }
                return@launch
            }
            
            // 更新规则
            val updatedRule = editingRule.copy(
                scopes = scopes,
                matchMode = matchMode,  // 更新匹配模式
                updatedAt = System.currentTimeMillis()
            )
            blockRuleRepository.updateRule(updatedRule)
            
            // 关闭对话框
            hideEditDialog()
        }
    }
}

/**
 * UI 状态数据类
 */
data class BlockListUiState(
    val showAddAuthorDialog: Boolean = false,
    val showAddTagDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val authorIdInput: String = "",
    val tagInput: String = "",
    val selectedScopes: Set<ContentScope> = ContentScope.ALL_SCOPES,
    val selectedMatchMode: TagMatchMode = TagMatchMode.EXACT,  // Tag 匹配模式
    val inputError: InputError? = null,
    val ruleToDelete: BlockRule? = null,
    val editingRule: BlockRule? = null
)

/**
 * 输入错误类型
 */
enum class InputError {
    EMPTY,
    EMPTY_SCOPE,
    ALREADY_EXISTS
}
