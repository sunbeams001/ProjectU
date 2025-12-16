package com.projectu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.projectu.shared.domain.model.CacheSize
import com.projectu.shared.domain.model.ImageQuality
import com.projectu.shared.domain.model.DetailImageQuality
import com.projectu.shared.domain.model.NovelDownloadImageQuality
import com.projectu.ui.screens.download.DownloadRulesScreen
import com.projectu.ui.util.CacheDetails
import com.projectu.ui.util.LocalImageCacheManager
import com.projectu.ui.util.rememberPathPicker
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.settings_title
import projectu.composeapp.generated.resources.settings_general
import projectu.composeapp.generated.resources.settings_pixiv
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
import projectu.composeapp.generated.resources.settings_content_filter
import projectu.composeapp.generated.resources.settings_r18_sanity_threshold
import projectu.composeapp.generated.resources.settings_r18_sanity_threshold_desc
import projectu.composeapp.generated.resources.settings_r18_sanity_level_safe
import projectu.composeapp.generated.resources.settings_r18_sanity_level_normal
import projectu.composeapp.generated.resources.settings_r18_sanity_level_suggestive
import projectu.composeapp.generated.resources.settings_r18_sanity_level_r18
import projectu.composeapp.generated.resources.settings_image_quality
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
import projectu.composeapp.generated.resources.settings_bookmark_behavior
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
        
        // 初始化时刷新缓存大小
        LaunchedEffect(Unit) {
            cacheManager.refreshCacheSize()
        }
        
        SettingsScreenContent(
            currentAppLanguage = settings.appLanguage,
            currentPixivLanguage = settings.pixivLanguage,
            currentThemeMode = settings.themeMode,
            isLoggedIn = isLoggedIn,
            currentPhpSessionId = pixivConfig.phpSessionId,
            currentUserId = pixivConfig.getUserId(),
            currentClickBookmarkAction = settings.clickBookmarkAction,
            currentLongPressBookmarkAction = settings.longPressBookmarkAction,
            currentR18SanityThreshold = settings.r18SanityLevelThreshold,
            currentPreferredImageQuality = settings.preferredImageQuality,
            currentDetailImageQuality = settings.detailImageQuality,
            currentNovelDownloadImageQuality = settings.novelDownloadImageQuality,
            currentImageCacheSize = settings.imageCacheSize,
            currentCacheSizeBytes = currentCacheSize,
            cacheDetails = cacheDetails,
            maxCacheSizeBytes = cacheManager.maxCacheSize,
            currentBaseDownloadPath = downloadSettings.baseDownloadPath,
            navigator = navigator,
            onAppLanguageChange = { viewModel.updateAppLanguage(it) },
            onPixivLanguageChange = { viewModel.updatePixivLanguage(it) },
            onThemeModeChange = { viewModel.updateThemeMode(it) },
            onR18SanityThresholdChange = { viewModel.updateR18SanityLevelThreshold(it) },
            onPreferredImageQualityChange = { viewModel.updatePreferredImageQuality(it) },
            onDetailImageQualityChange = { viewModel.updateDetailImageQuality(it) },
            onNovelDownloadImageQualityChange = { viewModel.updateNovelDownloadImageQuality(it) },
            onImageCacheSizeChange = { viewModel.updateImageCacheSize(it) },
            onClearCache = { cacheManager.clearCache() },
            onEditPhpSessionId = { viewModel.editPhpSessionId(it) },
            onLogout = { viewModel.logout(navigator) },
            onClickBookmarkActionChange = { viewModel.updateClickBookmarkAction(it) },
            onLongPressBookmarkActionChange = { viewModel.updateLongPressBookmarkAction(it) },
            onNavigateBack = { navigator.pop() },
            onNavigateToApiTest = { navigator.push(com.projectu.ui.screens.apitest.ApiTestScreen()) },
            onBaseDownloadPathChange = { viewModel.updateBaseDownloadPath(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    currentAppLanguage: AppLanguage,
    currentPixivLanguage: PixivLanguage,
    currentThemeMode: ThemeMode,
    isLoggedIn: Boolean,
    currentPhpSessionId: String,
    currentUserId: Long?,
    currentClickBookmarkAction: com.projectu.shared.domain.model.BookmarkAction,
    currentLongPressBookmarkAction: com.projectu.shared.domain.model.BookmarkAction,
    currentR18SanityThreshold: Int,
    currentPreferredImageQuality: ImageQuality,
    currentDetailImageQuality: DetailImageQuality,
    currentNovelDownloadImageQuality: NovelDownloadImageQuality,
    currentImageCacheSize: CacheSize,
    currentCacheSizeBytes: Long,
    cacheDetails: CacheDetails,
    maxCacheSizeBytes: Long,
    currentBaseDownloadPath: String,
    navigator: cafe.adriel.voyager.navigator.Navigator,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onPixivLanguageChange: (PixivLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onR18SanityThresholdChange: (Int) -> Unit,
    onPreferredImageQualityChange: (ImageQuality) -> Unit,
    onDetailImageQualityChange: (DetailImageQuality) -> Unit,
    onNovelDownloadImageQualityChange: (NovelDownloadImageQuality) -> Unit,
    onImageCacheSizeChange: (CacheSize) -> Unit,
    onClearCache: suspend () -> Unit,
    onEditPhpSessionId: (String) -> Unit,
    onLogout: () -> Unit,
    onClickBookmarkActionChange: (com.projectu.shared.domain.model.BookmarkAction) -> Unit,
    onLongPressBookmarkActionChange: (com.projectu.shared.domain.model.BookmarkAction) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToApiTest: () -> Unit = {},
    onBaseDownloadPathChange: (String) -> Unit
) {
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showPixivLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClickBookmarkActionDialog by remember { mutableStateOf(false) }
    var showLongPressBookmarkActionDialog by remember { mutableStateOf(false) }
    var showR18ThresholdDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showDetailImageQualityDialog by remember { mutableStateOf(false) }
    var showNovelDownloadImageQualityDialog by remember { mutableStateOf(false) }
    var showEditPhpSessionIdDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCacheSizeDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    
    val pathPicker = rememberPathPicker()
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cacheClearedMessage = stringResource(Res.string.settings_cache_cleared)
    
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
            // 通用设置分组
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
            
            // Pixiv 设置分组
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
            
            // 收藏行为设置分组
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_bookmark_behavior))
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
            
            // 内容过滤设置分组
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_content_filter))
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
            
            // 图片质量设置分组
            item {
                SettingsGroupHeader(title = stringResource(Res.string.settings_image_quality))
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
            
            // 缓存管理设置分组
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
            
            // API 测试工具 (开发者选项)
            item {
                SettingsItem(
                    title = stringResource(Res.string.settings_api_test_tool),
                    subtitle = stringResource(Res.string.settings_api_test_subtitle),
                    description = stringResource(Res.string.settings_api_test_desc),
                    onClick = onNavigateToApiTest
                )
            }
            
            // 下载设置分组
            item {
                SettingsGroupHeader(title = "下载设置")
            }
            
            // 下载路径设置
            item {
                SettingsItem(
                    title = "下载路径",
                    subtitle = currentBaseDownloadPath.ifEmpty { "使用默认路径" },
                    description = "设置作品下载保存的基础路径",
                    onClick = {
                        pathPicker.pickDirectory(
                            initialPath = currentBaseDownloadPath.ifEmpty { null }
                        ) { selectedPath ->
                            selectedPath?.let { onBaseDownloadPathChange(it) }
                        }
                    }
                )
            }
            
            // 下载管理
            item {
                SettingsItem(
                    title = "下载管理",
                    subtitle = "管理下载任务",
                    description = "查看和管理所有下载任务",
                    onClick = { navigator.push(com.projectu.ui.screens.download.DownloadScreen()) }
                )
            }
            
            // 下载规则管理
            item {
                SettingsItem(
                    title = "下载路径规则",
                    subtitle = "自定义下载路径规则",
                    description = "根据资源类型、R-18、AI 等条件自动分类保存",
                    onClick = { navigator.push(DownloadRulesScreen()) }
                )
            }
            
            // 账号管理分组
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
            
            // 退出登录
            if (isLoggedIn) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLogoutConfirmDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_logout),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider()
                }
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
    
    // 清空缓存确认对话框
    if (showClearCacheConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            title = { Text(stringResource(Res.string.settings_clear_cache_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_clear_cache_confirm_message)) },
            confirmButton = {
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
                        text = stringResource(Res.string.settings_clear_image_cache),
                        color = MaterialTheme.colorScheme.error
                    )
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
    subtitle: String,
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
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    text = stringResource(Res.string.settings_current_cache_size),
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
