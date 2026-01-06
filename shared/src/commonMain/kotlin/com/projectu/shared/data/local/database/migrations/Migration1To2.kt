package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本1到版本2
 * 
 * 变更内容：
 * - 在 app_settings 表中添加小说阅读设置字段
 *   - novelFontSize: 字号设置 (默认: MEDIUM)
 *   - novelTextColor: 文字颜色 (可选)
 *   - novelBackgroundColor: 背景颜色 (可选)
 *   - novelBackgroundScheme: 背景方案 (默认: THEME_DEFAULT)
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加 novelFontSize 字段，默认值为 MEDIUM
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN novelFontSize TEXT NOT NULL DEFAULT 'MEDIUM'"
        )
        
        // 添加 novelTextColor 字段，可为空
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN novelTextColor TEXT"
        )
        
        // 添加 novelBackgroundColor 字段，可为空
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN novelBackgroundColor TEXT"
        )
        
        // 添加 novelBackgroundScheme 字段，默认值为 THEME_DEFAULT
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN novelBackgroundScheme TEXT NOT NULL DEFAULT 'THEME_DEFAULT'"
        )
    }
}
