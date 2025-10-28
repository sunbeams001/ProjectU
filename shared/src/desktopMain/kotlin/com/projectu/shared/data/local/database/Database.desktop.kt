package com.projectu.shared.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Desktop平台的数据库构建器
 * 按照官方KMP Room文档标准实现
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "app_database.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}
