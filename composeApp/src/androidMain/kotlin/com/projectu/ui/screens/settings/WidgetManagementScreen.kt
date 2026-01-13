package com.projectu.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.R
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.repository.WidgetRepository
import com.projectu.ui.localization.LocalLocaleManager
import com.projectu.ui.util.createLocalizedContext
import com.projectu.widget.PixivWidget
import com.projectu.widget.WidgetConfigActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Widget管理屏幕（Android专用）
 */
class WidgetManagementScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val localeManager = LocalLocaleManager.current
        val currentLanguage by localeManager.currentLanguage.collectAsState()
        
        // 创建本地化 Context，遵循应用语言设置
        val localizedContext = remember(currentLanguage) {
            context.createLocalizedContext(currentLanguage)
        }
        
        val widgetRepository: WidgetRepository = koinInject()
        val scope = rememberCoroutineScope()
        
        var widgets by remember { mutableStateOf<List<WidgetConfig>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        
        // 加载Widget列表
        LaunchedEffect(Unit) {
            scope.launch {
                widgetRepository.getAllWidgetConfigs().collect { configs ->
                    widgets = configs
                    isLoading = false
                }
            }
        }
        
        WidgetManagementContent(
            widgets = widgets,
            isLoading = isLoading,
            localizedContext = localizedContext,
            onNavigateBack = { navigator.pop() },
            onReconfigure = { widgetId ->
                openWidgetConfig(context, widgetId)
            },
            onDelete = { widgetId ->
                scope.launch {
                    widgetRepository.deleteWidgetConfig(widgetId)
                    // 触发Widget更新（会自动清理）
                    updateWidget(context, widgetId)
                }
            }
        )
    }
    
    private fun openWidgetConfig(context: Context, widgetId: Int) {
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun updateWidget(context: Context, widgetId: Int) {
        val intent = Intent(context, PixivWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        }
        context.sendBroadcast(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetManagementContent(
    widgets: List<WidgetConfig>,
    isLoading: Boolean,
    localizedContext: Context,
    onNavigateBack: () -> Unit,
    onReconfigure: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedContext.getString(R.string.widget_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedContext.getString(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                widgets.isEmpty() -> {
                    EmptyWidgetList(localizedContext)
                }
                else -> {
                    WidgetList(
                        widgets = widgets,
                        localizedContext = localizedContext,
                        onReconfigure = onReconfigure,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWidgetList(localizedContext: Context) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = localizedContext.getString(R.string.widget_no_widgets),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = localizedContext.getString(R.string.widget_add_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WidgetList(
    widgets: List<WidgetConfig>,
    localizedContext: Context,
    onReconfigure: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(widgets, key = { it.widgetId }) { widget ->
            WidgetItem(
                widget = widget,
                localizedContext = localizedContext,
                onReconfigure = { onReconfigure(widget.widgetId) },
                onDelete = { onDelete(widget.widgetId) }
            )
        }
    }
}

@Composable
private fun WidgetItem(
    widget: WidgetConfig,
    localizedContext: Context,
    onReconfigure: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Widget #${widget.widgetId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = localizedContext.getString(
                            R.string.widget_data_source_label,
                            when (widget.dataSource) {
                                com.projectu.shared.domain.model.WidgetDataSource.RECOMMENDED -> localizedContext.getString(R.string.widget_data_source_recommended)
                                com.projectu.shared.domain.model.WidgetDataSource.FOLLOWING_LATEST -> localizedContext.getString(R.string.widget_data_source_following)
                                com.projectu.shared.domain.model.WidgetDataSource.RANKING -> localizedContext.getString(R.string.widget_data_source_ranking)
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    widget.rankingMode?.let { mode ->
                        Text(
                            text = localizedContext.getString(
                                R.string.widget_ranking_mode_label,
                                when (mode) {
                                    com.projectu.shared.domain.model.WidgetRankingMode.DAY -> localizedContext.getString(R.string.widget_ranking_day)
                                    com.projectu.shared.domain.model.WidgetRankingMode.WEEK -> localizedContext.getString(R.string.widget_ranking_week)
                                    com.projectu.shared.domain.model.WidgetRankingMode.MONTH -> localizedContext.getString(R.string.widget_ranking_month)
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = localizedContext.getString(R.string.widget_update_interval_label, widget.updateIntervalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onReconfigure) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = localizedContext.getString(R.string.widget_reconfigure)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = localizedContext.getString(R.string.widget_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(localizedContext.getString(R.string.widget_delete_confirm_title)) },
            text = { Text(localizedContext.getString(R.string.widget_delete_confirm_message, widget.widgetId)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(localizedContext.getString(R.string.widget_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(localizedContext.getString(R.string.common_cancel))
                }
            }
        )
    }
}
