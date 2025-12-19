package com.projectu.shared.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本 14 -> 15
 * 添加收藏行为配置字段
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加 clickBookmarkAction 字段，默认值为 "PUBLIC"
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN clickBookmarkAction TEXT NOT NULL DEFAULT 'PUBLIC'"
        )
        
        // 添加 longPressBookmarkAction 字段，默认值为 "PRIVATE"
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN longPressBookmarkAction TEXT NOT NULL DEFAULT 'PRIVATE'"
        )
    }
}

/**
 * 数据库迁移：版本 16 -> 17
 * 添加大图浏览页首选图片质量字段
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加 viewerImageQuality 字段，默认值为 "MASTER_1200"
        connection.execSQL(
            "ALTER TABLE app_settings ADD COLUMN viewerImageQuality TEXT NOT NULL DEFAULT 'MASTER_1200'"
        )
    }
}
