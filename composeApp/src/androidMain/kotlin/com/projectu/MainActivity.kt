package com.projectu

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.projectu.shared.data.local.database.ContextHolder
import com.projectu.widget.PixivWidget
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

/**
 * 分享的图片数据（Android 平台）
 */
data class SharedImageData(
    val imageUri: String,
    val searchEngine: ImageSearchEngine = ImageSearchEngine.SAUCENAO
)

class MainActivity : ComponentActivity() {
    
    companion object {
        // 进程级别的标志，应用进程存活期间保持，进程被杀时自动重置
        // 用于避免按返回键退出再打开时重复刷新 Widget
        private var hasReactivatedWidgetsInProcess = false
    }
    
    private val settingsRepository: com.projectu.shared.domain.repository.SettingsRepository by inject()
    
    // 存储待处理的深度链接
    private val pendingDeepLink = mutableStateOf<String?>(null)
    
    // 存储待处理的分享图片
    private val pendingSharedImage = mutableStateOf<SharedImageData?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 用于控制启动画面显示时长
        var keepSplashScreen = true
        
        // 安装启动画面（必须在 super.onCreate 之前调用）
        val splashScreen = installSplashScreen()
        
        // 保持启动画面显示，直到设置加载完成
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        
        super.onCreate(savedInstanceState)
        
        // 初始化ContextHolder - 用于KMP数据库构建
        ContextHolder.setContext(this)
        
        // 只在应用冷启动时重新激活 Widget（强制停止后首次打开）
        // savedInstanceState == null 表示是真正的冷启动，而非配置变更或返回键返回
        if (savedInstanceState == null) {
            reactivateWidgetReceiverIfNeeded()
        }
        
        // 检查是否有来自 Widget 的待处理操作
        checkWidgetPendingAction()
        
        // 处理启动时的 Intent
        handleIntent(intent)
        
        // 异步加载设置，完成后隐藏启动画面
        lifecycleScope.launch {
            // 预加载设置，确保主题数据已准备好
            settingsRepository.getSettings().first()
            // 设置加载完成，允许启动画面消失
            keepSplashScreen = false
        }
        
        enableEdgeToEdge()
        setContent {
            App(
                deepLink = pendingDeepLink.value,
                onDeepLinkConsumed = { pendingDeepLink.value = null },
                sharedImage = pendingSharedImage.value?.let { 
                    SharedImage(
                        imageUri = it.imageUri,
                        searchEngine = it.searchEngine
                    )
                },
                onSharedImageConsumed = { pendingSharedImage.value = null }
            )
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的 Intent（App 已在运行时收到新 Intent）
        handleIntent(intent)
    }
    


    override fun onResume() {
        super.onResume()
        // 关键修复：从其他 Activity 返回时，重新设置应用语言
        // 
        // 问题根源（已通过日志验证）：
        // Android 系统在 Activity 切换时会重置 Locale.getDefault() 为系统语言
        // 
        // 实际观察到的行为：
        // 1. MainActivity (Locale=en) → WidgetConfigActivity 启动
        // 2. WidgetConfigActivity.attachBaseContext (Locale=zh_CN) ← 系统已重置
        // 3. WidgetConfigActivity → MainActivity 返回
        // 4. MainActivity.onResume (Locale=zh_CN) ← 仍被重置为系统语言
        // 5. 执行 Locale.setDefault(appLanguage) ← 恢复为应用语言
        // 6. Locale.getDefault()=en ← 修复成功
        // 
        // 为什么需要修复：
        // - Activity.attachBaseContext() 只影响 Activity Context 的 Configuration
        // - 不影响 JVM 全局的 Locale.getDefault()
        // - Compose Resources (Res.string.xxx) 使用 Locale.getDefault() 选择资源
        // - 如果不修复，SettingsScreen 会显示错误语言的文本
        lifecycleScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                val locale = when (settings.appLanguage) {
                    com.projectu.shared.data.local.AppLanguage.SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
                    com.projectu.shared.data.local.AppLanguage.TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
                    com.projectu.shared.data.local.AppLanguage.ENGLISH -> Locale.ENGLISH
                    com.projectu.shared.data.local.AppLanguage.JAPANESE -> Locale.JAPANESE
                    com.projectu.shared.data.local.AppLanguage.KOREAN -> Locale.KOREAN
                }
                Locale.setDefault(locale)
            } catch (e: Exception) {
                // 忽略错误，保持当前 Locale
            }
        }
        
        // 每次恢复时检查是否有来自 Widget 的待处理操作
        checkWidgetPendingAction()
    }
    
    /**
     * 重新激活 Widget 的 BroadcastReceiver
     * 
     * 问题：应用强制停止后，Widget 上的 PendingIntent 失效
     * 解决：仅在应用真正的冷启动时强制刷新所有 Widget
     * 
     * 优化：使用进程级别的静态变量标志
     * - 应用进程存活期间：标志保持，不重复刷新（按返回键退出再打开）
     * - 应用进程被杀：标志自动重置为 false（强制停止后首次启动）
     */
    private fun reactivateWidgetReceiverIfNeeded() {
        // 如果本次应用进程已经重新激活过，跳过
        if (hasReactivatedWidgetsInProcess) {
            android.util.Log.d("MainActivity", "Widgets already reactivated in this process, skipping")
            return
        }
        
        try {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val widgetComponent = android.content.ComponentName(
                applicationContext,
                com.projectu.widget.PixivWidget::class.java
            )
            
            // 获取所有已添加的 Widget ID
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            
            if (widgetIds.isNotEmpty()) {
                android.util.Log.d("MainActivity", "Reactivating ${widgetIds.size} widgets: ${widgetIds.joinToString()}")
                
                // 强制刷新所有 Widget，重新绑定点击事件
                val updateIntent = Intent(applicationContext, com.projectu.widget.PixivWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                sendBroadcast(updateIntent)
                
                android.util.Log.d("MainActivity", "Widget reactivation broadcast sent")
                
                // 标记本次进程已重新激活
                hasReactivatedWidgetsInProcess = true
            } else {
                android.util.Log.d("MainActivity", "No widgets found to reactivate")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to reactivate widgets", e)
        }
    }
    
    /**
     * 检查并处理来自 Widget 的待处理操作
     * 用于解决应用强制停止后 PendingIntent 数据丢失的问题
     */
    private fun checkWidgetPendingAction() {
        lifecycleScope.launch {
            try {
                val pendingAction = com.projectu.widget.WidgetClickStore.consumePendingAction(applicationContext)
                if (pendingAction != null) {
                    val (action, data) = pendingAction
                    android.util.Log.d("MainActivity", "Widget pending action: $action, data=$data")
                    
                    when (action) {
                        "view_artwork" -> {
                            // 跳转到作品详情
                            val deepLink = "projectu://artwork/$data"
                            android.util.Log.d("MainActivity", "Setting deepLink from widget: $deepLink")
                            pendingDeepLink.value = deepLink
                        }
                        "refresh_widget" -> {
                            // 注意：这个分支现在应该不会被触发了
                            // 刷新按钮已改为直接在 WidgetClickReceiver 中处理，不会打开 App
                            val widgetId = data.toIntOrNull()
                            android.util.Log.d("MainActivity", "Legacy refresh widget: $widgetId (should not happen)")
                            if (widgetId != null && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val widgetIntent = Intent(applicationContext, com.projectu.widget.PixivWidget::class.java)
                                widgetIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
                                sendBroadcast(widgetIntent)
                                android.util.Log.d("MainActivity", "Widget refresh broadcast sent")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error checking widget pending action", e)
            }
        }
    }
    
    /**
     * 处理 Intent
     */
    private fun handleIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "handleIntent: action=${intent?.action}, data=${intent?.data}")
        
        when (intent?.action) {
            // 深度链接和 Widget 点击事件（统一使用 ACTION_VIEW）
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                android.util.Log.d("MainActivity", "ACTION_VIEW with URI: $uri")
                
                if (uri != null) {
                    when {
                        // Widget 刷新请求: projectu://widget/refresh?widgetId=xxx
                        uri.scheme == "projectu" && uri.host == "widget" && uri.pathSegments.firstOrNull() == "refresh" -> {
                            val widgetId = uri.getQueryParameter("widgetId")?.toIntOrNull()
                            android.util.Log.d("MainActivity", "Widget refresh request for widgetId=$widgetId")
                            if (widgetId != null && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                // 触发 Widget 刷新
                                lifecycleScope.launch {
                                    try {
                                        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                                        val widgetIntent = Intent(applicationContext, PixivWidget::class.java).apply {
                                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
                                        }
                                        sendBroadcast(widgetIntent)
                                        android.util.Log.d("MainActivity", "Widget refresh broadcast sent")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Failed to refresh widget", e)
                                    }
                                }
                            }
                        }
                        // 作品详情: projectu://artwork/{id}
                        uri.scheme == "projectu" && uri.host == "artwork" -> {
                            val artworkId = uri.pathSegments.firstOrNull()
                            android.util.Log.d("MainActivity", "Artwork view request for artworkId=$artworkId")
                            if (!artworkId.isNullOrBlank()) {
                                pendingDeepLink.value = uri.toString()
                            }
                        }
                        // 其他深度链接
                        else -> {
                            android.util.Log.d("MainActivity", "Generic deep link: $uri")
                            pendingDeepLink.value = uri.toString()
                        }
                    }
                }
            }
            // 分享图片
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    // 根据 Activity 组件名判断使用哪个搜索引擎
                    val searchEngine = if (intent.component?.className?.contains("Ascii2d") == true) {
                        ImageSearchEngine.ASCII2D
                    } else {
                        ImageSearchEngine.SAUCENAO
                    }
                    handleSharedImage(intent, searchEngine)
                }
            }
        }
    }
    
    /**
     * 处理分享的图片
     */
    @Suppress("DEPRECATION")
    private fun handleSharedImage(intent: Intent, searchEngine: ImageSearchEngine) {
        val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        if (imageUri != null) {
            pendingSharedImage.value = SharedImageData(imageUri.toString(), searchEngine)
        }
    }
}

