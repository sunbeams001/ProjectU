package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本4到版本5
 * 
 * 变更内容：
 * - 在 app_settings 表中添加三个页面的导航配置字段（嵌套JSON格式）：
 *   1. rankingNavigationConfig: 排行榜导航配置
 *   2. discoveryNavigationConfig: 发现页导航配置
 *   3. followLatestNavigationConfig: 动态页导航配置
 * 
 * 默认值为空JSON对象 '{}'，应用层会自动使用默认配置（全部启用）
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        // 添加排行榜导航配置字段
        connection.execSQL(
            """
            ALTER TABLE app_settings 
            ADD COLUMN rankingNavigationConfig TEXT NOT NULL DEFAULT '{}'
            """
        )
        
        // 添加发现页导航配置字段
        connection.execSQL(
            """
            ALTER TABLE app_settings 
            ADD COLUMN discoveryNavigationConfig TEXT NOT NULL DEFAULT '{}'
            """
        )
        
        // 添加动态页导航配置字段
        connection.execSQL(
            """
            ALTER TABLE app_settings 
            ADD COLUMN followLatestNavigationConfig TEXT NOT NULL DEFAULT '{}'
            """
        )
    }
}
