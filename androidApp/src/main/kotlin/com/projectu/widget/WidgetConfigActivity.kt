package com.projectu.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.projectu.R
import com.projectu.shared.domain.model.FilterType
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.model.WidgetDataSource
import com.projectu.shared.domain.model.WidgetRankingMode
import com.projectu.shared.domain.repository.WidgetRepository
import com.projectu.ui.theme.AppTheme
import com.projectu.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.util.Locale

/**
 * Widget 配置界面
 */
class WidgetConfigActivity : ComponentActivity() {
    
    private val widgetRepository: WidgetRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun attachBaseContext(newBase: Context) {
        // 在Activity创建前设置正确的Locale
        val settingsRepository = try {
            org.koin.core.context.GlobalContext.get().get<SettingsRepository>()
        } catch (e: Exception) {
            null
        }
        
        val context = if (settingsRepository != null) {
            // 获取应用语言设置
            val appLanguage = runBlocking {
                try {
                    settingsRepository.getCurrentSettings().appLanguage
                } catch (e: Exception) {
                    com.projectu.shared.data.local.AppLanguage.SIMPLIFIED_CHINESE
                }
            }
            
            // 创建本地化Context
            val locale = when (appLanguage) {
                com.projectu.shared.data.local.AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
                com.projectu.shared.data.local.AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
                com.projectu.shared.data.local.AppLanguage.ENGLISH -> Locale.ENGLISH
                com.projectu.shared.data.local.AppLanguage.JAPANESE -> Locale.JAPANESE
                com.projectu.shared.data.local.AppLanguage.KOREAN -> Locale.KOREAN
            }
            
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置初始结果为取消
        setResult(Activity.RESULT_CANCELED)
        
        // 获取 Widget ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setContent {
            AppTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    widgetRepository = widgetRepository,
                    onConfigSaved = { finishWithSuccess() },
                    onCancel = { finish() }
                )
            }
        }
    }
    
    private fun finishWithSuccess() {
        // 返回结果
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        
        // 触发 Widget 更新
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val updateIntent = Intent(this, PixivWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(updateIntent)
        
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    widgetRepository: WidgetRepository,
    onConfigSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var dataSource by remember { mutableStateOf(WidgetDataSource.RECOMMENDED) }
    var rankingMode by remember { mutableStateOf<WidgetRankingMode?>(null) }
    var r18Filter by remember { mutableStateOf(FilterType.MUST_NOT_BE) }
    var aiFilter by remember { mutableStateOf(FilterType.ANY) }
    var updateIntervalMinutes by remember { mutableStateOf(60) }
    var showRefreshButton by remember { mutableStateOf(true) }
    var imageScaleType by remember { mutableStateOf(com.projectu.shared.domain.model.WidgetImageScaleType.FIT_CENTER) }
    
    // 加载已有配置（如果存在）
    LaunchedEffect(appWidgetId) {
        val existingConfig = widgetRepository.getWidgetConfig(appWidgetId)
        existingConfig?.let {
            dataSource = it.dataSource
            rankingMode = it.rankingMode
            r18Filter = it.r18Filter
            aiFilter = it.aiFilter
            updateIntervalMinutes = it.updateIntervalMinutes
            showRefreshButton = it.showRefreshButton
            imageScaleType = it.imageScaleType
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据源选择
            DataSourceSection(
                dataSource = dataSource,
                rankingMode = rankingMode,
                onDataSourceChanged = { 
                    dataSource = it
                    if (it != WidgetDataSource.RANKING) {
                        rankingMode = null
                    }
                },
                onRankingModeChanged = { rankingMode = it }
            )
            
            HorizontalDivider()
            
            // 过滤设置
            FilterSection(
                r18Filter = r18Filter,
                aiFilter = aiFilter,
                onR18FilterChanged = { r18Filter = it },
                onAiFilterChanged = { aiFilter = it }
            )
            
            HorizontalDivider()
            
            // 更新间隔
            UpdateIntervalSection(
                updateIntervalMinutes = updateIntervalMinutes,
                onUpdateIntervalChanged = { updateIntervalMinutes = it }
            )
            
            HorizontalDivider()
            
            // 刷新按钮
            RefreshButtonSection(
                showRefreshButton = showRefreshButton,
                onShowRefreshButtonChanged = { showRefreshButton = it }
            )
            
            HorizontalDivider()
            
            // 显示设置
            DisplaySettingsSection(
                imageScaleType = imageScaleType,
                onImageScaleTypeChanged = { imageScaleType = it }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            val config = WidgetConfig(
                                widgetId = appWidgetId,
                                dataSource = dataSource,
                                rankingMode = rankingMode,
                                r18Filter = r18Filter,
                                aiFilter = aiFilter,
                                updateIntervalMinutes = updateIntervalMinutes,
                                showRefreshButton = showRefreshButton,
                                imageScaleType = imageScaleType
                            )
                            widgetRepository.saveWidgetConfig(config)
                            onConfigSaved()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

@Composable
fun DataSourceSection(
    dataSource: WidgetDataSource,
    rankingMode: WidgetRankingMode?,
    onDataSourceChanged: (WidgetDataSource) -> Unit,
    onRankingModeChanged: (WidgetRankingMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.widget_data_source),
            style = MaterialTheme.typography.titleMedium
        )
        
        WidgetDataSource.entries.forEach { source ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = dataSource == source,
                    onClick = { onDataSourceChanged(source) }
                )
                Text(
                    text = when (source) {
                        WidgetDataSource.RECOMMENDED -> stringResource(R.string.widget_data_source_recommended)
                        WidgetDataSource.FOLLOWING_LATEST -> stringResource(R.string.widget_data_source_following)
                        WidgetDataSource.RANKING -> stringResource(R.string.widget_data_source_ranking)
                    },
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        
        // 排行榜模式（仅在选择排行榜时显示）
        if (dataSource == WidgetDataSource.RANKING) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_ranking_mode),
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    WidgetRankingMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = rankingMode == mode,
                                onClick = { onRankingModeChanged(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    WidgetRankingMode.DAY -> stringResource(R.string.widget_ranking_day)
                                    WidgetRankingMode.WEEK -> stringResource(R.string.widget_ranking_week)
                                    WidgetRankingMode.MONTH -> stringResource(R.string.widget_ranking_month)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    r18Filter: FilterType,
    aiFilter: FilterType,
    onR18FilterChanged: (FilterType) -> Unit,
    onAiFilterChanged: (FilterType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.widget_filter_settings),
            style = MaterialTheme.typography.titleMedium
        )
        
        // R-18 过滤
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.widget_filter_r18),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterType.entries.forEach { filter ->
                    FilterChip(
                        selected = r18Filter == filter,
                        onClick = { onR18FilterChanged(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    FilterType.MUST_BE -> stringResource(R.string.widget_filter_must_be)
                                    FilterType.MUST_NOT_BE -> stringResource(R.string.widget_filter_must_not_be)
                                    FilterType.ANY -> stringResource(R.string.widget_filter_any)
                                }
                            )
                        }
                    )
                }
            }
        }
        
        // AI 生成过滤
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.widget_filter_ai),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterType.entries.forEach { filter ->
                    FilterChip(
                        selected = aiFilter == filter,
                        onClick = { onAiFilterChanged(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    FilterType.MUST_BE -> stringResource(R.string.widget_filter_must_be)
                                    FilterType.MUST_NOT_BE -> stringResource(R.string.widget_filter_must_not_be)
                                    FilterType.ANY -> stringResource(R.string.widget_filter_any)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateIntervalSection(
    updateIntervalMinutes: Int,
    onUpdateIntervalChanged: (Int) -> Unit
) {
    // 离散的时间间隔选项（分钟）
    val intervalOptions = remember {
        listOf(
            15, 20, 25, 30, 35, 40, 45, 50, 55, 60,  // 15分钟到1小时，每5分钟
            90, 120, 180, 240, 360, 480, 720, 1440   // 1.5小时到24小时
        )
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.widget_update_interval),
            style = MaterialTheme.typography.titleMedium
        )
        
        // 显示当前间隔（格式化显示）
        Text(
            text = formatIntervalTime(updateIntervalMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 找到当前值对应的索引
        val currentIndex = remember(updateIntervalMinutes) {
            intervalOptions.indexOf(updateIntervalMinutes).let { 
                if (it == -1) {
                    // 如果不在列表中，找到最接近的值
                    intervalOptions.indexOfFirst { it >= updateIntervalMinutes }
                        .let { idx -> if (idx == -1) intervalOptions.lastIndex else idx }
                } else it
            }
        }
        
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { floatValue ->
                val index = (floatValue + 0.5f).toInt().coerceIn(0, intervalOptions.lastIndex)
                val selectedValue = intervalOptions[index]
                if (selectedValue != updateIntervalMinutes) {
                    onUpdateIntervalChanged(selectedValue)
                }
            },
            valueRange = 0f..(intervalOptions.size - 1).toFloat(),
            steps = intervalOptions.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatIntervalTime(intervalOptions.first()), style = MaterialTheme.typography.labelSmall)
            Text(formatIntervalTime(intervalOptions.last()), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * 格式化时间间隔显示
 */
@Composable
private fun formatIntervalTime(minutes: Int): String {
    return when {
        minutes < 60 -> stringResource(R.string.time_unit_minute, minutes)
        minutes < 1440 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins == 0) {
                stringResource(R.string.time_unit_hour, hours)
            } else {
                stringResource(R.string.time_unit_hour_minute, hours, mins)
            }
        }
        else -> stringResource(R.string.time_unit_day, minutes / 1440)
    }
}

@Composable
fun RefreshButtonSection(
    showRefreshButton: Boolean,
    onShowRefreshButtonChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.widget_show_refresh_button),
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            checked = showRefreshButton,
            onCheckedChange = onShowRefreshButtonChanged
        )
    }
}

@Composable
fun DisplaySettingsSection(
    imageScaleType: com.projectu.shared.domain.model.WidgetImageScaleType,
    onImageScaleTypeChanged: (com.projectu.shared.domain.model.WidgetImageScaleType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.widget_display_settings),
            style = MaterialTheme.typography.titleMedium
        )
        
        // 图片缩放方式
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.widget_image_scale_type),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.projectu.shared.domain.model.WidgetImageScaleType.entries.forEach { type ->
                    FilterChip(
                        selected = imageScaleType == type,
                        onClick = { onImageScaleTypeChanged(type) },
                        label = {
                            Text(
                                when (type) {
                                    com.projectu.shared.domain.model.WidgetImageScaleType.FIT_CENTER -> stringResource(R.string.widget_scale_fit_center)
                                    com.projectu.shared.domain.model.WidgetImageScaleType.CENTER_CROP -> stringResource(R.string.widget_scale_center_crop)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
