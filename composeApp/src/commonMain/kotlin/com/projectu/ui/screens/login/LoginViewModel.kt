package com.projectu.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录屏幕 ViewModel
 * 采用 MVI 架构模式
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginScreenState())
    val state: StateFlow<LoginScreenState> = _state.asStateFlow()
    
    /**
     * 处理用户意图
     */
    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.PhpSessionIdChanged -> {
                _state.update { it.copy(phpSessionId = intent.value, errorMessage = null) }
            }
            
            is LoginIntent.LoginClicked -> {
                performLogin()
            }
            
            is LoginIntent.SwitchLoginMode -> {
                _state.update { it.copy(loginMode = intent.mode, errorMessage = null) }
            }
            
            is LoginIntent.ToggleHelpDialog -> {
                _state.update { it.copy(showHelpDialog = intent.show) }
            }
            
            is LoginIntent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }
    
    /**
     * 执行登录
     */
    private fun performLogin() {
        val currentState = _state.value
        
        // 验证输入
        if (currentState.phpSessionId.isBlank()) {
            _state.update { it.copy(errorMessage = "请输入 PHPSESSID") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // 保存凭据
                val result = authRepository.saveCredentials(currentState.phpSessionId)
                
                result.fold(
                    onSuccess = {
                        // 登录成功，状态会由导航处理
                        _state.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "登录失败，请检查 PHPSESSID 格式"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "登录失败: ${e.message}"
                    )
                }
            }
        }
    }
}
