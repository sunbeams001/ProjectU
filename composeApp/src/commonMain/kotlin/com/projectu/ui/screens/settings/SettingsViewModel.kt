package com.projectu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.ui.screens.login.LoginScreen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 设置页面 ViewModel
 * 管理设置相关的状态和业务逻辑
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // 设置状态流
    val settingsState: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings.DEFAULT
        )
    
    /**
     * 更新应用语言
     */
    fun updateAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(language)
        }
    }
    
    /**
     * 更新 Pixiv 语言
     */
    fun updatePixivLanguage(language: PixivLanguage) {
        viewModelScope.launch {
            settingsRepository.updatePixivLanguage(language)
        }
    }
    
    /**
     * 更新主题模式
     */
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }
    
    /**
     * 重置设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettings()
        }
    }
    
    /**
     * 编辑 PHPSESSID
     */
    fun editPhpSessionId(newPhpSessionId: String) {
        viewModelScope.launch {
            authRepository.saveCredentials(newPhpSessionId)
        }
    }
    
    /**
     * 登出
     */
    fun logout(navigator: Navigator) {
        viewModelScope.launch {
            authRepository.clearCredentials()
            // 跳转到登录页面
            navigator.replaceAll(LoginScreen())
        }
    }
}

