package com.projectu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.projectu.MainActivity

/**
 * Widget 点击事件接收器
 * 用于解决应用强制停止后无法通过 PendingIntent 传递数据的问题
 * 
 * 工作流程：
 * 1. Widget 点击 → 发送 Broadcast → 本 Receiver 接收
 * 2. 将点击信息存储到 SharedPreferences
 * 3. 启动 MainActivity
 * 4. MainActivity 读取并处理存储的信息
 */
class WidgetClickReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_WIDGET_CLICK = "com.projectu.widget.ACTION_WIDGET_CLICK"
        const val ACTION_WIDGET_REFRESH = "com.projectu.widget.ACTION_WIDGET_REFRESH"
        const val EXTRA_ARTWORK_ID = "artwork_id"
        const val EXTRA_WIDGET_ID = "widget_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_CLICK -> {
                // Widget 卡片点击 - 跳转到作品详情
                val artworkId = intent.getStringExtra(EXTRA_ARTWORK_ID)
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                
                if (artworkId != null && widgetId != -1) {
                    // 存储点击信息
                    WidgetClickStore.storePendingArtwork(context, artworkId, widgetId)
                    
                    // 启动 MainActivity
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(mainIntent)
                }
            }
            
            ACTION_WIDGET_REFRESH -> {
                // Widget 刷新按钮点击 - 直接后台刷新，不打开 App 界面
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                
                if (widgetId != -1 && widgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // 发送 ACTION_REFRESH 而不是 ACTION_APPWIDGET_UPDATE
                    // 这样会调用 refreshWidget() 而不是 updateWidget()，确保切换到下一张
                    val refreshIntent = Intent(context, PixivWidget::class.java).apply {
                        action = PixivWidget.ACTION_REFRESH
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    context.sendBroadcast(refreshIntent)
                }
            }
        }
    }
}
