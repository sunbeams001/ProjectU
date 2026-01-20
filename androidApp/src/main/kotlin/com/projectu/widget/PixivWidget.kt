package com.projectu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.projectu.MainActivity
import com.projectu.R
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.WidgetConfig
import com.projectu.shared.domain.repository.WidgetRepository
import com.projectu.shared.domain.usecase.GetWidgetArtworksUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Pixiv Widget Provider
 * 展示 Pixiv 作品的桌面小部件
 */
class PixivWidget : AppWidgetProvider(), KoinComponent {
    
    private val widgetRepository: WidgetRepository by inject()
    private val getWidgetArtworksUseCase: GetWidgetArtworksUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        const val ACTION_REFRESH = "com.projectu.widget.ACTION_REFRESH"
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新所有 Widget 实例
        for (appWidgetId in appWidgetIds) {
            scope.launch {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    // 使用临时 scope 而不是类成员，确保应用死亡后也能正常工作
                    CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                        try {
                            refreshWidget(context, appWidgetManager, appWidgetId)
                        } catch (e: Exception) {
                            // 静默失败
                        }
                    }
                }
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // 删除 Widget 配置和取消定时任务
        scope.launch {
            for (appWidgetId in appWidgetIds) {
                widgetRepository.deleteWidgetConfig(appWidgetId)
                cancelWidgetUpdate(context, appWidgetId)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        // 首个 Widget 添加时的初始化
    }
    
    override fun onDisabled(context: Context) {
        // 最后一个 Widget 删除时的清理
    }
    
    /**
     * 更新 Widget
     */
    private suspend fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            // 读取 Widget 配置
            val config = widgetRepository.getWidgetConfig(appWidgetId)
            if (config == null) {
                return
            }
            
            // 获取作品列表
            val artworksResult = getWidgetArtworksUseCase(config)
            val artworks = artworksResult.getOrNull() ?: emptyList()
            
            if (artworks.isEmpty()) {
                updateWidgetWithError(context, appWidgetManager, appWidgetId, context.getString(R.string.widget_error_no_artwork))
                return
            }
            
            // 获取当前要显示的作品
            val currentIndex = config.currentIndex.coerceIn(0, artworks.size - 1)
            val artwork = artworks[currentIndex]
            
            // 加载图片
            val bitmap = WidgetImageLoader.loadArtworkImage(context, artwork)
            
            // 构建 RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_pixiv)
            
            if (bitmap != null) {
                // 根据配置选择使用哪个ImageView
                val useFitCenter = config.imageScaleType == com.projectu.shared.domain.model.WidgetImageScaleType.FIT_CENTER
                
                if (useFitCenter) {
                    views.setImageViewBitmap(R.id.widget_image_fit, bitmap)
                    views.setViewVisibility(R.id.widget_image_fit, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_image_crop, View.GONE)
                } else {
                    views.setImageViewBitmap(R.id.widget_image_crop, bitmap)
                    views.setViewVisibility(R.id.widget_image_crop, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_image_fit, View.GONE)
                }
                views.setViewVisibility(R.id.widget_loading, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_image_fit, View.GONE)
                views.setViewVisibility(R.id.widget_image_crop, View.GONE)
                views.setViewVisibility(R.id.widget_loading, View.VISIBLE)
            }
            
            // 设置刷新按钮可见性
            views.setViewVisibility(
                R.id.widget_refresh_button,
                if (config.showRefreshButton) View.VISIBLE else View.GONE
            )
            
            // 设置点击事件
            setClickIntents(context, views, artwork, appWidgetId, config)
            
            // 更新 Widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // 调度下次更新
            scheduleWidgetUpdate(context, appWidgetId, config.updateIntervalMinutes.toLong())
            
        } catch (e: Exception) {
            updateWidgetWithError(context, appWidgetManager, appWidgetId, e.message ?: context.getString(R.string.widget_error_update_failed))
        }
    }
    
    /**
     * 刷新 Widget（切换到下一张作品）
     */
    private suspend fun refreshWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val config = widgetRepository.getWidgetConfig(appWidgetId)
            if (config == null) {
                return
            }
            
            // 常规刷新：使用缓存数据在列表中循环切换，不重新请求API
            // 这样可以节省流量和电量，提升响应速度
            val artworksResult = getWidgetArtworksUseCase(config, forceRefresh = false)
            val artworks = artworksResult.getOrNull() ?: emptyList()
            
            if (artworks.isEmpty()) {
                return
            }
            
            // 切换到下一张
            val nextIndex = (config.currentIndex + 1) % artworks.size
            val nextArtwork = artworks[nextIndex]
            
            // 更新当前索引
            widgetRepository.updateCurrentArtwork(appWidgetId, nextArtwork.id, nextIndex)
            
            // 更新显示
            updateWidget(context, appWidgetManager, appWidgetId)
            
        } catch (e: Exception) {
            updateWidgetWithError(context, appWidgetManager, appWidgetId, e.message ?: context.getString(R.string.widget_error_refresh_failed))
        }
    }
    
    /**
     * 设置点击事件
     */
    private fun setClickIntents(
        context: Context,
        views: RemoteViews,
        artwork: Artwork,
        appWidgetId: Int,
        config: WidgetConfig
    ) {
        // 点击卡片主体跳转到作品详情
        // 关键修复：应用强制停止后 PendingIntent 数据会丢失
        // 解决方案：通过 BroadcastReceiver + SharedPreferences 传递数据
        val artworkIntent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = WidgetClickReceiver.ACTION_WIDGET_CLICK
            putExtra(WidgetClickReceiver.EXTRA_ARTWORK_ID, artwork.id)
            putExtra(WidgetClickReceiver.EXTRA_WIDGET_ID, appWidgetId)
            // 添加 FLAG_INCLUDE_STOPPED_PACKAGES 尝试唤醒强制停止的应用
            // 注意：这对"强制停止"可能仍然无效，这是 Android 系统安全限制
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        
        // 使用作品ID作为requestCode，确保不同作品的PendingIntent不会相互覆盖
        val artworkPendingIntent = PendingIntent.getBroadcast(
            context,
            artwork.id.hashCode(),
            artworkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 点击图片跳转到作品详情（两个ImageView都设置）
        views.setOnClickPendingIntent(R.id.widget_image_fit, artworkPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_image_crop, artworkPendingIntent)
        
        // 点击刷新按钮
        val refreshIntent = Intent(context, WidgetClickReceiver::class.java).apply {
            action = WidgetClickReceiver.ACTION_WIDGET_REFRESH
            putExtra(WidgetClickReceiver.EXTRA_WIDGET_ID, appWidgetId)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 10000,  // 偏移避免与点击事件冲突
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
    }
    
    /**
     * 显示错误信息
     */
    private fun updateWidgetWithError(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        message: String
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_pixiv)
        // 隐藏图片和加载指示器，只显示空白
        views.setViewVisibility(R.id.widget_image_fit, View.GONE)
        views.setViewVisibility(R.id.widget_image_crop, View.GONE)
        views.setViewVisibility(R.id.widget_loading, View.VISIBLE)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    /**
     * 调度 Widget 更新
     */
    private fun scheduleWidgetUpdate(
        context: Context,
        appWidgetId: Int,
        intervalMinutes: Long
    ) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            intervalMinutes.coerceAtLeast(15), // WorkManager 系统限制：最小间隔 15 分钟
            TimeUnit.MINUTES
        )
            .setInputData(workDataOf("appWidgetId" to appWidgetId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "widget_update_$appWidgetId",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    /**
     * 取消 Widget 更新
     */
    private fun cancelWidgetUpdate(context: Context, appWidgetId: Int) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("widget_update_$appWidgetId")
    }
}
