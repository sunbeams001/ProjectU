package com.projectu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingContentModeConfig
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.RankingNavigationPreferences
import com.projectu.shared.domain.model.DiscoveryNavigationPreferences
import com.projectu.shared.domain.model.FollowLatestNavigationPreferences
import com.projectu.ui.screens.discovery.DiscoveryContentType
import com.projectu.ui.screens.followlatest.FollowLatestContentType
import com.projectu.ui.screens.followlatest.FollowLatestMode
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 统一的导航偏好设置页面
 * 使用Tab切换管理排行榜、发现页、动态页三个页面的导航项设置
 */
class NavigationPreferencesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository = koinInject()
        
        // 订阅设置
        val settings by settingsRepository.getSettings()
            .collectAsState(initial = com.projectu.shared.data.local.AppSettings.DEFAULT)
        
        val scope = rememberCoroutineScope()
        
        NavigationPreferencesContent(
            rankingPreferences = settings.rankingNavigationPreferences,
            discoveryPreferences = settings.discoveryNavigationPreferences,
            followLatestPreferences = settings.followLatestNavigationPreferences,
            onUpdateRankingPreferences = { newPreferences ->
                scope.launch {
                    settingsRepository.updateRankingNavigationPreferences(newPreferences)
                }
            },
            onUpdateDiscoveryPreferences = { newPreferences ->
                scope.launch {
                    settingsRepository.updateDiscoveryNavigationPreferences(newPreferences)
                }
            },
            onUpdateFollowLatestPreferences = { newPreferences ->
                scope.launch {
                    settingsRepository.updateFollowLatestNavigationPreferences(newPreferences)
                }
            },
            onResetRankingToDefault = {
                scope.launch {
                    settingsRepository.updateRankingNavigationPreferences(
                        RankingNavigationPreferences.DEFAULT
                    )
                }
            },
            onResetDiscoveryToDefault = {
                scope.launch {
                    settingsRepository.updateDiscoveryNavigationPreferences(
                        DiscoveryNavigationPreferences.DEFAULT
                    )
                }
            },
            onResetFollowLatestToDefault = {
                scope.launch {
                    settingsRepository.updateFollowLatestNavigationPreferences(
                        FollowLatestNavigationPreferences.DEFAULT
                    )
                }
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationPreferencesContent(
    rankingPreferences: RankingNavigationPreferences,
    discoveryPreferences: DiscoveryNavigationPreferences,
    followLatestPreferences: FollowLatestNavigationPreferences,
    onUpdateRankingPreferences: (RankingNavigationPreferences) -> Unit,
    onUpdateDiscoveryPreferences: (DiscoveryNavigationPreferences) -> Unit,
    onUpdateFollowLatestPreferences: (FollowLatestNavigationPreferences) -> Unit,
    onResetRankingToDefault: () -> Unit,
    onResetDiscoveryToDefault: () -> Unit,
    onResetFollowLatestToDefault: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_navigation_config)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab导航栏
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(Res.string.nav_ranking)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(Res.string.nav_discovery)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(stringResource(Res.string.nav_follow_latest)) }
                )
            }
            
            // Tab内容区域
            when (selectedTabIndex) {
                0 -> RankingPreferencesTab(
                    preferences = rankingPreferences,
                    onUpdatePreferences = onUpdateRankingPreferences,
                    onResetToDefault = onResetRankingToDefault
                )
                1 -> DiscoveryPreferencesTab(
                    preferences = discoveryPreferences,
                    onUpdatePreferences = onUpdateDiscoveryPreferences,
                    onResetToDefault = onResetDiscoveryToDefault
                )
                2 -> FollowLatestPreferencesTab(
                    preferences = followLatestPreferences,
                    onUpdatePreferences = onUpdateFollowLatestPreferences,
                    onResetToDefault = onResetFollowLatestToDefault
                )
            }
        }
    }
}

/**
 * 排行榜导航偏好设置Tab
 */
@Composable
fun RankingPreferencesTab(
    preferences: RankingNavigationPreferences,
    onUpdatePreferences: (RankingNavigationPreferences) -> Unit,
    onResetToDefault: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 说明文字
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(Res.string.nav_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 内容类型列表
        RankingContent.entries.forEach { content ->
            item {
                RankingContentTypeItem(
                    content = content,
                    isEnabled = preferences.isContentTypeEnabled(content),
                    onToggle = {
                        val newPreferences = preferences.toggleContentType(content)
                        if (newPreferences.getEnabledContentTypes().isNotEmpty()) {
                            onUpdatePreferences(newPreferences)
                        }
                    }
                )
            }
            
            if (preferences.isContentTypeEnabled(content)) {
                val supportedModes = RankingContentModeConfig.getSupportedModes(content)
                supportedModes.forEach { mode ->
                    item {
                        RankingModeItem(
                            mode = mode,
                            isEnabled = mode in preferences.getEnabledModes(content),
                            onToggle = {
                                val newPreferences = preferences.toggleMode(content, mode)
                                if (newPreferences.getEnabledModes(content).isNotEmpty()) {
                                    onUpdatePreferences(newPreferences)
                                }
                            }
                        )
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        
        // 恢复默认按钮
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(Res.string.nav_settings_reset))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = {
                onResetToDefault()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

/**
 * 发现页导航偏好设置Tab
 */
@Composable
fun DiscoveryPreferencesTab(
    preferences: DiscoveryNavigationPreferences,
    onUpdatePreferences: (DiscoveryNavigationPreferences) -> Unit,
    onResetToDefault: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 说明文字
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(Res.string.nav_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 内容类型列表
        DiscoveryContentType.entries.forEach { contentType ->
            item {
                DiscoveryContentTypeItem(
                    contentType = contentType,
                    isEnabled = preferences.isContentTypeEnabled(contentType.name),
                    onToggle = {
                        val newPreferences = preferences.toggleContentType(contentType.name)
                        if (newPreferences.getFilteredContentTypes().isNotEmpty()) {
                            onUpdatePreferences(newPreferences)
                        }
                    }
                )
            }
            
            if (preferences.isContentTypeEnabled(contentType.name)) {
                // 根据内容类型显示对应的二级导航项
                when (contentType) {
                    DiscoveryContentType.ILLUSTS -> {
                        DiscoveryMode.entries.forEach { mode ->
                            item {
                                DiscoveryModeItem(
                                    mode = mode,
                                    isEnabled = preferences.illustsEnabledModes.contains(mode.name),
                                    onToggle = {
                                        val newModes = if (preferences.illustsEnabledModes.contains(mode.name)) {
                                            preferences.illustsEnabledModes - mode.name
                                        } else {
                                            preferences.illustsEnabledModes + mode.name
                                        }
                                        if (newModes.isNotEmpty()) {
                                            onUpdatePreferences(preferences.copy(illustsEnabledModes = newModes))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    DiscoveryContentType.NOVELS -> {
                        DiscoveryMode.entries.forEach { mode ->
                            item {
                                DiscoveryModeItem(
                                    mode = mode,
                                    isEnabled = preferences.novelsEnabledModes.contains(mode.name),
                                    onToggle = {
                                        val newModes = if (preferences.novelsEnabledModes.contains(mode.name)) {
                                            preferences.novelsEnabledModes - mode.name
                                        } else {
                                            preferences.novelsEnabledModes + mode.name
                                        }
                                        if (newModes.isNotEmpty()) {
                                            onUpdatePreferences(preferences.copy(novelsEnabledModes = newModes))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    DiscoveryContentType.PIXIVISION -> {
                        item {
                            PixivisionCategoryItem(
                                category = "ILLUSTRATION",
                                isEnabled = preferences.pixivisionEnabledCategories.contains("ILLUSTRATION"),
                                onToggle = {
                                    val newCategories = if (preferences.pixivisionEnabledCategories.contains("ILLUSTRATION")) {
                                        preferences.pixivisionEnabledCategories - "ILLUSTRATION"
                                    } else {
                                        preferences.pixivisionEnabledCategories + "ILLUSTRATION"
                                    }
                                    if (newCategories.isNotEmpty()) {
                                        onUpdatePreferences(preferences.copy(pixivisionEnabledCategories = newCategories))
                                    }
                                }
                            )
                        }
                        item {
                            PixivisionCategoryItem(
                                category = "MANGA",
                                isEnabled = preferences.pixivisionEnabledCategories.contains("MANGA"),
                                onToggle = {
                                    val newCategories = if (preferences.pixivisionEnabledCategories.contains("MANGA")) {
                                        preferences.pixivisionEnabledCategories - "MANGA"
                                    } else {
                                        preferences.pixivisionEnabledCategories + "MANGA"
                                    }
                                    if (newCategories.isNotEmpty()) {
                                        onUpdatePreferences(preferences.copy(pixivisionEnabledCategories = newCategories))
                                    }
                                }
                            )
                        }
                    }
                    DiscoveryContentType.USERS -> {
                        // 用户推荐没有二级导航
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        
        // 恢复默认按钮
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(Res.string.nav_settings_reset))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = {
                onResetToDefault()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

/**
 * 动态页导航偏好设置Tab
 */
@Composable
fun FollowLatestPreferencesTab(
    preferences: FollowLatestNavigationPreferences,
    onUpdatePreferences: (FollowLatestNavigationPreferences) -> Unit,
    onResetToDefault: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // 说明文字
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(Res.string.nav_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // 内容类型列表
        FollowLatestContentType.entries.forEach { contentType ->
            item {
                FollowLatestContentTypeItem(
                    contentType = contentType,
                    isEnabled = preferences.isContentTypeEnabled(contentType.name),
                    onToggle = {
                        val newPreferences = preferences.toggleContentType(contentType.name)
                        if (newPreferences.getFilteredContentTypes().isNotEmpty()) {
                            onUpdatePreferences(newPreferences)
                        }
                    }
                )
            }
            
            if (preferences.isContentTypeEnabled(contentType.name)) {
                // 根据内容类型显示对应的二级导航项
                when (contentType) {
                    FollowLatestContentType.ILLUSTS -> {
                        FollowLatestMode.entries.forEach { mode ->
                            item {
                                FollowLatestModeItem(
                                    mode = mode,
                                    isEnabled = preferences.illustsEnabledModes.contains(mode.name),
                                    onToggle = {
                                        val newModes = if (preferences.illustsEnabledModes.contains(mode.name)) {
                                            preferences.illustsEnabledModes - mode.name
                                        } else {
                                            preferences.illustsEnabledModes + mode.name
                                        }
                                        if (newModes.isNotEmpty()) {
                                            onUpdatePreferences(preferences.copy(illustsEnabledModes = newModes))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    FollowLatestContentType.NOVELS -> {
                        FollowLatestMode.entries.forEach { mode ->
                            item {
                                FollowLatestModeItem(
                                    mode = mode,
                                    isEnabled = preferences.novelsEnabledModes.contains(mode.name),
                                    onToggle = {
                                        val newModes = if (preferences.novelsEnabledModes.contains(mode.name)) {
                                            preferences.novelsEnabledModes - mode.name
                                        } else {
                                            preferences.novelsEnabledModes + mode.name
                                        }
                                        if (newModes.isNotEmpty()) {
                                            onUpdatePreferences(preferences.copy(novelsEnabledModes = newModes))
                                        }
                                    }
                                )
                            }
                        }
                    }
                    FollowLatestContentType.WATCH_LIST -> {
                        item {
                            WatchListTypeItem(
                                type = "MANGA",
                                isEnabled = preferences.watchListEnabledTypes.contains("MANGA"),
                                onToggle = {
                                    val newTypes = if (preferences.watchListEnabledTypes.contains("MANGA")) {
                                        preferences.watchListEnabledTypes - "MANGA"
                                    } else {
                                        preferences.watchListEnabledTypes + "MANGA"
                                    }
                                    if (newTypes.isNotEmpty()) {
                                        onUpdatePreferences(preferences.copy(watchListEnabledTypes = newTypes))
                                    }
                                }
                            )
                        }
                        item {
                            WatchListTypeItem(
                                type = "NOVELS",
                                isEnabled = preferences.watchListEnabledTypes.contains("NOVELS"),
                                onToggle = {
                                    val newTypes = if (preferences.watchListEnabledTypes.contains("NOVELS")) {
                                        preferences.watchListEnabledTypes - "NOVELS"
                                    } else {
                                        preferences.watchListEnabledTypes + "NOVELS"
                                    }
                                    if (newTypes.isNotEmpty()) {
                                        onUpdatePreferences(preferences.copy(watchListEnabledTypes = newTypes))
                                    }
                                }
                            )
                        }
                    }
                    FollowLatestContentType.GOOD_P_FRIENDS -> {
                        // 好P友没有二级导航
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        
        // 恢复默认按钮
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(Res.string.nav_settings_reset))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = {
                onResetToDefault()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

// ==================== 通用组件 ====================

@Composable
fun ResetConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nav_settings_reset_confirm_title)) },
        text = { Text(stringResource(Res.string.nav_settings_reset_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

// ==================== 排行榜组件 ====================

@Composable
fun RankingContentTypeItem(
    content: RankingContent,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = content.getLocalizedDisplayName(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RankingModeItem(
    mode: RankingMode,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = mode.getLocalizedDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

// ==================== 发现页组件 ====================

@Composable
fun DiscoveryContentTypeItem(
    contentType: DiscoveryContentType,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(contentType.displayNameRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DiscoveryModeItem(
    mode: DiscoveryMode,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (mode) {
                    DiscoveryMode.ALL -> stringResource(Res.string.discovery_mode_all)
                    DiscoveryMode.SAFE -> stringResource(Res.string.discovery_mode_safe)
                    DiscoveryMode.R18 -> stringResource(Res.string.discovery_mode_r18)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@Composable
fun PixivisionCategoryItem(
    category: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (category) {
                    "ILLUSTRATION" -> stringResource(Res.string.pixivision_illustration)
                    "MANGA" -> stringResource(Res.string.pixivision_manga)
                    else -> category
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

// ==================== 动态页组件 ====================

@Composable
fun FollowLatestContentTypeItem(
    contentType: FollowLatestContentType,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(contentType.displayNameRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FollowLatestModeItem(
    mode: FollowLatestMode,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (mode) {
                    FollowLatestMode.ALL -> stringResource(Res.string.follow_latest_mode_all)
                    FollowLatestMode.R18 -> stringResource(Res.string.follow_latest_mode_r18)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

@Composable
fun WatchListTypeItem(
    type: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (type) {
                    "MANGA" -> stringResource(Res.string.follow_latest_watch_list_manga)
                    "NOVELS" -> stringResource(Res.string.follow_latest_watch_list_novels)
                    else -> type
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}


