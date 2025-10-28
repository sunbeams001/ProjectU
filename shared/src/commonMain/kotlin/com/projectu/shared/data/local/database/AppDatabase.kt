package com.projectu.shared.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2, // 增加版本号以支持新添加的SettingsEntity
    exportSchema = true
)
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
