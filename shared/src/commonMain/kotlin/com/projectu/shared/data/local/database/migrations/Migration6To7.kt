package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本 6 -> 7
 * 为 app_settings 表添加 showUserProfileBackground 字段
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // 为 app_settings 表添加 showUserProfileBackground 字段，默认值为 1 (true)
        // Room 会自动将驼峰命名映射为数据库字段名
        connection.execSQL("""
            ALTER TABLE app_settings 
            ADD COLUMN showUserProfileBackground INTEGER NOT NULL DEFAULT 1
        """.trimIndent())
    }
}
