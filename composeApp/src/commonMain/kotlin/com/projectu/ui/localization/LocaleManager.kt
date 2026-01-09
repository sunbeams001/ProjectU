package com.projectu.ui.localization

import androidx.compose.runtime.*
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 语言配置管理器
 * 管理应用的当前语言设置，支持运行时切换和持久化存储
 * 使用 Compose Resources 自动处理多语言
 */
class LocaleManager(
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _currentLanguage = MutableStateFlow(AppLanguage.SIMPLIFIED_CHINESE)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    init {
        // 应用启动时从数据库加载语言设置
        loadLanguageFromDatabase()
    }
    
    /**
     * 从数据库加载语言设置
     */
    private fun loadLanguageFromDatabase() {
        coroutineScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                _currentLanguage.value = settings.appLanguage
                setSystemLocale(settings.appLanguage)
            } catch (e: Exception) {
                // 如果加载失败，使用默认语言
                _currentLanguage.value = AppLanguage.SIMPLIFIED_CHINESE
                setSystemLocale(AppLanguage.SIMPLIFIED_CHINESE)
            } finally {
                // 标记初始化完成
                _isInitialized.value = true
            }
        }
    }
    
    /**
     * 设置当前语言
     * @param language 目标语言
     */
    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        setSystemLocale(language)
        
        // 保存到数据库
        coroutineScope.launch {
            try {
                settingsRepository.updateAppLanguage(language)
            } catch (e: Exception) {
                // 如果保存失败，不影响UI更新
            }
        }
    }
    
    /**
     * 获取当前语言代码（Compose Resources 格式）
     */
    fun getCurrentLocaleCode(): String {
        return when (_currentLanguage.value) {
            AppLanguage.SIMPLIFIED_CHINESE -> "zh-rCN"
            AppLanguage.TRADITIONAL_CHINESE -> "zh-rTW"
            AppLanguage.ENGLISH -> "en"
            AppLanguage.JAPANESE -> "ja"
            AppLanguage.KOREAN -> "ko"
        }
    }
    
    /**
     * 设置系统 Locale
     * Compose Resources 会自动根据 Locale.getDefault() 选择资源
     */
    private fun setSystemLocale(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> java.util.Locale.SIMPLIFIED_CHINESE
            AppLanguage.TRADITIONAL_CHINESE -> java.util.Locale.TRADITIONAL_CHINESE
            AppLanguage.ENGLISH -> java.util.Locale.ENGLISH
            AppLanguage.JAPANESE -> java.util.Locale.JAPANESE
            AppLanguage.KOREAN -> java.util.Locale.KOREAN
        }
        java.util.Locale.setDefault(locale)
    }
}

/**
 * CompositionLocal 用于在 Compose 层级中提供语言管理器
 */
val LocalLocaleManager = staticCompositionLocalOf<LocaleManager> {
    error("LocalLocaleManager not provided")
}

/**
 * 在 Composable 中使用当前语言的 Hook
 */
@Composable
fun rememberCurrentLanguage(): AppLanguage {
    val localeManager = LocalLocaleManager.current
    return localeManager.currentLanguage.collectAsState().value
}

