package com.projectu.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.local.NovelBackgroundScheme
import com.projectu.shared.data.local.NovelFontSize
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.ui.components.ColorPickerDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.*

/**
 * 小说阅读设置页面
 * 支持设置字号、文字颜色、背景色等
 * 顶部提供实时预览功能
 */
class NovelReadingSettingsScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()
        val settings by settingsRepository.getSettings().collectAsState(initial = null)
        val coroutineScope = rememberCoroutineScope()
        
        var showTextColorPicker by remember { mutableStateOf(false) }
        var showBackgroundColorPicker by remember { mutableStateOf(false) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.novel_reading_settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.nav_back))
                        }
                    }
                )
            }
        ) { paddingValues ->
            settings?.let { currentSettings ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 预览区域 - 固定在顶部
                    NovelReadingPreview(
                        modifier = Modifier.padding(16.dp),
                        fontSize = currentSettings.novelFontSize,
                        textColor = currentSettings.novelTextColor,
                        backgroundColor = currentSettings.novelBackgroundColor,
                        backgroundScheme = currentSettings.novelBackgroundScheme
                    )
                    
                    // 可滚动的设置区域
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 字号设置
                        SettingSectionHeader(title = stringResource(Res.string.novel_setting_font_size))
                        FontSizeSelector(
                            currentSize = currentSettings.novelFontSize,
                            onSizeSelected = { newSize ->
                                coroutineScope.launch {
                                    settingsRepository.updateNovelFontSize(newSize)
                                }
                            }
                        )
                        
                        // 背景色方案设置
                        SettingSectionHeader(title = stringResource(Res.string.novel_setting_background_scheme))
                        BackgroundSchemeSelector(
                            currentScheme = currentSettings.novelBackgroundScheme,
                            onSchemeSelected = { newScheme ->
                                coroutineScope.launch {
                                    settingsRepository.updateNovelBackgroundScheme(newScheme)
                                    // 如果选择了非自定义方案，清除自定义颜色
                                    if (newScheme != NovelBackgroundScheme.CUSTOM) {
                                        settingsRepository.updateNovelBackgroundColor(null)
                                        settingsRepository.updateNovelTextColor(null)
                                    }
                                }
                            }
                        )
                        
                        // 自定义颜色设置（仅当选择自定义方案时显示）
                        if (currentSettings.novelBackgroundScheme == NovelBackgroundScheme.CUSTOM) {
                            SettingSectionHeader(title = stringResource(Res.string.novel_setting_custom_colors))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 文字颜色
                                ColorSettingItem(
                                    label = stringResource(Res.string.novel_setting_text_color),
                                    color = currentSettings.novelTextColor,
                                    onClick = { showTextColorPicker = true }
                                )
                                
                                // 背景色
                                ColorSettingItem(
                                    label = stringResource(Res.string.novel_setting_background_color),
                                    color = currentSettings.novelBackgroundColor,
                                    onClick = { showBackgroundColorPicker = true }
                                )
                            }
                        }
                        
                        // 底部间距
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 文字颜色选择器
                if (showTextColorPicker) {
                    ColorPickerDialog(
                        initialColor = currentSettings.novelTextColor,
                        onColorSelected = { color ->
                            coroutineScope.launch {
                                settingsRepository.updateNovelTextColor(color)
                            }
                        },
                        onDismiss = { showTextColorPicker = false }
                    )
                }
                
                // 背景色选择器
                if (showBackgroundColorPicker) {
                    ColorPickerDialog(
                        initialColor = currentSettings.novelBackgroundColor,
                        onColorSelected = { color ->
                            coroutineScope.launch {
                                settingsRepository.updateNovelBackgroundColor(color)
                            }
                        },
                        onDismiss = { showBackgroundColorPicker = false }
                    )
                }
            }
        }
    }
}

/**
 * 小说阅读预览组件
 */
@Composable
private fun NovelReadingPreview(
    modifier: Modifier = Modifier,
    fontSize: NovelFontSize,
    textColor: String?,
    backgroundColor: String?,
    backgroundScheme: NovelBackgroundScheme
) {
    // 确定最终使用的颜色
    val finalBackgroundColor = when {
        backgroundScheme == NovelBackgroundScheme.CUSTOM && backgroundColor != null -> parseColor(backgroundColor)
        backgroundScheme.backgroundColor != null -> parseColor(backgroundScheme.backgroundColor)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val finalTextColor = when {
        backgroundScheme == NovelBackgroundScheme.CUSTOM && textColor != null -> parseColor(textColor)
        backgroundScheme.textColor != null -> parseColor(backgroundScheme.textColor)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(finalBackgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.novel_preview_label),
                style = MaterialTheme.typography.labelSmall,
                color = finalTextColor.copy(alpha = 0.6f)
            )
            
            Text(
                text = stringResource(Res.string.novel_preview_content),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp.sp,
                    lineHeight = (fontSize.sp * 1.75f).sp
                ),
                color = finalTextColor
            )
        }
    }
}

/**
 * 字号选择器 - 使用Slider实现
 */
@Composable
private fun FontSizeSelector(
    currentSize: NovelFontSize,
    onSizeSelected: (NovelFontSize) -> Unit
) {
    val fontSizes = NovelFontSize.values()
    val currentIndex = fontSizes.indexOf(currentSize).toFloat()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 当前字号显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getFontSizeDisplayName(currentSize),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentSize.sp}sp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Slider
            Slider(
                value = currentIndex,
                onValueChange = { value ->
                    val index = value.toInt().coerceIn(0, fontSizes.size - 1)
                    onSizeSelected(fontSizes[index])
                },
                valueRange = 0f..(fontSizes.size - 1).toFloat(),
                steps = fontSizes.size - 2,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 字号范围标注
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getFontSizeDisplayName(fontSizes.first()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getFontSizeDisplayName(fontSizes.last()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 背景色方案选择器
 */
@Composable
private fun BackgroundSchemeSelector(
    currentScheme: NovelBackgroundScheme,
    onSchemeSelected: (NovelBackgroundScheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NovelBackgroundScheme.values().forEach { scheme ->
            BackgroundSchemeOption(
                scheme = scheme,
                isSelected = scheme == currentScheme,
                onClick = { onSchemeSelected(scheme) }
            )
        }
    }
}

/**
 * 背景色方案选项
 */
@Composable
private fun BackgroundSchemeOption(
    scheme: NovelBackgroundScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 颜色预览 - 显示背景色和文字颜色
                when {
                    scheme == NovelBackgroundScheme.THEME_DEFAULT || scheme == NovelBackgroundScheme.CUSTOM -> {
                        // 主题默认和自定义不显示预览
                        Box(modifier = Modifier.size(48.dp))
                    }
                    else -> {
                        // 显示背景色和文字颜色预览
                        Box(
                            modifier = Modifier
                                .size(48.dp, 40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(parseColor(scheme.backgroundColor))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // 在背景色上显示“A”字样，使用文字颜色
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = parseColor(scheme.textColor)
                            )
                        }
                    }
                }
                
                Text(
                    text = getBackgroundSchemeDisplayName(scheme),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 颜色设置项
 */
@Composable
private fun ColorSettingItem(
    label: String,
    color: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (color != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseColor(color))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        text = color,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.color_theme_default),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 设置分组标题
 */
@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 解析十六进制颜色字符串为Color对象
 */
private fun parseColor(hex: String?): Color {
    return try {
        if (hex != null && hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            val colorInt = hex.substring(1).toLong(16).toInt()
            Color(0xFF000000 or colorInt.toLong())
        } else {
            Color.Transparent
        }
    } catch (e: Exception) {
        Color.Transparent
    }
}

/**
 * 获取字号显示名称
 */
@Composable
private fun getFontSizeDisplayName(size: NovelFontSize): String {
    return when (size) {
        NovelFontSize.SMALL -> stringResource(Res.string.novel_font_size_small)
        NovelFontSize.MEDIUM -> stringResource(Res.string.novel_font_size_medium)
        NovelFontSize.LARGE -> stringResource(Res.string.novel_font_size_large)
        NovelFontSize.EXTRA_LARGE -> stringResource(Res.string.novel_font_size_extra_large)
        NovelFontSize.HUGE -> stringResource(Res.string.novel_font_size_huge)
    }
}

/**
 * 获取背景方案显示名称
 */
@Composable
private fun getBackgroundSchemeDisplayName(scheme: NovelBackgroundScheme): String {
    return when (scheme) {
        NovelBackgroundScheme.THEME_DEFAULT -> stringResource(Res.string.novel_bg_theme_default)
        NovelBackgroundScheme.PAPER_WHITE -> stringResource(Res.string.novel_bg_paper_white)
        NovelBackgroundScheme.EYE_CARE_GREEN -> stringResource(Res.string.novel_bg_eye_care_green)
        NovelBackgroundScheme.WARM_YELLOW -> stringResource(Res.string.novel_bg_warm_yellow)
        NovelBackgroundScheme.CLASSIC_BEIGE -> stringResource(Res.string.novel_bg_classic_beige)
        NovelBackgroundScheme.NIGHT_BLACK -> stringResource(Res.string.novel_bg_night_black)
        NovelBackgroundScheme.CUSTOM -> stringResource(Res.string.novel_bg_custom)
    }
}
