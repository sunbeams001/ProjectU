package com.projectu.shared.data.local.database

import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.projectu.shared.data.local.database.migrations.MIGRATION_1_2
import com.projectu.shared.data.local.database.migrations.MIGRATION_2_3
import com.projectu.shared.data.local.database.migrations.MIGRATION_3_4

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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
}
