package com.projectu.shared.domain.manager

import com.projectu.shared.domain.model.User
import com.projectu.shared.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户状态管理器
 * 
 * 负责管理当前登录用户的全局状态，在整个应用生命周期内保持单一实例。
 * 在应用启动时初始化一次，之后所有需要用户信息的地方都从这里获取。
 * 
 * 主要功能：
 * - 在应用启动时获取当前用户信息
 * - 提供用户信息的 StateFlow 供各处订阅
 * - 提供便捷的 isPremium 属性用于权限判断
 */
class UserStateManager(
    private val userRepository: UserRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 当前用户是否为高级会员
     */
    val isPremium: Boolean
        get() = _currentUser.value?.isPremium ?: false
    
    /**
     * 初始化用户状态
     * 应在应用启动时调用一次
     */
    fun initialize() {
        if (_isLoading.value) return // 防止重复初始化
        
        _isLoading.value = true
        coroutineScope.launch {
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    _currentUser.value = user
                }
                .onFailure { error ->
                    // 获取失败时不影响应用启动
                    _currentUser.value = null
                }
            _isLoading.value = false
        }
    }
    
    /**
     * 刷新用户信息
     * 当用户信息可能发生变化时调用（如升级会员）
     */
    fun refresh() {
        coroutineScope.launch {
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    _currentUser.value = user
                }
        }
    }
    
    /**
     * 清除用户状态
     * 用户退出登录时调用
     */
    fun clear() {
        _currentUser.value = null
    }
}
