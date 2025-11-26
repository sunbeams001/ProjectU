package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现用户页面 ViewModel
 * MVI 架构模式
 */
class DiscoveryUsersViewModel(
    private val userRepository: UserRepository
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryUsersState())
    val state: StateFlow<DiscoveryUsersState> = _state.asStateFlow()
    
    init {
        // 初始加载
        loadUsers()
    }
    
    /**
     * 加载更多用户
     */
    fun loadMore() {
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        
        _state.update { it.copy(isLoadingMore = true) }
        loadUsers(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            it.copy(
                users = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadUsers()
    }
    
    /**
     * 加载用户
     */
    private fun loadUsers(append: Boolean = false) {
        screenModelScope.launch {
            userRepository.getDiscoveryUsers(limit = 20)
                .onSuccess { newUsers ->
                    _state.update { currentState ->
                        // 如果是追加模式，需要去重
                        val updatedUsers = if (append) {
                            val existingIds = currentState.users.map { it.id }.toSet()
                            val uniqueNewUsers = newUsers.filter { it.id !in existingIds }
                            currentState.users + uniqueNewUsers
                        } else {
                            newUsers
                        }
                        
                        currentState.copy(
                            users = updatedUsers,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "未知错误"
                        )
                    }
                }
        }
    }
}

/**
 * 发现用户页面状态
 */
data class DiscoveryUsersState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
