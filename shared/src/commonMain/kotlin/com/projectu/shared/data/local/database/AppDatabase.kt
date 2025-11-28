package com.projectu.shared.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.projectu.shared.data.local.dao.ArtworkDao
import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.dao.UgoiraCacheDao
import com.projectu.shared.data.local.entity.ArtworkEntity
import com.projectu.shared.data.local.entity.SettingsEntity
import com.projectu.shared.data.local.entity.UgoiraCacheEntity

/**
 * 应用数据库配置
 * 使用 Room 进行跨平台数据持久化
 */
@Database(
    entities = [
        ArtworkEntity::class,
        UgoiraCacheEntity::class,
        SettingsEntity::class
    ],
    version = 7, // 版本7: 在SettingsEntity中添加imageCacheSize字段
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 作品数据访问对象
     */
    abstract fun artworkDao(): ArtworkDao
    
    /**
     * Ugoira缓存数据访问对象
     */
    abstract fun ugoiraCacheDao(): UgoiraCacheDao
    
    /**
     * 设置数据访问对象
     */
    abstract fun settingsDao(): SettingsDao
}

/**
 * 数据库构造器
 * Room 在非Android平台需要使用此构造器
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
