package com.projectu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
import com.projectu.shared.data.local.StartupTab
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.NovelDownloadImageQuality
import com.projectu.shared.domain.model.TranslationEngine
import com.projectu.shared.domain.model.TranslationLanguage
import com.projectu.shared.data.local.FileNameMode
import com.projectu.ui.screens.download.DownloadRulesScreen
import com.projectu.ui.util.CacheDetails
import com.projectu.ui.util.LocalImageCacheManager
import com.projectu.ui.util.rememberPathPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*
import projectu.composeapp.generated.resources.settings_title
import projectu.composeapp.generated.resources.settings_general
import projectu.composeapp.generated.resources.settings_interaction_preferences
import projectu.composeapp.generated.resources.settings_browsing_experience
import projectu.composeapp.generated.resources.settings_pixiv
import projectu.composeapp.generated.resources.settings_content_management
import projectu.composeapp.generated.resources.settings_developer_options
import projectu.composeapp.generated.resources.settings_app_language
import projectu.composeapp.generated.resources.settings_pixiv_language
import projectu.composeapp.generated.resources.settings_pixiv_language_desc
import projectu.composeapp.generated.resources.settings_theme_mode
import projectu.composeapp.generated.resources.settings_select_app_language
import projectu.composeapp.generated.resources.settings_select_pixiv_language
import projectu.composeapp.generated.resources.settings_select_theme
import projectu.composeapp.generated.resources.theme_light
import projectu.composeapp.generated.resources.theme_dark
import projectu.composeapp.generated.resources.theme_system
import projectu.composeapp.generated.resources.settings_default_startup_tab
import projectu.composeapp.generated.resources.settings_default_startup_tab_desc
import projectu.composeapp.generated.resources.settings_select_startup_tab
import projectu.composeapp.generated.resources.startup_tab_last_used
import projectu.composeapp.generated.resources.startup_tab_home
import projectu.composeapp.generated.resources.startup_tab_discovery
import projectu.composeapp.generated.resources.startup_tab_follow_latest
import projectu.composeapp.generated.resources.startup_tab_ranking
import projectu.composeapp.generated.resources.startup_tab_profile
import projectu.composeapp.generated.resources.common_cancel
import projectu.composeapp.generated.resources.common_save
import projectu.composeapp.generated.resources.settings_account
import projectu.composeapp.generated.resources.settings_user_id
import projectu.composeapp.generated.resources.settings_unknown
import projectu.composeapp.generated.resources.settings_phpsessid
import projectu.composeapp.generated.resources.settings_not_logged_in
import projectu.composeapp.generated.resources.settings_phpsessid_desc
import projectu.composeapp.generated.resources.settings_logout
import projectu.composeapp.generated.resources.settings_logout_confirm_title
import projectu.composeapp.generated.resources.settings_logout_confirm_message
import projectu.composeapp.generated.resources.settings_edit_phpsessid
import projectu.composeapp.generated.resources.settings_phpsessid_input_hint
import projectu.composeapp.generated.resources.settings_phpsessid_warning
import projectu.composeapp.generated.resources.login_phpsessid_label
import projectu.composeapp.generated.resources.settings_r18_sanity_threshold
import projectu.composeapp.generated.resources.settings_r18_sanity_threshold_desc
import projectu.composeapp.generated.resources.settings_r18_sanity_level_safe
import projectu.composeapp.generated.resources.settings_r18_sanity_level_normal
import projectu.composeapp.generated.resources.settings_r18_sanity_level_suggestive
import projectu.composeapp.generated.resources.settings_r18_sanity_level_r18
import projectu.composeapp.generated.resources.settings_preferred_image_quality
import projectu.composeapp.generated.resources.settings_preferred_image_quality_desc
import projectu.composeapp.generated.resources.settings_detail_image_quality
import projectu.composeapp.generated.resources.settings_detail_image_quality_desc
import projectu.composeapp.generated.resources.image_quality_square_medium
import projectu.composeapp.generated.resources.image_quality_medium
import projectu.composeapp.generated.resources.image_quality_large
import projectu.composeapp.generated.resources.image_quality_master_1200
import projectu.composeapp.generated.resources.detail_image_quality_square_medium
import projectu.composeapp.generated.resources.detail_image_quality_medium
import projectu.composeapp.generated.resources.detail_image_quality_large
import projectu.composeapp.generated.resources.detail_image_quality_master_1200
import projectu.composeapp.generated.resources.detail_image_quality_original
import projectu.composeapp.generated.resources.settings_novel_download_image_quality
import projectu.composeapp.generated.resources.settings_novel_download_image_quality_desc
import projectu.composeapp.generated.resources.novel_download_image_quality_small
import projectu.composeapp.generated.resources.novel_download_image_quality_medium
import projectu.composeapp.generated.resources.novel_download_image_quality_large
import projectu.composeapp.generated.resources.novel_download_image_quality_original
import projectu.composeapp.generated.resources.settings_cache_management
import projectu.composeapp.generated.resources.settings_image_cache_size
import projectu.composeapp.generated.resources.settings_image_cache_size_desc
import projectu.composeapp.generated.resources.settings_current_cache_size
import projectu.composeapp.generated.resources.settings_clear_image_cache
import projectu.composeapp.generated.resources.settings_clear_cache_confirm_title
import projectu.composeapp.generated.resources.settings_clear_cache_confirm_message
import projectu.composeapp.generated.resources.settings_cache_cleared
import projectu.composeapp.generated.resources.settings_image_cache_cleared
import projectu.composeapp.generated.resources.settings_ugoira_cache_cleared
import projectu.composeapp.generated.resources.settings_cache_info_image
import projectu.composeapp.generated.resources.settings_cache_info_ugoira
import projectu.composeapp.generated.resources.settings_clear_image_button
import projectu.composeapp.generated.resources.settings_clear_ugoira_button
import projectu.composeapp.generated.resources.settings_clear_all_button
import projectu.composeapp.generated.resources.settings_cache_size_change_note
import projectu.composeapp.generated.resources.cache_size_small
import projectu.composeapp.generated.resources.cache_size_medium
import projectu.composeapp.generated.resources.cache_size_large
import projectu.composeapp.generated.resources.cache_size_extra_large
import projectu.composeapp.generated.resources.cache_size_unlimited
import projectu.composeapp.generated.resources.cache_size_unlimited_desc
import projectu.composeapp.generated.resources.cache_type_image
import projectu.composeapp.generated.resources.cache_type_ugoira
import projectu.composeapp.generated.resources.settings_api_test_tool
import projectu.composeapp.generated.resources.settings_api_test_subtitle
import projectu.composeapp.generated.resources.settings_api_test_desc
import projectu.composeapp.generated.resources.login_password_hide
import projectu.composeapp.generated.resources.login_password_show
import projectu.composeapp.generated.resources.settings_phpsessid_empty
import projectu.composeapp.generated.resources.settings_phpsessid_invalid_format
import projectu.composeapp.generated.resources.settings_click_bookmark_action
import projectu.composeapp.generated.resources.settings_click_bookmark_action_desc
import projectu.composeapp.generated.resources.settings_long_press_bookmark_action
import projectu.composeapp.generated.resources.settings_long_press_bookmark_action_desc
import projectu.composeapp.generated.resources.settings_select_bookmark_action
import projectu.composeapp.generated.resources.bookmark_action_public
import projectu.composeapp.generated.resources.bookmark_action_private
import projectu.composeapp.generated.resources.bookmark_action_with_tags
import org.koin.compose.koinInject

/**
 * 设置页面
 * 提供应用各项设置的配置界面
 */
class SettingsScreen : Screen {
    
    @Composable
    override fun Content() {
        val viewModel: SettingsViewModel = koinInject()
        val settings by viewModel.settingsState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val authRepository: com.projectu.shared.domain.repository.AuthRepository = koinInject()
        val cacheManager = LocalImageCacheManager.current
        
        // 观察登录状态和配置
        val isLoggedIn by authRepository.observeLoginState().collectAsState(initial = false)
        val pixivConfig by authRepository.observePixivConfig()
            .collectAsState(initial = com.projectu.shared.data.local.PixivConfig.DEFAULT)
        
        // 获取缓存大小和详情
        val currentCacheSize by cacheManager.currentCacheSize.collectAsState()
        val cacheDetails by cacheManager.cacheDetails.collectAsState()
        
        // 下载设置
        val downloadSettings = settings.downloadSettings
        
        // 更新检查状态
        val updateCheckState by viewModel.updateCheckState.collectAsState()
        val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
        
        // 初始化时刷新缓存大小
        LaunchedEffect(Unit) {
            cacheManager.refreshCacheSize()
        }
        
        // 更新对话框
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        updateCheckState?.let { result ->
            when (result) {
                is com.projectu.shared.domain.model.UpdateCheckResult.HasUpdate -> {
                    UpdateDialog(
                        updateInfo = result.updateInfo,
                        onDownload = { url ->
                            viewModel.resetUpdateCheckState()
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                // 忽略错误
                            }
                        },
                        onDismiss = { viewModel.resetUpdateCheckState() }
                    )
                }
                is com.projectu.shared.domain.model.UpdateCheckResult.NoUpdate -> {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.resetUpdateCheckState() },
                        title = { Text(stringResource(Res.string.settings_check_update)) },
                        text = { Text(stringResource(Res.string.settings_no_update)) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resetUpdateCheckState() }) {
                                Text(stringResource(Res.string.common_ok))
                            }
                        }
                    )
                }
                is com.projectu.shared.domain.model.UpdateCheckResult.Error -> {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { viewModel.resetUpdateCheckState() },
                        title = { Text(stringResource(Res.string.settings_check_update)) },
                        text = { Text(stringResource(Res.string.settings_update_check_error, result.message)) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resetUpdateCheckState() }) {
                                Text(stringResource(Res.string.common_ok))
                            }
                        }
                    )
                }
            }
        }
        
        SettingsScreenContent(
            currentAppLanguage = settings.appLanguage,
            currentPixivLanguage = settings.pixivLanguage,
            currentThemeMode = settings.themeMode,
            currentDefaultStartupTab = settings.defaultStartupTab,
            isLoggedIn = isLoggedIn,
            currentPhpSessionId = pixivConfig.phpSessionId,
            currentUserId = pixivConfig.getUserId(),
            currentClickBookmarkAction = settings.clickBookmarkAction,
            currentLongPressBookmarkAction = settings.longPressBookmarkAction,
            currentR18SanityThreshold = settings.r18SanityLevelThreshold,
            currentPreferredImageQuality = settings.preferredImageQuality,
            currentDetailImageQuality = settings.detailImageQuality,
            currentViewerImageQuality = settings.viewerImageQuality,
            currentNovelDownloadImageQuality = settings.novelDownloadImageQuality,
            currentImageCacheSize = settings.imageCacheSize,
            currentCacheSizeBytes = currentCacheSize,
            cacheDetails = cacheDetails,
            maxCacheSizeBytes = cacheManager.maxCacheSize,
            currentStaggeredGridColumns = settings.staggeredGridColumns,
            currentShowUserProfileBackground = settings.showUserProfileBackground,
            currentBaseDownloadPath = downloadSettings.baseDownloadPath,
            currentFileNameMode = downloadSettings.fileNameMode,
            currentCustomFileNameTemplate = downloadSettings.customFileNameTemplate,
            currentTranslationEngine = settings.translationEngine,
            currentTranslationTargetLanguage = settings.translationTargetLanguage,
            navigator = navigator,
            onAppLanguageChange = { viewModel.updateAppLanguage(it) },
            onPixivLanguageChange = { viewModel.updatePixivLanguage(it) },
            onThemeModeChange = { viewModel.updateThemeMode(it) },
            onDefaultStartupTabChange = { viewModel.updateDefaultStartupTab(it) },
            onR18SanityThresholdChange = { viewModel.updateR18SanityLevelThreshold(it) },
            onPreferredImageQualityChange = { viewModel.updatePreferredImageQuality(it) },
            onDetailImageQualityChange = { viewModel.updateDetailImageQuality(it) },
            onViewerImageQualityChange = { viewModel.updateViewerImageQuality(it) },
            onNovelDownloadImageQualityChange = { viewModel.updateNovelDownloadImageQuality(it) },
            onImageCacheSizeChange = { viewModel.updateImageCacheSize(it) },
            onClearCache = { cacheManager.clearCache() },
            onClearImageCache = { cacheManager.clearImageCache() },
            onClearUgoiraCache = { cacheManager.clearUgoiraCache() },
            onEditPhpSessionId = { viewModel.editPhpSessionId(it) },
            onLogout = { viewModel.logout(navigator) },
            onClickBookmarkActionChange = { viewModel.updateClickBookmarkAction(it) },
            onLongPressBookmarkActionChange = { viewModel.updateLongPressBookmarkAction(it) },
            onStaggeredGridColumnsChange = { viewModel.updateStaggeredGridColumns(it) },
            onShowUserProfileBackgroundChange = { viewModel.updateShowUserProfileBackground(it) },
            onNavigateBack = { navigator.pop() },
            onNavigateToApiTest = { navigator.push(com.projectu.ui.screens.apitest.ApiTestScreen()) },
            onBaseDownloadPathChange = { viewModel.updateBaseDownloadPath(it) },
            onTranslationEngineChange = { viewModel.updateTranslationEngine(it) },
            onTranslationTargetLanguageChange = { viewModel.updateTranslationTargetLanguage(it) },
            currentAppVersion = "${com.projectu.ui.util.AppVersion.VERSION_NAME} (${com.projectu.ui.util.AppVersion.VERSION_CODE})",
            onCheckUpdate = { 
                if (!isCheckingUpdate) {
                    viewModel.checkForUpdate()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    currentAppLanguage: AppLanguage,
    currentPixivLanguage: PixivLanguage,
    currentThemeMode: ThemeMode,
    currentDefaultStartupTab: StartupTab,
    isLoggedIn: Boolean,
    currentPhpSessionId: String,
    currentUserId: Long?,
    currentClickBookmarkAction: com.projectu.shared.domain.model.BookmarkAction,
    currentLongPressBookmarkAction: com.projectu.shared.domain.model.BookmarkAction,
    currentR18SanityThreshold: Int,
    currentPreferredImageQuality: ImageQuality,
    currentDetailImageQuality: DetailImageQuality,
    currentViewerImageQuality: com.projectu.shared.domain.model.ViewerImageQuality,
    currentNovelDownloadImageQuality: NovelDownloadImageQuality,
    currentImageCacheSize: CacheSize,
    currentCacheSizeBytes: Long,
    cacheDetails: CacheDetails,
    maxCacheSizeBytes: Long,
    currentStaggeredGridColumns: Int,
    currentShowUserProfileBackground: Boolean,
    currentBaseDownloadPath: String,
    currentFileNameMode: FileNameMode,
    currentCustomFileNameTemplate: String,
    currentTranslationEngine: TranslationEngine,
    currentTranslationTargetLanguage: TranslationLanguage,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onPixivLanguageChange: (PixivLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultStartupTabChange: (StartupTab) -> Unit,
    onR18SanityThresholdChange: (Int) -> Unit,
    onPreferredImageQualityChange: (ImageQuality) -> Unit,
    onDetailImageQualityChange: (DetailImageQuality) -> Unit,
    onViewerImageQualityChange: (com.projectu.shared.domain.model.ViewerImageQuality) -> Unit,
    onNovelDownloadImageQualityChange: (NovelDownloadImageQuality) -> Unit,
    onImageCacheSizeChange: (CacheSize) -> Unit,
    onClearCache: suspend () -> Unit,
    onClearImageCache: suspend () -> Unit,
    onClearUgoiraCache: suspend () -> Unit,
    onEditPhpSessionId: (String) -> Unit,
    onLogout: () -> Unit,
    onClickBookmarkActionChange: (com.projectu.shared.domain.model.BookmarkAction) -> Unit,
    onLongPressBookmarkActionChange: (com.projectu.shared.domain.model.BookmarkAction) -> Unit,
    onStaggeredGridColumnsChange: (Int) -> Unit,
    onShowUserProfileBackgroundChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToApiTest: () -> Unit = {},
    onBaseDownloadPathChange: (String) -> Unit,
    onTranslationEngineChange: (TranslationEngine) -> Unit,
    onTranslationTargetLanguageChange: (TranslationLanguage) -> Unit,
    currentAppVersion: String,
    onCheckUpdate: () -> Unit
) {
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showPixivLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStartupTabDialog by remember { mutableStateOf(false) }
    var showClickBookmarkActionDialog by remember { mutableStateOf(false) }
    var showLongPressBookmarkActionDialog by remember { mutableStateOf(false) }
    var showR18ThresholdDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showDetailImageQualityDialog by remember { mutableStateOf(false) }
    var showViewerImageQualityDialog by remember { mutableStateOf(false) }
    var showNovelDownloadImageQualityDialog by remember { mutableStateOf(false) }
    var showStaggeredGridColumnsDialog by remember { mutableStateOf(false) }
    var showEditPhpSessionIdDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCacheSizeDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    var showFileNameVariableHelpDialog by remember { mutableStateOf(false) }
    var showTranslationEngineDialog by remember { mutableStateOf(false) }
    var showTranslationTargetLanguageDialog by remember { mutableStateOf(false) }
    
    val pathPicker = rememberPathPicker()
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cacheClearedMessage = stringResource(Res.string.settings_cache_cleared)
    val imageCacheClearedMessage = stringResource(Res.string.settings_image_cache_cleared)
    val ugoiraCacheClearedMessage = stringResource(Res.string.settings_ugoira_cache_cleared)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.settings_title))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 📱 1. 通用设置 (General Settings)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_general))
            }
            
            // 应用语言设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_app_language),
                    subtitle = currentAppLanguage.displayName,
                    onClick = { showAppLanguageDialog = true }
                )
            }
            
            // 主题设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_theme_mode),
                    subtitle = when (currentThemeMode) {
                        ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                        ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                        ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            // 默认启动Tab设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_default_startup_tab),
                    subtitle = stringResource(
                        when (currentDefaultStartupTab) {
                            StartupTab.LAST_USED -> Res.string.startup_tab_last_used
                            StartupTab.HOME -> Res.string.startup_tab_home
                            StartupTab.DISCOVERY -> Res.string.startup_tab_discovery
                            StartupTab.FOLLOW_LATEST -> Res.string.startup_tab_follow_latest
                            StartupTab.RANKING -> Res.string.startup_tab_ranking
                            StartupTab.PROFILE -> Res.string.startup_tab_profile
                        }
                    ),
                    description = stringResource(Res.string.settings_default_startup_tab_desc),
                    onClick = { showStartupTabDialog = true }
                )
            }
            
            // � 2. Pixiv 设置 (Pixiv Settings)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_pixiv))
            }
            
            // Pixiv 语言设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_pixiv_language),
                    subtitle = currentPixivLanguage.displayName,
                    description = stringResource(Res.string.settings_pixiv_language_desc),
                    onClick = { showPixivLanguageDialog = true }
                )
            }
            
            // 🎯 3. 交互偏好 (Interaction Preferences)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_interaction_preferences))
            }
            
            // 点击收藏按钮行为设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_click_bookmark_action),
                    subtitle = when (currentClickBookmarkAction) {
                        com.projectu.shared.domain.model.BookmarkAction.PUBLIC -> 
                            stringResource(Res.string.bookmark_action_public)
                        com.projectu.shared.domain.model.BookmarkAction.PRIVATE -> 
                            stringResource(Res.string.bookmark_action_private)
                        com.projectu.shared.domain.model.BookmarkAction.WITH_TAGS -> 
                            stringResource(Res.string.bookmark_action_with_tags)
                    },
                    description = stringResource(Res.string.settings_click_bookmark_action_desc),
                    onClick = { showClickBookmarkActionDialog = true }
                )
            }
            
            // 长按收藏按钮行为设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_long_press_bookmark_action),
                    subtitle = when (currentLongPressBookmarkAction) {
                        com.projectu.shared.domain.model.BookmarkAction.PUBLIC -> 
                            stringResource(Res.string.bookmark_action_public)
                        com.projectu.shared.domain.model.BookmarkAction.PRIVATE -> 
                            stringResource(Res.string.bookmark_action_private)
                        com.projectu.shared.domain.model.BookmarkAction.WITH_TAGS -> 
                            stringResource(Res.string.bookmark_action_with_tags)
                    },
                    description = stringResource(Res.string.settings_long_press_bookmark_action_desc),
                    onClick = { showLongPressBookmarkActionDialog = true }
                )
            }
            
            // 🖼️ 4. 浏览体验 (Browsing Experience)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_browsing_experience))
            }
            
            // 插画卡片首选图片质量设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_preferred_image_quality),
                    subtitle = when (currentPreferredImageQuality) {
                        ImageQuality.SQUARE_MEDIUM -> stringResource(Res.string.image_quality_square_medium)
                        ImageQuality.MEDIUM -> stringResource(Res.string.image_quality_medium)
                        ImageQuality.LARGE -> stringResource(Res.string.image_quality_large)
                        ImageQuality.MASTER_1200 -> stringResource(Res.string.image_quality_master_1200)
                    },
                    description = stringResource(Res.string.settings_preferred_image_quality_desc),
                    onClick = { showImageQualityDialog = true }
                )
            }
            
            // 插画详情页首选图片质量设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_detail_image_quality),
                    subtitle = when (currentDetailImageQuality) {
                        DetailImageQuality.SQUARE_MEDIUM -> stringResource(Res.string.detail_image_quality_square_medium)
                        DetailImageQuality.MEDIUM -> stringResource(Res.string.detail_image_quality_medium)
                        DetailImageQuality.LARGE -> stringResource(Res.string.detail_image_quality_large)
                        DetailImageQuality.MASTER_1200 -> stringResource(Res.string.detail_image_quality_master_1200)
                        DetailImageQuality.ORIGINAL -> stringResource(Res.string.detail_image_quality_original)
                    },
                    description = stringResource(Res.string.settings_detail_image_quality_desc),
                    onClick = { showDetailImageQualityDialog = true }
                )
            }
            
            // 大图浏览页首选图片质量设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_viewer_image_quality),
                    subtitle = when (currentViewerImageQuality) {
                        com.projectu.shared.domain.model.ViewerImageQuality.MASTER_1200 -> stringResource(Res.string.viewer_image_quality_master_1200)
                        com.projectu.shared.domain.model.ViewerImageQuality.ORIGINAL -> stringResource(Res.string.viewer_image_quality_original)
                    },
                    description = stringResource(Res.string.settings_viewer_image_quality_desc),
                    onClick = { showViewerImageQualityDialog = true }
                )
            }
            
            // 导航设置（跳转到统一设置页面）
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_navigation_config),
                    subtitle = null,
                    description = stringResource(Res.string.settings_navigation_config_desc),
                    onClick = { navigator.push(NavigationPreferencesScreen()) }
                )
            }
            
            // 瀑布流列数设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_staggered_grid_columns),
                    subtitle = when (currentStaggeredGridColumns) {
                        2 -> stringResource(Res.string.settings_grid_columns_2)
                        3 -> stringResource(Res.string.settings_grid_columns_3)
                        4 -> stringResource(Res.string.settings_grid_columns_4)
                        5 -> stringResource(Res.string.settings_grid_columns_5)
                        else -> stringResource(Res.string.common_columns_format, currentStaggeredGridColumns)
                    },
                    description = stringResource(Res.string.settings_staggered_grid_columns_desc),
                    onClick = { showStaggeredGridColumnsDialog = true }
                )
            }
            
            // 用户主页背景图显示设置
            item {
                SettingsSwitchItem(
                    title = stringResource(Res.string.settings_show_user_profile_background),
                    description = stringResource(Res.string.settings_show_user_profile_background_desc),
                    checked = currentShowUserProfileBackground,
                    onCheckedChange = onShowUserProfileBackgroundChange
                )
            }
            
            // Widget管理 (Android专用)
            item { 
                var hasWidgetScreen by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    hasWidgetScreen = try {
                        Class.forName("com.projectu.ui.screens.settings.WidgetManagementScreen")
                        true
                    } catch (e: ClassNotFoundException) {
                        false
                    }
                }
                
                if (hasWidgetScreen) {
                    Column {
                        SettingsGroupHeader(title = stringResource(Res.string.settings_widget))
                        SettingsItem(
                            title = stringResource(Res.string.settings_widget_management),
                            subtitle = stringResource(Res.string.settings_widget_management_desc),
                            onClick = {
                                try {
                                    val screenClass = Class.forName("com.projectu.ui.screens.settings.WidgetManagementScreen")
                                    navigator.push(screenClass.getDeclaredConstructor().newInstance() as Screen)
                                } catch (e: Exception) {
                                    // 忽略错误
                                }
                            }
                        )
                    }
                }
            }
            
            // 📚 5. 小说设置 (Novel Settings)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_novel))
            }
            
            // 小说阅读设置（跳转到独立页面）
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_novel_reading),
                    subtitle = stringResource(Res.string.settings_novel_reading_desc),
                    onClick = { navigator.push(NovelReadingSettingsScreen()) }
                )
            }
            
            // 小说下载首选图片质量设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_novel_download_image_quality),
                    subtitle = when (currentNovelDownloadImageQuality) {
                        NovelDownloadImageQuality.SMALL -> stringResource(Res.string.novel_download_image_quality_small)
                        NovelDownloadImageQuality.MEDIUM -> stringResource(Res.string.novel_download_image_quality_medium)
                        NovelDownloadImageQuality.LARGE -> stringResource(Res.string.novel_download_image_quality_large)
                        NovelDownloadImageQuality.ORIGINAL -> stringResource(Res.string.novel_download_image_quality_original)
                    },
                    description = stringResource(Res.string.settings_novel_download_image_quality_desc),
                    onClick = { showNovelDownloadImageQualityDialog = true }
                )
            }
            
            // 🌐 6. 翻译设置 (Translation Settings)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_translation))
            }
            
            // 翻译引擎设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_translation_engine),
                    subtitle = when (currentTranslationEngine) {
                        TranslationEngine.NONE -> stringResource(Res.string.translation_engine_none)
                        TranslationEngine.GOOGLE_FREE -> stringResource(Res.string.translation_engine_google_free)
                    },
                    description = stringResource(Res.string.settings_translation_engine_desc),
                    onClick = { showTranslationEngineDialog = true }
                )
            }
            
            // 翻译目标语言设置（仅当翻译引擎不是NONE时显示）
            if (currentTranslationEngine != TranslationEngine.NONE) {
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_translation_target_language),
                        subtitle = when (currentTranslationTargetLanguage) {
                            TranslationLanguage.SIMPLIFIED_CHINESE -> stringResource(Res.string.translation_lang_zh_cn)
                            TranslationLanguage.TRADITIONAL_CHINESE -> stringResource(Res.string.translation_lang_zh_tw)
                            TranslationLanguage.ENGLISH -> stringResource(Res.string.translation_lang_en)
                            TranslationLanguage.JAPANESE -> stringResource(Res.string.translation_lang_ja)
                            TranslationLanguage.KOREAN -> stringResource(Res.string.translation_lang_ko)
                            TranslationLanguage.FRENCH -> stringResource(Res.string.translation_lang_fr)
                            TranslationLanguage.GERMAN -> stringResource(Res.string.translation_lang_de)
                            TranslationLanguage.SPANISH -> stringResource(Res.string.translation_lang_es)
                            TranslationLanguage.ITALIAN -> stringResource(Res.string.translation_lang_it)
                            TranslationLanguage.RUSSIAN -> stringResource(Res.string.translation_lang_ru)
                            TranslationLanguage.PORTUGUESE -> stringResource(Res.string.translation_lang_pt)
                            TranslationLanguage.THAI -> stringResource(Res.string.translation_lang_th)
                            TranslationLanguage.VIETNAMESE -> stringResource(Res.string.translation_lang_vi)
                            TranslationLanguage.INDONESIAN -> stringResource(Res.string.translation_lang_id)
                            TranslationLanguage.MALAY -> stringResource(Res.string.translation_lang_ms)
                        },
                        description = stringResource(Res.string.settings_translation_target_language_desc),
                        onClick = { showTranslationTargetLanguageDialog = true }
                    )
                }
            }
            
            // 🔒 7. 内容管理 (Content Management)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_content_management))
            }
            
            // R18 Sanity Level 阈值设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_r18_sanity_threshold),
                    subtitle = when (currentR18SanityThreshold) {
                        in 0..1 -> stringResource(Res.string.settings_r18_sanity_level_safe)
                        in 2..3 -> stringResource(Res.string.settings_r18_sanity_level_normal)
                        in 4..5 -> stringResource(Res.string.settings_r18_sanity_level_suggestive)
                        else -> stringResource(Res.string.settings_r18_sanity_level_r18)
                    } + " ($currentR18SanityThreshold)",
                    description = stringResource(Res.string.settings_r18_sanity_threshold_desc),
                    onClick = { showR18ThresholdDialog = true }
                )
            }
            
            // 屏蔽列表
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_block_list),
                    subtitle = stringResource(Res.string.settings_block_list_desc),
                    onClick = { navigator.push(com.projectu.ui.screens.blocklist.BlockListScreen()) }
                )
            }
            
            // 浏览历史
            item {
                SettingsItem(
                    title = stringResource(Res.string.history_browse_history),
                    subtitle = stringResource(Res.string.history_view_history),
                    description = stringResource(Res.string.history_desc),
                    onClick = { navigator.push(com.projectu.ui.screens.history.BrowseHistoryScreen()) }
                )
            }
            
            // 📥 8. 下载设置 (Download Settings)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_download))
            }
            
            // 下载路径设置
            item {
                val selectDirTitle = stringResource(Res.string.select_download_directory)
                SettingsItem(
                    title = stringResource(Res.string.settings_download_path),
                    subtitle = currentBaseDownloadPath.ifEmpty { stringResource(Res.string.settings_download_path_default) },
                    description = stringResource(Res.string.settings_download_path_desc),
                    onClick = {
                        pathPicker.pickDirectory(
                            title = selectDirTitle,
                            initialPath = currentBaseDownloadPath.ifEmpty { null }
                        ) { selectedPath ->
                            selectedPath?.let { onBaseDownloadPathChange(it) }
                        }
                    }
                )
            }
            
            // 下载路径规则管理
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_download_rules_title),
                    subtitle = stringResource(Res.string.settings_download_rules_subtitle),
                    description = stringResource(Res.string.settings_download_rules_desc),
                    onClick = { navigator.push(DownloadRulesScreen()) }
                )
            }
            
            // 文件命名规则管理
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_file_name_rules_title),
                    subtitle = when (currentFileNameMode) {
                        com.projectu.shared.data.local.FileNameMode.STANDARD -> stringResource(Res.string.settings_file_name_mode_standard)
                        com.projectu.shared.data.local.FileNameMode.CUSTOM -> stringResource(Res.string.settings_file_name_mode_custom, currentCustomFileNameTemplate)
                    },
                    description = stringResource(Res.string.settings_file_name_rules_desc),
                    onClick = { navigator.push(FileNameRulesScreen()) }
                )
            }
            
            // 下载管理
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_download_management_title),
                    subtitle = stringResource(Res.string.settings_download_management_subtitle),
                    description = stringResource(Res.string.settings_download_management_desc),
                    onClick = { navigator.push(com.projectu.ui.screens.download.DownloadScreen()) }
                )
            }
            
            // 💾 9. 缓存管理 (Cache Management)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_cache_management))
            }
            
            // 缓存大小设置
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_image_cache_size),
                    subtitle = when (currentImageCacheSize) {
                        CacheSize.SMALL -> stringResource(Res.string.cache_size_small)
                        CacheSize.MEDIUM -> stringResource(Res.string.cache_size_medium)
                        CacheSize.LARGE -> stringResource(Res.string.cache_size_large)
                        CacheSize.EXTRA_LARGE -> stringResource(Res.string.cache_size_extra_large)
                        CacheSize.UNLIMITED -> stringResource(Res.string.cache_size_unlimited)
                    },
                    description = stringResource(Res.string.settings_image_cache_size_desc),
                    onClick = { showCacheSizeDialog = true }
                )
            }
            
            // 当前缓存大小显示（点击可清空缓存）
            item {
                CacheInfoItem(
                    cacheDetails = cacheDetails,
                    currentCacheSizeBytes = currentCacheSizeBytes,
                    maxCacheSizeBytes = maxCacheSizeBytes,
                    isUnlimited = currentImageCacheSize.isUnlimited,
                    onClearCacheClick = { showClearCacheConfirmDialog = true }
                )
            }
            
            // � 10. 数据管理 (Data Management)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_data_management))
            }
            
            // 备份与恢复
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_backup_restore),
                    subtitle = null,
                    description = stringResource(Res.string.settings_backup_restore_desc),
                    onClick = { navigator.push(BackupRestoreScreen()) }
                )
            }
            
            // 🔧 11. 开发者选项 (Developer Options)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_developer_options))
            }
            
            // API 测试工具
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_api_test_tool),
                    subtitle = stringResource(Res.string.settings_api_test_subtitle),
                    description = stringResource(Res.string.settings_api_test_desc),
                    onClick = onNavigateToApiTest
                )
            }
            
            // � 12. 关于 (About)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_about_app))
            }
            
            // 当前版本 + 检查更新（合并一行）
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCheckUpdate)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.settings_current_version),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = currentAppVersion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(Res.string.settings_check_update),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider()
            }
            
            // Telegram 群组
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                SettingsItem(
                    title = stringResource(Res.string.settings_telegram_group),
                    subtitle = "t.me/ProjectUApp",
                    onClick = {
                        try {
                            uriHandler.openUri("https://t.me/ProjectUApp")
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    }
                )
            }
            
            // GitHub 项目
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                SettingsItem(
                    title = stringResource(Res.string.settings_github_project),
                    subtitle = "github.com/sunbeams001/ProjectU",
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/sunbeams001/ProjectU")
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    }
                )
            }
            
            // 👤 13. 账号 (Account)
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_account))
            }
            
            // 账号信息
            if (isLoggedIn) {
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_user_id),
                        subtitle = currentUserId?.toString() ?: stringResource(Res.string.settings_unknown),
                        onClick = { }
                    )
                }
            }
            
            // 编辑 PHPSESSID
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_phpsessid),
                    subtitle = if (isLoggedIn) {
                        "${currentPhpSessionId.take(8)}...${currentPhpSessionId.takeLast(4)}"
                    } else {
                        stringResource(Res.string.settings_not_logged_in)
                    },
                    description = stringResource(Res.string.settings_phpsessid_desc),
                    onClick = { showEditPhpSessionIdDialog = true }
                )
            }
            
            // 分隔线和间距
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 退出登录（独立、醒目）
            if (isLoggedIn) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLogoutConfirmDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_logout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // 应用语言选择对话框
    if (showAppLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_select_app_language),
            languages = AppLanguage.values().map { it.displayName },
            selectedIndex = AppLanguage.values().indexOf(currentAppLanguage),
            onSelect = { index ->
                onAppLanguageChange(AppLanguage.values()[index])
                showAppLanguageDialog = false
            },
            onDismiss = { showAppLanguageDialog = false }
        )
    }
    
    // Pixiv 语言选择对话框
    if (showPixivLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_select_pixiv_language),
            languages = PixivLanguage.values().map { it.displayName },
            selectedIndex = PixivLanguage.values().indexOf(currentPixivLanguage),
            onSelect = { index ->
                onPixivLanguageChange(PixivLanguage.values()[index])
                showPixivLanguageDialog = false
            },
            onDismiss = { showPixivLanguageDialog = false }
        )
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentThemeMode,
            onSelect = { mode ->
                onThemeModeChange(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // 启动Tab选择对话框
    if (showStartupTabDialog) {
        StartupTabSelectionDialog(
            currentTab = currentDefaultStartupTab,
            onSelect = { tab ->
                onDefaultStartupTabChange(tab)
                showStartupTabDialog = false
            },
            onDismiss = { showStartupTabDialog = false }
        )
    }
    
    // 点击收藏按钮行为选择对话框
    if (showClickBookmarkActionDialog) {
        BookmarkActionSelectionDialog(
            title = stringResource(Res.string.settings_click_bookmark_action),
            currentAction = currentClickBookmarkAction,
            onSelect = { action ->
                onClickBookmarkActionChange(action)
                showClickBookmarkActionDialog = false
            },
            onDismiss = { showClickBookmarkActionDialog = false }
        )
    }
    
    // 长按收藏按钮行为选择对话框
    if (showLongPressBookmarkActionDialog) {
        BookmarkActionSelectionDialog(
            title = stringResource(Res.string.settings_long_press_bookmark_action),
            currentAction = currentLongPressBookmarkAction,
            onSelect = { action ->
                onLongPressBookmarkActionChange(action)
                showLongPressBookmarkActionDialog = false
            },
            onDismiss = { showLongPressBookmarkActionDialog = false }
        )
    }
    
    // R18 阈值设置对话框
    if (showR18ThresholdDialog) {
        R18ThresholdDialog(
            currentThreshold = currentR18SanityThreshold,
            onConfirm = { threshold ->
                onR18SanityThresholdChange(threshold)
                showR18ThresholdDialog = false
            },
            onDismiss = { showR18ThresholdDialog = false }
        )
    }
    
    // 图片质量选择对话框
    if (showImageQualityDialog) {
        ImageQualitySelectionDialog(
            currentQuality = currentPreferredImageQuality,
            onSelect = { quality ->
                onPreferredImageQualityChange(quality)
                showImageQualityDialog = false
            },
            onDismiss = { showImageQualityDialog = false }
        )
    }
    
    // 详情页图片质量选择对话框
    if (showDetailImageQualityDialog) {
        DetailImageQualitySelectionDialog(
            currentQuality = currentDetailImageQuality,
            onSelect = { quality ->
                onDetailImageQualityChange(quality)
                showDetailImageQualityDialog = false
            },
            onDismiss = { showDetailImageQualityDialog = false }
        )
    }
    
    // 大图浏览页图片质量选择对话框
    if (showViewerImageQualityDialog) {
        ViewerImageQualitySelectionDialog(
            currentQuality = currentViewerImageQuality,
            onSelect = { quality ->
                onViewerImageQualityChange(quality)
                showViewerImageQualityDialog = false
            },
            onDismiss = { showViewerImageQualityDialog = false }
        )
    }
    
    // 小说下载图片质量选择对话框
    if (showNovelDownloadImageQualityDialog) {
        NovelDownloadImageQualitySelectionDialog(
            currentQuality = currentNovelDownloadImageQuality,
            onSelect = { quality ->
                onNovelDownloadImageQualityChange(quality)
                showNovelDownloadImageQualityDialog = false
            },
            onDismiss = { showNovelDownloadImageQualityDialog = false }
        )
    }
    
    // 翻译引擎选择对话框
    if (showTranslationEngineDialog) {
        TranslationEngineSelectionDialog(
            currentEngine = currentTranslationEngine,
            onSelect = { engine ->
                onTranslationEngineChange(engine)
                showTranslationEngineDialog = false
            },
            onDismiss = { showTranslationEngineDialog = false }
        )
    }
    
    // 翻译目标语言选择对话框
    if (showTranslationTargetLanguageDialog) {
        TranslationTargetLanguageSelectionDialog(
            currentLanguage = currentTranslationTargetLanguage,
            onSelect = { language ->
                onTranslationTargetLanguageChange(language)
                showTranslationTargetLanguageDialog = false
            },
            onDismiss = { showTranslationTargetLanguageDialog = false }
        )
    }
    
    // 编辑 PHPSESSID 对话框
    if (showEditPhpSessionIdDialog) {
        EditPhpSessionIdDialog(
            currentValue = currentPhpSessionId,
            onConfirm = { newValue ->
                onEditPhpSessionId(newValue)
                showEditPhpSessionIdDialog = false
            },
            onDismiss = { showEditPhpSessionIdDialog = false }
        )
    }
    
    // 退出登录确认对话框
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text(stringResource(Res.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogout()
                        showLogoutConfirmDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.settings_logout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    
    // 缓存大小选择对话框
    if (showCacheSizeDialog) {
        CacheSizeSelectionDialog(
            currentSize = currentImageCacheSize,
            onSelect = { size ->
                onImageCacheSizeChange(size)
                showCacheSizeDialog = false
            },
            onDismiss = { showCacheSizeDialog = false }
        )
    }
    
    // 瀑布流列数选择对话框
    if (showStaggeredGridColumnsDialog) {
        AlertDialog(
            onDismissRequest = { showStaggeredGridColumnsDialog = false },
            title = { Text(stringResource(Res.string.settings_select_grid_columns)) },
            text = {
                LazyColumn {
                    items(listOf(2, 3, 4, 5)) { columns ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStaggeredGridColumnsChange(columns)
                                    showStaggeredGridColumnsDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentStaggeredGridColumns == columns,
                                    onClick = {
                                        onStaggeredGridColumnsChange(columns)
                                        showStaggeredGridColumnsDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = when (columns) {
                                        2 -> stringResource(Res.string.settings_grid_columns_2)
                                        3 -> stringResource(Res.string.settings_grid_columns_3)
                                        4 -> stringResource(Res.string.settings_grid_columns_4)
                                        5 -> stringResource(Res.string.settings_grid_columns_5)
                                        else -> stringResource(Res.string.common_columns_format, columns)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStaggeredGridColumnsDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
    
    // 清空缓存确认对话框
    if (showClearCacheConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            title = { Text(stringResource(Res.string.settings_clear_cache_confirm_title)) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.settings_clear_cache_confirm_message))
                    Text(
                        text = stringResource(Res.string.settings_cache_info_image, formatCacheSize(cacheDetails.imageCacheSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.settings_cache_info_ugoira, formatCacheSize(cacheDetails.ugoiraCacheSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 清空图片缓存
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                onClearImageCache()
                                snackbarHostState.showSnackbar(imageCacheClearedMessage)
                            }
                            showClearCacheConfirmDialog = false
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_clear_image_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    // 清空动图缓存
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                onClearUgoiraCache()
                                snackbarHostState.showSnackbar(ugoiraCacheClearedMessage)
                            }
                            showClearCacheConfirmDialog = false
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_clear_ugoira_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    // 清空全部缓存
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                onClearCache()
                                snackbarHostState.showSnackbar(cacheClearedMessage)
                            }
                            showClearCacheConfirmDialog = false
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_clear_all_button),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

/**
 * 格式化缓存大小显示
 */
private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

/**
 * 设置分组标题
 */
@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

/**
 * 带开关的设置项
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
    HorizontalDivider()
}

/**
 * 语言选择对话框
 */
@Composable
private fun LanguageSelectionDialog(
    title: String,
    languages: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(languages) { language ->
                    val index = languages.indexOf(language)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = { onSelect(index) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = language,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 主题选择对话框
 */
@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        ThemeMode.LIGHT to stringResource(Res.string.theme_light),
        ThemeMode.DARK to stringResource(Res.string.theme_dark),
        ThemeMode.SYSTEM to stringResource(Res.string.theme_system)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_select_theme)) },
        text = {
            Column {
                themes.forEach { (mode, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == currentTheme,
                                onClick = { onSelect(mode) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 启动Tab选择对话框
 */
@Composable
private fun StartupTabSelectionDialog(
    currentTab: StartupTab,
    onSelect: (StartupTab) -> Unit,
    onDismiss: () -> Unit
) {
    val tabs = listOf(
        StartupTab.LAST_USED to stringResource(Res.string.startup_tab_last_used),
        StartupTab.HOME to stringResource(Res.string.startup_tab_home),
        StartupTab.DISCOVERY to stringResource(Res.string.startup_tab_discovery),
        StartupTab.FOLLOW_LATEST to stringResource(Res.string.startup_tab_follow_latest),
        StartupTab.RANKING to stringResource(Res.string.startup_tab_ranking),
        StartupTab.PROFILE to stringResource(Res.string.startup_tab_profile)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_select_startup_tab)) },
        text = {
            Column {
                tabs.forEach { (tab, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tab) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tab == currentTab,
                                onClick = { onSelect(tab) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}


/**
 * 编辑 PHPSESSID 对话框
 */
@Composable
private fun EditPhpSessionIdDialog(
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var phpSessionId by remember { mutableStateOf(currentValue) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_edit_phpsessid)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.settings_phpsessid_input_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = phpSessionId,
                    onValueChange = {
                        phpSessionId = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(Res.string.login_phpsessid_label)) },
                    placeholder = { Text("12345678_xxxxxxxxxxxx") },
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) 
                                    Icons.Default.Visibility 
                                else 
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) stringResource(Res.string.login_password_hide) else stringResource(Res.string.login_password_show)
                            )
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (currentValue.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.settings_phpsessid_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val phpsessidEmptyError = stringResource(Res.string.settings_phpsessid_empty)
            val phpsessidInvalidFormatError = stringResource(Res.string.settings_phpsessid_invalid_format)
            TextButton(
                onClick = {
                    if (phpSessionId.isBlank()) {
                        errorMessage = phpsessidEmptyError
                    } else if (!phpSessionId.contains("_")) {
                        errorMessage = phpsessidInvalidFormatError
                    } else {
                        val parts = phpSessionId.split("_")
                        if (parts.size < 2 || parts[0].toLongOrNull() == null) {
                            errorMessage = phpsessidInvalidFormatError
                        } else {
                            onConfirm(phpSessionId)
                        }
                    }
                }
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * R18 Sanity Level 阈值设置对话框
 */
@Composable
private fun R18ThresholdDialog(
    currentThreshold: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_r18_sanity_threshold)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.settings_r18_sanity_threshold_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = threshold.toFloat(),
                            onValueChange = { value ->
                                // 四舍五入到最近的整数，确保所有整数值都能选中
                                threshold = kotlin.math.round(value).toInt()
                            },
                            valueRange = 0f..9f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // 阈值描述
                        Text(
                            text = when (threshold) {
                                in 0..1 -> stringResource(Res.string.settings_r18_sanity_level_safe)
                                in 2..3 -> stringResource(Res.string.settings_r18_sanity_level_normal)
                                in 4..5 -> stringResource(Res.string.settings_r18_sanity_level_suggestive)
                                else -> stringResource(Res.string.settings_r18_sanity_level_r18)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Text(
                        text = threshold.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(threshold) }
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 图片质量选择对话框
 */
@Composable
private fun ImageQualitySelectionDialog(
    currentQuality: ImageQuality,
    onSelect: (ImageQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        ImageQuality.SQUARE_MEDIUM to stringResource(Res.string.image_quality_square_medium),
        ImageQuality.MEDIUM to stringResource(Res.string.image_quality_medium),
        ImageQuality.LARGE to stringResource(Res.string.image_quality_large),
        ImageQuality.MASTER_1200 to stringResource(Res.string.image_quality_master_1200)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_preferred_image_quality)) },
        text = {
            Column {
                qualities.forEach { (quality, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == currentQuality,
                                onClick = { onSelect(quality) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 详情页图片质量选择对话框
 */
@Composable
private fun DetailImageQualitySelectionDialog(
    currentQuality: DetailImageQuality,
    onSelect: (DetailImageQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        DetailImageQuality.SQUARE_MEDIUM to stringResource(Res.string.detail_image_quality_square_medium),
        DetailImageQuality.MEDIUM to stringResource(Res.string.detail_image_quality_medium),
        DetailImageQuality.LARGE to stringResource(Res.string.detail_image_quality_large),
        DetailImageQuality.MASTER_1200 to stringResource(Res.string.detail_image_quality_master_1200),
        DetailImageQuality.ORIGINAL to stringResource(Res.string.detail_image_quality_original)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_detail_image_quality)) },
        text = {
            Column {
                qualities.forEach { (quality, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == currentQuality,
                                onClick = { onSelect(quality) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 大图浏览页图片质量选择对话框
 */
@Composable
private fun ViewerImageQualitySelectionDialog(
    currentQuality: com.projectu.shared.domain.model.ViewerImageQuality,
    onSelect: (com.projectu.shared.domain.model.ViewerImageQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        com.projectu.shared.domain.model.ViewerImageQuality.MASTER_1200 to stringResource(Res.string.viewer_image_quality_master_1200),
        com.projectu.shared.domain.model.ViewerImageQuality.ORIGINAL to stringResource(Res.string.viewer_image_quality_original)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_viewer_image_quality)) },
        text = {
            Column {
                qualities.forEach { (quality, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == currentQuality,
                                onClick = { onSelect(quality) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 小说下载图片质量选择对话框
 */
@Composable
private fun NovelDownloadImageQualitySelectionDialog(
    currentQuality: NovelDownloadImageQuality,
    onSelect: (NovelDownloadImageQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val qualities = listOf(
        NovelDownloadImageQuality.SMALL to stringResource(Res.string.novel_download_image_quality_small),
        NovelDownloadImageQuality.MEDIUM to stringResource(Res.string.novel_download_image_quality_medium),
        NovelDownloadImageQuality.LARGE to stringResource(Res.string.novel_download_image_quality_large),
        NovelDownloadImageQuality.ORIGINAL to stringResource(Res.string.novel_download_image_quality_original)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_novel_download_image_quality)) },
        text = {
            Column {
                qualities.forEach { (quality, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(quality) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = quality == currentQuality,
                                onClick = { onSelect(quality) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 翻译引擎选择对话框
 */
@Composable
private fun TranslationEngineSelectionDialog(
    currentEngine: TranslationEngine,
    onSelect: (TranslationEngine) -> Unit,
    onDismiss: () -> Unit
) {
    val engines = listOf(
        TranslationEngine.NONE to stringResource(Res.string.translation_engine_none),
        TranslationEngine.GOOGLE_FREE to stringResource(Res.string.translation_engine_google_free)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_select_translation_engine)) },
        text = {
            Column {
                engines.forEach { (engine, name) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(engine) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = engine == currentEngine,
                                onClick = { onSelect(engine) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 翻译目标语言选择对话框
 */
@Composable
private fun TranslationTargetLanguageSelectionDialog(
    currentLanguage: TranslationLanguage,
    onSelect: (TranslationLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        TranslationLanguage.SIMPLIFIED_CHINESE to stringResource(Res.string.translation_lang_zh_cn),
        TranslationLanguage.TRADITIONAL_CHINESE to stringResource(Res.string.translation_lang_zh_tw),
        TranslationLanguage.ENGLISH to stringResource(Res.string.translation_lang_en),
        TranslationLanguage.JAPANESE to stringResource(Res.string.translation_lang_ja),
        TranslationLanguage.KOREAN to stringResource(Res.string.translation_lang_ko),
        TranslationLanguage.FRENCH to stringResource(Res.string.translation_lang_fr),
        TranslationLanguage.GERMAN to stringResource(Res.string.translation_lang_de),
        TranslationLanguage.SPANISH to stringResource(Res.string.translation_lang_es),
        TranslationLanguage.ITALIAN to stringResource(Res.string.translation_lang_it),
        TranslationLanguage.RUSSIAN to stringResource(Res.string.translation_lang_ru),
        TranslationLanguage.PORTUGUESE to stringResource(Res.string.translation_lang_pt),
        TranslationLanguage.THAI to stringResource(Res.string.translation_lang_th),
        TranslationLanguage.VIETNAMESE to stringResource(Res.string.translation_lang_vi),
        TranslationLanguage.INDONESIAN to stringResource(Res.string.translation_lang_id),
        TranslationLanguage.MALAY to stringResource(Res.string.translation_lang_ms)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_select_translation_target_language)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(languages.size) { index ->
                    val (language, name) = languages[index]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == currentLanguage,
                                onClick = { onSelect(language) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 缓存大小选择对话框
 */
@Composable
private fun CacheSizeSelectionDialog(
    currentSize: CacheSize,
    onSelect: (CacheSize) -> Unit,
    onDismiss: () -> Unit
) {
    val unlimitedDesc = stringResource(Res.string.cache_size_unlimited_desc)
    val sizes = listOf(
        CacheSize.SMALL to stringResource(Res.string.cache_size_small) to null,
        CacheSize.MEDIUM to stringResource(Res.string.cache_size_medium) to null,
        CacheSize.LARGE to stringResource(Res.string.cache_size_large) to null,
        CacheSize.EXTRA_LARGE to stringResource(Res.string.cache_size_extra_large) to null,
        CacheSize.UNLIMITED to stringResource(Res.string.cache_size_unlimited) to unlimitedDesc
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_image_cache_size)) },
        text = {
            Column {
                sizes.forEach { (sizeAndName, description) ->
                    val (size, name) = sizeAndName
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(size) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = size == currentSize,
                                onClick = { onSelect(size) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (description != null) {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 提示信息
                Text(
                    text = stringResource(Res.string.settings_cache_size_change_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}

/**
 * 缓存信息显示项（合并当前缓存大小和清空缓存功能）
 * 显示总缓存大小和各类型缓存详情
 */
@Composable
private fun CacheInfoItem(
    cacheDetails: CacheDetails,
    currentCacheSizeBytes: Long,
    maxCacheSizeBytes: Long,
    isUnlimited: Boolean,
    onClearCacheClick: () -> Unit
) {
    val imageLabel = stringResource(Res.string.cache_type_image)
    val ugoiraLabel = stringResource(Res.string.cache_type_ugoira)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClearCacheClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.settings_clear_cache),
                    style = MaterialTheme.typography.bodyLarge
                )
                // 总缓存大小
                Text(
                    text = if (isUnlimited) {
                        formatCacheSize(currentCacheSizeBytes)
                    } else {
                        formatCacheSize(currentCacheSizeBytes) + " / " + formatCacheSize(maxCacheSizeBytes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 缓存类型详情
                if (cacheDetails.imageCacheSize > 0 || cacheDetails.ugoiraCacheSize > 0) {
                    val detailParts = buildList {
                        if (cacheDetails.imageCacheSize > 0) {
                            add("$imageLabel: ${formatCacheSize(cacheDetails.imageCacheSize)}")
                        }
                        if (cacheDetails.ugoiraCacheSize > 0) {
                            add("$ugoiraLabel: ${formatCacheSize(cacheDetails.ugoiraCacheSize)}")
                        }
                    }
                    Text(
                        text = detailParts.joinToString(" | "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = stringResource(Res.string.settings_clear_image_cache),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    HorizontalDivider()
}

/**
 * 收藏行为选择对话框
 */
@Composable
private fun BookmarkActionSelectionDialog(
    title: String,
    currentAction: com.projectu.shared.domain.model.BookmarkAction,
    onSelect: (com.projectu.shared.domain.model.BookmarkAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                com.projectu.shared.domain.model.BookmarkAction.values().forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(action) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == currentAction,
                            onClick = { onSelect(action) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (action) {
                                com.projectu.shared.domain.model.BookmarkAction.PUBLIC -> 
                                    stringResource(Res.string.bookmark_action_public)
                                com.projectu.shared.domain.model.BookmarkAction.PRIVATE -> 
                                    stringResource(Res.string.bookmark_action_private)
                                com.projectu.shared.domain.model.BookmarkAction.WITH_TAGS -> 
                                    stringResource(Res.string.bookmark_action_with_tags)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    )
}
/**
 * 更新对话框
 */
@Composable
private fun UpdateDialog(
    updateInfo: com.projectu.shared.domain.model.UpdateInfo,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_update_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 版本信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.settings_latest_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = updateInfo.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                
                // 文件大小
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.settings_update_file_size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(updateInfo.fileSize),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 发布时间
                Text(
                    text = "${stringResource(Res.string.settings_update_published_at)}: ${formatPublishedDate(updateInfo.publishedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 更新内容
                if (updateInfo.releaseNotes.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(Res.string.settings_update_content),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDownload(updateInfo.downloadUrl) }) {
                Text(stringResource(Res.string.settings_update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_update_later))
            }
        }
    )
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * 格式化发布日期
 */
private fun formatPublishedDate(isoDate: String): String {
    return try {
        // 简单处理，只取日期部分
        isoDate.substringBefore('T')
    } catch (e: Exception) {
        isoDate
    }
}