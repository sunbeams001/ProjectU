package com.projectu.di

import com.projectu.shared.data.cache.UgoiraCache
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.dao.DownloadDao
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
import com.projectu.ui.screens.discovery.DiscoveryPixivisionViewModel
import com.projectu.ui.screens.discovery.DiscoveryUsersViewModel
import com.projectu.ui.screens.pixivision.PixivisionDetailViewModel
import com.projectu.ui.screens.followlatest.FollowLatestIllustsViewModel
import com.projectu.ui.screens.followlatest.FollowLatestNovelsViewModel
import com.projectu.ui.screens.followlatest.WatchListMangaViewModel
import com.projectu.ui.screens.followlatest.WatchListNovelsViewModel
import com.projectu.ui.screens.userrelations.UserRelationsViewModel
import com.projectu.ui.screens.comment.CommentsViewModel
import com.projectu.ui.screens.download.DownloadRulesViewModel
import io.ktor.client.engine.okhttp.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    // 使用 OkHttp 引擎以获得更好的 Android 平台性能和连接池优化
    // 配置优先使用 HTTP/2 协议以提升性能和降低延迟
    single {
        val okHttpClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()
        NetworkClient.create(OkHttp.create { preconfigured = okHttpClient })
    }
}

actual val databaseModule: Module = module {
    // 数据库实例 - 按照官方文档标准实现
    single {
        val builder = getDatabaseBuilder()
        getRoomDatabase(builder)
    }
    
    // DAO
    single<SettingsDao> { get<AppDatabase>().settingsDao() }
    single<DownloadDao> { get<AppDatabase>().downloadDao() }
    single { get<AppDatabase>().downloadRulesDao() }
    single { get<AppDatabase>().browseHistoryDao() }
    single { get<AppDatabase>().blockRuleDao() }
    single { get<AppDatabase>().widgetConfigDao() }
    
    // 设置存储
    single { SettingsStore(get()) }
}

actual val repositoryModule: Module = module {
    // 设置仓储
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    
    // Widget 仓储
    single<com.projectu.shared.domain.repository.WidgetRepository> {
        com.projectu.shared.data.repository.WidgetRepositoryImpl(
            widgetConfigDao = get(),
            artworkRepository = get()
        )
    }
}

actual val useCaseModule: Module = module {
    // Ugoira 缓存管理器
    single {
        val context = androidContext()
        val cacheDir = context.cacheDir.toOkioPath()
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
    
    // Widget UseCase
    factory {
        com.projectu.shared.domain.usecase.GetWidgetArtworksUseCase(
            widgetRepository = get()
        )
    }
}

actual val viewModelModule: Module = module {
    // 设置 ViewModel
    viewModel { SettingsViewModel(get(), get()) }
    
    // 屏蔽列表 ViewModel
    viewModel { com.projectu.ui.screens.blocklist.BlockListViewModel(get()) }
    
    // 登录 ViewModel
    viewModel { com.projectu.ui.screens.login.LoginViewModel(get()) }
    
    // API 测试 ViewModel
    viewModel { ApiTestViewModel(get(), get()) }
    
    // 下载 ViewModel
    viewModel { com.projectu.ui.screens.download.DownloadViewModel(get()) }
    
    // 下载规则管理 ViewModel
    viewModel { DownloadRulesViewModel(get()) }
    
    // 浏览历史 ViewModel
    viewModel { com.projectu.ui.screens.history.BrowseHistoryViewModel(get()) }
    
    // 发现插画 ScreenModel
    single { DiscoveryIllustsViewModel(get(), get(), get()) }
    
    // 发现用户 ScreenModel
    single { DiscoveryUsersViewModel(get(), get(), get(), get()) }
    
    // 发现小说 ScreenModel
    single { DiscoveryNovelsViewModel(get(), get(), get()) }
    
    // 发现 Pixivision ScreenModel
    single { DiscoveryPixivisionViewModel(get(), get()) }
    
    // Pixivision 详情 ScreenModel
    factory { PixivisionDetailViewModel(get(), get(), get(), get()) }
    
    // 关注用户最新插画 ScreenModel
    single { FollowLatestIllustsViewModel(get(), get(), get()) }
    
    // 关注用户最新小说 ScreenModel
    single { FollowLatestNovelsViewModel(get(), get(), get()) }
    
    // 漫画追更列表 ScreenModel
    single { WatchListMangaViewModel(get()) }
    
    // 小说追更列表 ScreenModel
    single { WatchListNovelsViewModel(get()) }
    
    // 排行榜 ScreenModel
    single { com.projectu.ui.screens.ranking.RankingViewModel(get(), get(), get(), get(), get()) }
    
    // 作品详情 ScreenModel
    factory { com.projectu.ui.screens.artwork.ArtworkDetailViewModel(get(), get(), get(), get(), get(), get(), get()) }
    
    // 用户主页 ScreenModel
    factory { com.projectu.ui.screens.user.UserViewModel(get(), get(), get(), get(), get(), get()) }
    
    // 小说系列详情 ScreenModel
    factory { com.projectu.ui.screens.novelseries.NovelSeriesViewModel(get()) }
    
    // 小说详情 ScreenModel
    factory { com.projectu.ui.screens.novel.NovelDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    
    // 漫画系列详情 ScreenModel
    factory { com.projectu.ui.screens.mangaseries.MangaSeriesViewModel(get()) }
    
    // 用户关系 ScreenModel
    factory { UserRelationsViewModel(get(), get(), get(), get(), get()) }
    
    // 评论页面 ScreenModel
    factory { CommentsViewModel(get(), get(), get(), get()) }
    
    // 搜索准备页面 ScreenModel
    single { com.projectu.ui.screens.search.SearchPreparationViewModel(get(), get(), get()) }
    
    // 搜索结果页面 ScreenModel
    factory { (keyword: String) -> com.projectu.ui.screens.search.SearchResultViewModel(keyword, get(), get(), get(), get(), get(), get()) }
}

