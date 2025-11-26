package com.projectu.shared.di

import com.projectu.shared.data.local.PixivConfigStore
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.local.SettingsStore
import com.projectu.shared.data.local.createPixivConfigDataStore
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.api.PixivApiClient
import com.projectu.shared.data.repository.ArtworkRepositoryImpl
import com.projectu.shared.data.repository.AuthRepositoryImpl
import com.projectu.shared.data.repository.SettingsRepositoryImpl
import com.projectu.shared.data.repository.UserRepositoryImpl
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.AuthRepository
import com.projectu.shared.domain.repository.SettingsRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.GetUgoiraUseCase
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import io.ktor.client.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
}

/**
 * UseCase模块
 */
val useCaseModule = module {
    // Ugoira 相关
    factory { GetUgoiraUseCase(get()) }
}

/**
 * 工具类模块
 */
val utilModule = module {
    // 标签翻译工具
    single { TagTranslationUtil(get()) }
    
    // 年龄限制判定工具
    single { AgeLimitDeterminer(get()) }
}
