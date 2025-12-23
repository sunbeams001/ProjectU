package com.projectu.shared.data.local.database

import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * 数据库构建器
 * 按照官方KMP Room文档标准实现
 */

// 平台特定的getDatabaseBuilder函数在各自平台文件中定义
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
