package com.projectu.shared.di

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.api.PixivApiClient
import com.projectu.shared.data.repository.ArtworkRepositoryImpl
import com.projectu.shared.data.repository.UserRepositoryImpl
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.UserRepository
import com.projectu.shared.domain.usecase.GetUgoiraUseCase
import com.projectu.shared.util.NetworkClient
import io.ktor.client.*
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
 * Pixiv API 模块
 * 需要在使用前设置 phpSessionId
 */
fun pixivApiModule(phpSessionId: String, token: String? = null) = module {
    // Pixiv API Client
    single {
        PixivApiClient(
            httpClient = get(),
            phpSessionId = phpSessionId,
            token = token
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
    // 作品仓储
    single<ArtworkRepository> { 
        ArtworkRepositoryImpl(get()) 
    }
    
    // 用户仓储
    single<UserRepository> { 
        UserRepositoryImpl(get()) 
    }
}

/**
 * UseCase模块
 */
val useCaseModule = module {
    // Ugoira 相关
    factory { GetUgoiraUseCase(get()) }
}

