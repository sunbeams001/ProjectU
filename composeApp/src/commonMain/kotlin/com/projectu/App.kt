package com.projectu

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.ui.localization.LocalLocaleManager
import com.projectu.ui.localization.LocaleManager
import com.projectu.ui.screens.home.HomeScreen
import com.projectu.ui.theme.AppTheme
import com.projectu.ui.util.ImageCacheManager
import com.projectu.ui.util.LocalImageCacheManager
import com.projectu.ui.util.createImageCacheManager
import com.projectu.ui.util.createImageLoader
import org.koin.compose.koinInject

@Composable
fun App() {
    val localeManager: LocaleManager = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val authRepository: com.projectu.shared.domain.repository.AuthRepository = koinInject()
    
    // 先获取初始设置来确定缓存大小
    var initialSettings by remember { mutableStateOf<com.projectu.shared.data.local.AppSettings?>(null) }
    
    LaunchedEffect(Unit) {
        initialSettings = settingsRepository.getCurrentSettings()
    }
    
    // 等待初始设置加载完成
    val settings = initialSettings ?: return
    
    // 获取平台上下文用于创建 ImageLoader
    val platformContext = LocalPlatformContext.current
    
    // 创建 ImageLoader 和 CacheManager 实例
    val imageLoaderAndCacheManager = remember(settings.imageCacheSize) {
        val loader = createImageLoader(platformContext, settings.imageCacheSize.sizeInBytes)
        val manager = createImageCacheManager(loader, settings.imageCacheSize.sizeInBytes)
        loader to manager
    }
    val (imageLoader, cacheManager) = imageLoaderAndCacheManager

    // 配置 Coil ImageLoader
    setSingletonImageLoaderFactory { imageLoader }

    // 等待语言初始化完成
    val isLanguageInitialized by localeManager.isInitialized.collectAsState()
    
    // 等待登录状态加载完成
    var isLoadingLoginState by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    
    // 等待设置加载完成
    var isLoadingSettings by remember { mutableStateOf(true) }
    var appSettings by remember { mutableStateOf(com.projectu.shared.data.local.AppSettings.DEFAULT) }
    
    LaunchedEffect(Unit) {
        // 先获取初始登录状态
        isLoggedIn = authRepository.isLoggedIn()
        isLoadingLoginState = false
        
        // 然后持续监听状态变化
        authRepository.observeLoginState().collect { loggedIn ->
            isLoggedIn = loggedIn
        }
    }

    // 监听设置变化，同步 App 语言到 LocaleManager
    // 注意：Pixiv 语言已通过 SettingsCache 自动同步，无需手动处理
    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect { settings ->
            appSettings = settings
            isLoadingSettings = false
            // 同步 App 语言到 LocaleManager
            localeManager.setLanguage(settings.appLanguage)
            // 更新缓存管理器的配置
            cacheManager?.updateCacheSize(settings.imageCacheSize)
        }
    }

    // 获取当前语言，用于触发重组
    val currentLanguage by localeManager.currentLanguage.collectAsState()

    // 在加载登录状态、语言初始化或设置加载时显示空白（或可以显示启动画面）
    if (isLoadingLoginState || !isLanguageInitialized || isLoadingSettings) {
        return
    }

    // 使用 key 来强制在语言变化时重新创建整个 UI 树
    key(currentLanguage) {
        
        // 提供 LocaleManager 和 ImageCacheManager 给整个应用
        CompositionLocalProvider(
            LocalLocaleManager provides localeManager,
            LocalImageCacheManager provides cacheManager
        ) {
            // 根据主题模式确定是否使用深色主题
            val systemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (appSettings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemInDarkTheme
            }
            
            AppTheme(darkTheme = useDarkTheme) {
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

