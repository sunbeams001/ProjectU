package com.projectu

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.ui.localization.LocalLocaleManager
import com.projectu.ui.localization.LocaleManager
import com.projectu.ui.screens.home.HomeScreen
import com.projectu.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val localeManager: LocaleManager = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val authRepository: com.projectu.shared.domain.repository.AuthRepository = koinInject()

    // 检查登录状态
    val isLoggedIn by authRepository.observeLoginState().collectAsState(initial = false)

    // 监听设置变化，同步语言到 LocaleManager
    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect { settings ->
            localeManager.setLanguage(settings.appLanguage)
        }
    }

    // 获取当前语言，用于触发重组
    val currentLanguage by localeManager.currentLanguage.collectAsState()

    // 使用 key 来强制在语言变化时重新创建整个 UI 树
    key(currentLanguage) {
        // 提供 LocaleManager 给整个应用
        CompositionLocalProvider(LocalLocaleManager provides localeManager) {
            AppTheme {
                // 根据登录状态决定初始页面
                val initialScreen = if (isLoggedIn) {
                    HomeScreen()
                } else {
                    com.projectu.ui.screens.login.LoginScreen()
                }

                Navigator(initialScreen) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}

