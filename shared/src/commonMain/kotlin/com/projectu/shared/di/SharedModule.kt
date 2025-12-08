package com.projectu.shared.di

import com.projectu.shared.data.cache.ArtworkCacheManager
import com.projectu.shared.data.cache.DownloadRulesCache
import com.projectu.shared.data.cache.NovelCacheManager
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.createPixivConfigDataStore
import com.projectu.shared.data.local.store.DownloadRulesStore
import com.projectu.shared.data.manager.DownloadManager
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.api.PixivApiClient
import com.projectu.shared.data.repository.ArtworkRepositoryImpl
import com.projectu.shared.data.repository.AuthRepositoryImpl
import com.projectu.shared.data.repository.CommentRepositoryImpl
import com.projectu.shared.data.repository.DownloadRepositoryImpl
import com.projectu.shared.data.repository.DownloadRulesRepository
import com.projectu.shared.data.repository.DownloadRulesRepositoryImpl
import com.projectu.shared.data.repository.MangaSeriesRepositoryImpl
import com.projectu.shared.data.repository.NovelRepositoryImpl
import com.projectu.shared.data.repository.NovelSeriesRepositoryImpl
import com.projectu.shared.data.repository.SettingsRepositoryImpl
import com.projectu.shared.data.repository.StateCacheRepositoryInMemory
import com.projectu.shared.data.repository.UserRepositoryImpl
import com.projectu.shared.data.repository.WatchListRepositoryImpl
import com.projectu.shared.data.util.DownloadPathBuilder
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.CommentRepository
import com.projectu.shared.domain.repository.DownloadRepository
import com.projectu.shared.domain.repository.MangaSeriesRepository
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.repository.NovelSeriesRepository
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.shared.domain.repository.StateCacheRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.repository.WatchListRepository
import com.projectu.shared.domain.usecase.BookmarkArtworkUseCase
import com.projectu.shared.domain.usecase.BookmarkNovelUseCase
import com.projectu.shared.domain.usecase.FollowUserUseCase
import com.projectu.shared.domain.usecase.GetUgoiraUseCase
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.shared.domain.usecase.SyncUserFollowDetailsUseCase
import com.projectu.shared.domain.usecase.SyncUserStatesUseCase
import com.projectu.shared.domain.usecase.UnbookmarkArtworkUseCase
import com.projectu.shared.domain.usecase.UnbookmarkNovelUseCase
import com.projectu.shared.domain.usecase.UnfollowUserUseCase
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import io.ktor.client.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared模块的Koin配置
 */

/**
 * 通用网络模块
 */
fun networkModule(httpClient: HttpClient) = module {
    single { httpClient }
}

/**
 * 数据存储模块
 */
val dataStoreModule = module {
    // Pixiv 配置存储
    single {
        PixivConfigStore(createPixivConfigDataStore())
    }
    
    // 应用设置存储
    single {
        SettingsStore(get())
    }
    
    // 应用设置缓存（统一配置缓存管理）
    single {
        SettingsCache(get())
    }
    
    // 下载规则存储
    single {
        DownloadRulesStore(get())
    }
}

/**
 * Pixiv API 模块（动态凭据）
 * API 实例会根据 PixivConfigStore 中的凭据动态创建
 */
val pixivApiModule = module {
    // Pixiv API Client - 从 PixivConfigStore 读取凭据，从 SettingsCache 读取语言
    single {
        val pixivConfigStore: PixivConfigStore = get()
        val settingsCache: SettingsCache = get()
        val config = runBlocking { pixivConfigStore.getCurrentConfig() }
        
        PixivApiClient(
            httpClient = get(),
            phpSessionId = config.phpSessionId.ifBlank { "0_default" }, // 默认值，未登录时使用
            token = config.csrfToken,
            langProvider = { settingsCache.getPixivLanguageCode() }, // 从 SettingsCache 动态获取语言
            onTokenUpdated = { token ->
                // 保存获取到的CSRF token
                pixivConfigStore.setCsrfToken(token)
            }
        )
    }
    
    // Pixiv API 门面
    single {
        PixivApi(get())
    }
}

/**
 * Repository模块
 */
val repositoryModule = module {
    // 认证仓储
    single<AuthRepository> {
        AuthRepositoryImpl(
            pixivConfigStore = get(),
            pixivApi = getOrNull()  // 可选注入，防止循环依赖
        )
    }
    
    // 设置仓储
    single<SettingsRepository> {
        SettingsRepositoryImpl(get())
    }
    
    // 作品仓储
    single<ArtworkRepository> { 
        ArtworkRepositoryImpl(
            pixivApi = get(),
            tagTranslationUtil = get(),
            ageLimitDeterminer = get()
        ) 
    }
    
    // 用户仓储
    single<UserRepository> { 
        UserRepositoryImpl(
            pixivApi = get(),
            ageLimitDeterminer = get()
        ) 
    }
    
    // 小说仓储
    single<NovelRepository> {
        NovelRepositoryImpl(
            pixivApi = get(),
            ageLimitDeterminer = get()
        )
    }
    
    // 小说系列仓储
    single<NovelSeriesRepository> {
        NovelSeriesRepositoryImpl(
            pixivApi = get(),
            ageLimitDeterminer = get()
        )
    }
    
    // 漫画系列仓储
    single<MangaSeriesRepository> {
        MangaSeriesRepositoryImpl(
            pixivApi = get(),
            ageLimitDeterminer = get(),
            tagTranslationUtil = get()
        )
    }
    
    // 追更列表仓储
    single<WatchListRepository> {
        WatchListRepositoryImpl(
            followApi = get<PixivApi>().followApi
        )
    }
    
    // 评论仓储
    single<CommentRepository> {
        CommentRepositoryImpl(
            pixivApi = get()
        )
    }
    
    // 全局状态缓存仓储（纯内存实现，不依赖数据库）
    single<StateCacheRepository> {
        StateCacheRepositoryInMemory()
    }
    
    // 下载仓储
    single<DownloadRepository> {
        DownloadRepositoryImpl(get())
    }
    
    // 下载规则仓储
    single<DownloadRulesRepository> {
        DownloadRulesRepositoryImpl(get())
    }
}

/**
 * 全局状态缓存模块
 */
val stateCacheModule = module {
    // 全局状态缓存管理器
    single {
        StateCacheManager(
            stateCacheRepository = get()
        )
    }
    
    // 全局作品缓存管理器
    single {
        ArtworkCacheManager()
    }
    
    // 全局小说缓存管理器
    single {
        NovelCacheManager()
    }
    
    // 下载规则缓存（动态获取 baseDownloadPath）
    single {
        DownloadRulesCache(
            downloadRulesStore = get(),
            baseDownloadPathProvider = { get<SettingsCache>().getBaseDownloadPath() }
        )
    }
}

/**
 * UseCase模块
 */
val useCaseModule = module {
    // Ugoira 相关
    factory { GetUgoiraUseCase(get()) }
    
    // 作品收藏相关
    factory { BookmarkArtworkUseCase(get(), get()) }
    factory { UnbookmarkArtworkUseCase(get(), get()) }
    factory { SyncArtworkStatesUseCase(get()) }
    
    // 小说收藏相关
    factory { BookmarkNovelUseCase(get(), get()) }
    factory { UnbookmarkNovelUseCase(get(), get()) }
    factory { SyncNovelStatesUseCase(get()) }
    
    // 用户关注相关
    factory { FollowUserUseCase(get(), get()) }
    factory { UnfollowUserUseCase(get(), get()) }
    factory { SyncUserStatesUseCase(get()) }
    factory { SyncUserFollowDetailsUseCase(get(), get()) }
}

/**
 * 工具类模块
 */
val utilModule = module {
    // 标签翻译工具
    single { TagTranslationUtil(get()) }
    
    // 年龄限制判定工具
    single { AgeLimitDeterminer(get()) }
    
    // 下载路径构建器
    single { DownloadPathBuilder(FileSystem.SYSTEM) }
}

/**
 * 下载管理模块
 * cachedFileProvider是可选的，用于从UI层的图片缓存复用文件
 */
fun downloadModule(cachedFileProvider: com.projectu.shared.data.manager.CachedFileProvider? = null) = module {
    // 下载管理器
    single {
        com.projectu.shared.data.manager.DownloadManager(
            pixivApi = get(),
            downloadDao = get(),
            pathBuilder = get(),
            fileSystem = FileSystem.SYSTEM,
            platformFileWriter = get(),
            httpClient = get(),
            cachedFileProvider = cachedFileProvider,
            settingsCache = get(),
            downloadRulesCache = get()
        )
    }
}
