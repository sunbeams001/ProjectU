package com.projectu.shared.di

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
 * Repository模块
 */
val repositoryModule = module {
    // TODO: 添加Repository实现
}

/**
 * UseCase模块
 */
val useCaseModule = module {
    // TODO: 添加UseCase实现
}

