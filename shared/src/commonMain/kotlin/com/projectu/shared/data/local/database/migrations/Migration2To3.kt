package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本2到版本3
 * 
 * 变更内容：
 * - 在 app_settings 表中添加翻译功能设置字段
 *   - translationEngine: 翻译引擎 (默认: NONE)
 *   - translationTargetLanguage: 目标语言 (默认: SIMPLIFIED_CHINESE)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加 translationEngine 字段，默认值为 NONE
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN translationEngine TEXT NOT NULL DEFAULT 'NONE'"
        )
        
        // 添加 translationTargetLanguage 字段，默认值为 SIMPLIFIED_CHINESE
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN translationTargetLanguage TEXT NOT NULL DEFAULT 'SIMPLIFIED_CHINESE'"
        )
    }
}
