package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本 5 -> 6
 * 添加 Widget 配置表
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // 创建 widget_configs 表
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS widget_configs (
                widgetId INTEGER PRIMARY KEY NOT NULL,
                dataSource TEXT NOT NULL,
                rankingMode TEXT,
                r18Filter TEXT NOT NULL,
                aiFilter TEXT NOT NULL,
                updateIntervalMinutes INTEGER NOT NULL,
                showRefreshButton INTEGER NOT NULL,
                imageScaleType TEXT NOT NULL DEFAULT 'FIT_CENTER',
                currentArtworkId TEXT,
                currentIndex INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                lastUpdatedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
