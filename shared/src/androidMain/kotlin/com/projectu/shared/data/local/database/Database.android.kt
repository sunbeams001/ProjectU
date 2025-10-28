package com.projectu.shared.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android平台的数据库构建器
 * 按照官方KMP Room文档标准实现
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // 使用全局Context提供者
    val context = ContextHolder.getContext()
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("app_database.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

/**
 * Context持有者 - 用于KMP项目
 * 在Android应用启动时设置Context
 */
object ContextHolder {
    private var context: Context? = null
    
    fun setContext(ctx: Context) {
        context = ctx.applicationContext
    }
    
    fun getContext(): Context {
        return context ?: throw IllegalStateException("Context not initialized. Call setContext() first.")
    }
}
