package com.projectu.shared.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * 数据库迁移：版本3到版本4
 * 
 * 变更内容：
 * - 添加 novel_translation_cache 表用于缓存小说翻译内容
 *   - id: 主键，格式为 "novelId_pageIndex_targetLanguage"
 *   - novelId: 小说ID
 *   - pageIndex: 页码索引
 *   - originalContent: 原文内容
 *   - translatedContent: 翻译后的内容
 *   - targetLanguage: 目标语言代码
 *   - engine: 翻译引擎
 *   - createdAt: 缓存创建时间戳
 *   - lastAccessedAt: 最后访问时间戳
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // 创建 novel_translation_cache 表
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS novel_translation_cache (
                id TEXT PRIMARY KEY NOT NULL,
                novelId TEXT NOT NULL,
                pageIndex INTEGER NOT NULL,
                originalContent TEXT NOT NULL,
                translatedContent TEXT NOT NULL,
                targetLanguage TEXT NOT NULL,
                engine TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                lastAccessedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        
        // 为 novelId 创建索引以优化查询性能
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_novel_translation_cache_novelId ON novel_translation_cache(novelId)"
        )
        
        // 为 createdAt 创建索引以优化过期缓存清理
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_novel_translation_cache_createdAt ON novel_translation_cache(createdAt)"
        )
    }
}
