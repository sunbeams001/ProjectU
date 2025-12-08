package com.projectu.shared.data.local.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * 数据库构建器
 * 按照官方KMP Room文档标准实现
 */

// 平台特定的getDatabaseBuilder函数在各自平台文件中定义
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

/**
 * 数据库迁移：版本 8 -> 9
 * 添加 thumbnailUrl 字段到 download_tasks 表
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE download_tasks ADD COLUMN thumbnailUrl TEXT"
        )
    }
}

/**
 * 数据库迁移：版本 9 -> 10
 * 添加 baseDownloadPath 字段到 app_settings 表
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN baseDownloadPath TEXT NOT NULL DEFAULT ''"
        )
    }
}

/**
 * 数据库迁移：版本 10 -> 11
 * 创建下载规则表
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        // 创建下载规则表
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS download_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ruleOrder INTEGER NOT NULL,
                resourceTypeFilter TEXT NOT NULL,
                r18Filter TEXT NOT NULL,
                aiFilter TEXT NOT NULL,
                authorGrouping TEXT NOT NULL,
                targetPath TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        
        // 创建索引（优化查询性能）
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS index_download_rules_ruleOrder 
            ON download_rules(ruleOrder)
        """.trimIndent())
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加 subDirectory 列（默认为空字符串）
        connection.execSQL("""
            ALTER TABLE download_rules 
            ADD COLUMN subDirectory TEXT NOT NULL DEFAULT ''
        """.trimIndent())
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
