package com.projectu.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Widget 点击信息存储
 * 用于解决应用强制停止后 PendingIntent 数据丢失的问题
 */
object WidgetClickStore {
    private const val PREFS_NAME = "widget_click_data"
    private const val KEY_PENDING_ARTWORK_ID = "pending_artwork_id"
    private const val KEY_PENDING_WIDGET_ACTION = "pending_widget_action"
    private const val KEY_PENDING_WIDGET_ID = "pending_widget_id"
    private const val KEY_CLICK_TIMESTAMP = "click_timestamp"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 存储待查看的作品ID（Widget卡片点击）
     */
    fun storePendingArtwork(context: Context, artworkId: String, widgetId: Int) {
        getPrefs(context).edit().apply {
            putString(KEY_PENDING_ARTWORK_ID, artworkId)
            putString(KEY_PENDING_WIDGET_ACTION, "view_artwork")
            putInt(KEY_PENDING_WIDGET_ID, widgetId)
            putLong(KEY_CLICK_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * 存储待刷新的 Widget ID（刷新按钮点击）
     */
    fun storePendingRefresh(context: Context, widgetId: Int) {
        getPrefs(context).edit().apply {
            putString(KEY_PENDING_WIDGET_ACTION, "refresh_widget")
            putInt(KEY_PENDING_WIDGET_ID, widgetId)
            putLong(KEY_CLICK_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * 获取并清除待处理的点击信息
     * @return Pair<action, data> 或 null
     *   action: "view_artwork" 或 "refresh_widget"
     *   data: artworkId 或 widgetId.toString()
     */
    fun consumePendingAction(context: Context): Pair<String, String>? {
        val prefs = getPrefs(context)
        val action = prefs.getString(KEY_PENDING_WIDGET_ACTION, null)
        val timestamp = prefs.getLong(KEY_CLICK_TIMESTAMP, 0)
        
        // 只处理最近10秒内的点击（避免处理过期数据）
        if (action == null || System.currentTimeMillis() - timestamp > 10000) {
            clearPendingAction(context)
            return null
        }
        
        val result = when (action) {
            "view_artwork" -> {
                val artworkId = prefs.getString(KEY_PENDING_ARTWORK_ID, null)
                if (artworkId != null) {
                    Pair("view_artwork", artworkId)
                } else null
            }
            "refresh_widget" -> {
                val widgetId = prefs.getInt(KEY_PENDING_WIDGET_ID, -1)
                if (widgetId != -1) {
                    Pair("refresh_widget", widgetId.toString())
                } else null
            }
            else -> null
        }
        
        // 清除已处理的数据
        clearPendingAction(context)
        
        return result
    }
    
    /**
     * 清除待处理的点击信息
     */
    private fun clearPendingAction(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_PENDING_ARTWORK_ID)
            remove(KEY_PENDING_WIDGET_ACTION)
            remove(KEY_PENDING_WIDGET_ID)
            remove(KEY_CLICK_TIMESTAMP)
            apply()
        }
    }
}
