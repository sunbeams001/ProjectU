package com.projectu.shared.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.projectu.shared.data.local.dao.ArtworkDao
import com.projectu.shared.data.local.dao.BlockRuleDao
import com.projectu.shared.data.local.dao.BrowseHistoryDao
import com.projectu.shared.data.local.dao.DownloadDao
import com.projectu.shared.data.local.dao.DownloadRulesDao
import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.dao.UgoiraCacheDao
import com.projectu.shared.data.local.entity.ArtworkEntity
import com.projectu.shared.data.local.entity.BlockRuleEntity
import com.projectu.shared.data.local.entity.BrowseHistoryEntity
import com.projectu.shared.data.local.entity.DownloadRuleEntity
import com.projectu.shared.data.local.entity.DownloadTaskEntity
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
        SettingsEntity::class,
        DownloadTaskEntity::class,
        DownloadRuleEntity::class,
        BrowseHistoryEntity::class,
        BlockRuleEntity::class
    ],
    version = 2,
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
    
    /**
     * 下载任务数据访问对象
     */
    abstract fun downloadDao(): DownloadDao
    
    /**
     * 下载规则数据访问对象
     */
    abstract fun downloadRulesDao(): DownloadRulesDao
    
    /**
     * 浏览历史数据访问对象
     */
    abstract fun browseHistoryDao(): BrowseHistoryDao
    
    /**
     * 屏蔽规则数据访问对象
     */
    abstract fun blockRuleDao(): BlockRuleDao
}

/**
 * 数据库构造器
 * Room 在非Android平台需要使用此构造器
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
