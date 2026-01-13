package com.projectu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker

/**
 * Widget 定时更新 Worker
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): ListenableWorker.Result {
        val appWidgetId = inputData.getInt("appWidgetId", -1)
        
        return try {
            if (appWidgetId == -1) {
                return ListenableWorker.Result.failure()
            }
            
            // 触发 Widget 的 onUpdate
            val intent = Intent(applicationContext, PixivWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            applicationContext.sendBroadcast(intent)
            
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }
}
