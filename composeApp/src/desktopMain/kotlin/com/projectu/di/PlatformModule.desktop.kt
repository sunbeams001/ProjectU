package com.projectu.di

import com.projectu.shared.data.cache.UgoiraCache
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.dao.SettingsDao
import com.projectu.shared.data.local.database.AppDatabase
import com.projectu.shared.data.local.database.getDatabaseBuilder
import com.projectu.shared.data.local.database.getRoomDatabase
import com.projectu.shared.data.repository.SettingsRepositoryImpl
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.shared.util.NetworkClient
import com.projectu.ui.components.UgoiraLoaderManager
import com.projectu.ui.screens.apitest.ApiTestViewModel
import com.projectu.ui.screens.settings.SettingsViewModel
import com.projectu.ui.screens.discovery.DiscoveryIllustsViewModel
import com.projectu.ui.screens.discovery.DiscoveryNovelsViewModel
import com.projectu.ui.screens.discovery.DiscoveryUsersViewModel
import io.ktor.client.engine.cio.*
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    single { NetworkClient.create(CIO.create()) }
}

actual val databaseModule: Module = module {
    // 数据库实例 - 按照官方文档标准实现
    single {
        val builder = getDatabaseBuilder()
        getRoomDatabase(builder)
    }
    
    // DAO
    single<SettingsDao> { get<AppDatabase>().settingsDao() }
    
    // 设置存储
    single { SettingsStore(get()) }
}

actual val repositoryModule: Module = module {
    // 设置仓储
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

actual val useCaseModule: Module = module {
    // Ugoira 缓存管理器
    single {
        // Desktop 使用用户目录下的缓存目录
        val userHome = System.getProperty("user.home")
        val cacheDir = "$userHome/.projectu/cache".toPath()
        UgoiraCache(FileSystem.SYSTEM, cacheDir)
    }
    
    // Ugoira 加载管理器
    factory {
        UgoiraLoaderManager(
            artworkRepository = get(),
            ugoiraCache = get(),
            httpClient = get()
        )
    }
}

actual val viewModelModule: Module = module {
    // 设置 ViewModel
    single { SettingsViewModel(get(), get()) }
    
    // 登录 ViewModel
    single { com.projectu.ui.screens.login.LoginViewModel(get()) }
    
    // API 测试 ViewModel
    single { ApiTestViewModel(get(), get()) }
    
    // 发现插画 ScreenModel
    single { DiscoveryIllustsViewModel(get(), get(), get()) }
    
    // 发现用户 ScreenModel
    single { DiscoveryUsersViewModel(get(), get(), get(), get()) }
    
    // 发现小说 ScreenModel
    single { DiscoveryNovelsViewModel(get(), get(), get()) }
    
    // 排行榜 ScreenModel
    single { com.projectu.ui.screens.ranking.RankingViewModel(get(), get(), get(), get(), get()) }
    
    // 作品详情 ScreenModel
    factory { com.projectu.ui.screens.artwork.ArtworkDetailViewModel(get(), get(), get(), get(), get()) }
    
    // 用户主页 ScreenModel
    factory { com.projectu.ui.screens.user.UserViewModel(get(), get(), get(), get(), get(), get()) }
    
    // 小说系列详情 ScreenModel
    factory { com.projectu.ui.screens.novelseries.NovelSeriesViewModel(get()) }
    
    // 小说详情 ScreenModel
    factory { com.projectu.ui.screens.novel.NovelDetailViewModel(get(), get(), get(), get(), get(), get()) }
}

