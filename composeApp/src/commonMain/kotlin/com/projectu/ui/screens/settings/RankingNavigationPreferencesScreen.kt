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
import com.projectu.shared.domain.model.RankingNavigationPreferences
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*
import projectu.composeapp.generated.resources.Res

/**
 * 排行榜导航偏好设置页面
 * 允许用户自定义显示哪些排行榜导航项
 */
class RankingNavigationPreferencesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository = koinInject()
        
        // 订阅设置
        val settings by settingsRepository.getSettings()
            .collectAsState(initial = com.projectu.shared.data.local.AppSettings.DEFAULT)
        
        val preferences = settings.rankingNavigationPreferences
        val scope = rememberCoroutineScope()
        
        RankingNavigationPreferencesContent(
            preferences = preferences,
            onUpdatePreferences = { newPreferences ->
                scope.launch {
                    settingsRepository.updateRankingNavigationPreferences(newPreferences)
                }
            },
            onResetToDefault = {
                scope.launch {
                    settingsRepository.updateRankingNavigationPreferences(
                        RankingNavigationPreferences.DEFAULT
                    )
                }
            },
            onBackClick = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingNavigationPreferencesContent(
    preferences: RankingNavigationPreferences,
    onUpdatePreferences: (RankingNavigationPreferences) -> Unit,
    onResetToDefault: () -> Unit,
    onBackClick: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_settings_title_ranking)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                    ContentTypeItem(
                        content = content,
                        isEnabled = preferences.isContentTypeEnabled(content),
                        onToggle = {
                            val newPreferences = preferences.toggleContentType(content)
                            // 确保至少有一个内容类型启用
                            if (newPreferences.getEnabledContentTypes().isNotEmpty()) {
                                onUpdatePreferences(newPreferences)
                            }
                        }
                    )
                }
                
                // 如果该内容类型启用，显示其下的模式列表
                if (preferences.isContentTypeEnabled(content)) {
                    val supportedModes = RankingContentModeConfig.getSupportedModes(content)
                    supportedModes.forEach { mode ->
                        item {
                            ModeItem(
                                content = content,
                                mode = mode,
                                isEnabled = mode in preferences.getEnabledModes(content),
                                onToggle = {
                                    val newPreferences = preferences.toggleMode(content, mode)
                                    // 确保该内容类型至少有一个模式启用
                                    if (newPreferences.getEnabledModes(content).isNotEmpty()) {
                                        onUpdatePreferences(newPreferences)
                                    }
                                }
                            )
                        }
                    }
                    
                    // 分隔线
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
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
    }
    
    // 重置确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(Res.string.nav_settings_reset_confirm_title)) },
            text = { Text(stringResource(Res.string.nav_settings_reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}

/**
 * 内容类型项
 */
@Composable
fun ContentTypeItem(
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

/**
 * 模式项（缩进显示）
 */
@Composable
fun ModeItem(
    content: RankingContent,
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

/**
 * 获取 RankingContent 的本地化显示名称
 */
@Composable
fun RankingContent.getLocalizedDisplayName(): String {
    return when (this) {
        RankingContent.ALL -> stringResource(Res.string.ranking_content_all)
        RankingContent.ILLUST -> stringResource(Res.string.ranking_content_illust)
        RankingContent.MANGA -> stringResource(Res.string.ranking_content_manga)
        RankingContent.UGOIRA -> stringResource(Res.string.ranking_content_ugoira)
        RankingContent.NOVEL -> stringResource(Res.string.ranking_content_novel)
    }
}

/**
 * 获取 RankingMode 的本地化显示名称
 */
@Composable
fun RankingMode.getLocalizedDisplayName(): String {
    return when (this) {
        RankingMode.DAILY -> stringResource(Res.string.ranking_daily)
        RankingMode.WEEKLY -> stringResource(Res.string.ranking_weekly)
        RankingMode.MONTHLY -> stringResource(Res.string.ranking_monthly)
        RankingMode.ROOKIE -> stringResource(Res.string.ranking_rookie)
        RankingMode.ORIGINAL -> stringResource(Res.string.ranking_original)
        RankingMode.MALE -> stringResource(Res.string.ranking_male)
        RankingMode.FEMALE -> stringResource(Res.string.ranking_female)
        RankingMode.DAILY_AI -> stringResource(Res.string.ranking_ai)
        RankingMode.WEEKLY_ORIGINAL -> stringResource(Res.string.ranking_weekly_original)
        RankingMode.WEEKLY_AI -> stringResource(Res.string.ranking_weekly_ai)
        RankingMode.DAILY_R18 -> stringResource(Res.string.ranking_daily_r18)
        RankingMode.WEEKLY_R18 -> stringResource(Res.string.ranking_weekly_r18)
        RankingMode.MALE_R18 -> stringResource(Res.string.ranking_male_r18)
        RankingMode.FEMALE_R18 -> stringResource(Res.string.ranking_female_r18)
        RankingMode.DAILY_R18_AI -> stringResource(Res.string.ranking_ai_r18)
        RankingMode.WEEKLY_R18_AI -> stringResource(Res.string.ranking_weekly_r18_ai)
        RankingMode.R18G -> stringResource(Res.string.ranking_r18g)
    }
}
