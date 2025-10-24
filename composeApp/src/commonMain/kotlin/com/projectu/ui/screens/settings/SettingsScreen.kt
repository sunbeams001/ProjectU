package com.projectu.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.projectu.shared.data.local.AppLanguage
import com.projectu.shared.data.local.PixivLanguage
import com.projectu.shared.data.local.ThemeMode
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
        
        SettingsScreenContent(
            currentAppLanguage = settings.appLanguage,
            currentPixivLanguage = settings.pixivLanguage,
            currentThemeMode = settings.themeMode,
            onAppLanguageChange = { viewModel.updateAppLanguage(it) },
            onPixivLanguageChange = { viewModel.updatePixivLanguage(it) },
            onThemeModeChange = { viewModel.updateThemeMode(it) },
            onNavigateBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    currentAppLanguage: AppLanguage,
    currentPixivLanguage: PixivLanguage,
    currentThemeMode: ThemeMode,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onPixivLanguageChange: (PixivLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showPixivLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // 通用设置分组
            item {
                SettingsGroupHeader(title = "通用设置")
            }
            
            // 应用语言设置
            item {
                SettingsItem(
                    title = "应用语言",
                    subtitle = currentAppLanguage.displayName,
                    onClick = { showAppLanguageDialog = true }
                )
            }
            
            // 主题设置
            item {
                SettingsItem(
                    title = "主题模式",
                    subtitle = when (currentThemeMode) {
                        ThemeMode.LIGHT -> "浅色"
                        ThemeMode.DARK -> "深色"
                        ThemeMode.SYSTEM -> "跟随系统"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            // Pixiv 设置分组
            item {
                SettingsGroupHeader(title = "Pixiv 设置")
            }
            
            // Pixiv 语言设置
            item {
                SettingsItem(
                    title = "Pixiv 语言偏好",
                    subtitle = currentPixivLanguage.displayName,
                    description = "获取 Pixiv 数据时的语言设置",
                    onClick = { showPixivLanguageDialog = true }
                )
            }
        }
    }
    
    // 应用语言选择对话框
    if (showAppLanguageDialog) {
        LanguageSelectionDialog(
            title = "选择应用语言",
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
            title = "选择 Pixiv 语言",
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
                Text("取消")
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
        ThemeMode.LIGHT to "浅色",
        ThemeMode.DARK to "深色",
        ThemeMode.SYSTEM to "跟随系统"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
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
                Text("取消")
            }
        }
    )
}

