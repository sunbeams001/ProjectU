package com.projectu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.Navigator
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.AppSettings
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.ImageQuality
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
     * 更新 Pixiv API 语言偏好
     * 支持：简体中文、繁体中文、英语、日语、韩语、泰语、马来语
     * 注意：语言会通过 App.kt 的响应式监听自动同步到 PixivConfig
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
     * 更新 R18 Sanity Level 阈值
     * 阈值范围: 0-9
     * - 0-1: 安全内容
     * - 2-3: 正常内容
     * - 4-5: 暗示性内容
     * - 6-9: R18 内容
     */
    fun updateR18SanityLevelThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.updateR18SanityLevelThreshold(threshold)
        }
    }
    
    /**
     * 更新插画卡片首选图片质量
     */
    fun updatePreferredImageQuality(quality: ImageQuality) {
        viewModelScope.launch {
            settingsRepository.updatePreferredImageQuality(quality)
        }
    }
    
    /**
     * 更新插画详情页首选图片质量
     */
    fun updateDetailImageQuality(quality: com.projectu.shared.domain.model.DetailImageQuality) {
        viewModelScope.launch {
            settingsRepository.updateDetailImageQuality(quality)
        }
    }
    
    /**
     * 更新图片缓存大小
     * 注意：缓存大小变更需要重启应用才能完全生效
     */
    fun updateImageCacheSize(size: CacheSize) {
        viewModelScope.launch {
            settingsRepository.updateImageCacheSize(size)
        }
    }
    
    /**
     * 更新下载基础路径
     */
    fun updateBaseDownloadPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateBaseDownloadPath(path)
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

